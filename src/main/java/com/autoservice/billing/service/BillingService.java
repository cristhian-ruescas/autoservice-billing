package com.autoservice.billing.service;

import com.autoservice.billing.api.OrcamentoResponse;
import com.autoservice.billing.api.PagamentoResponse;
import com.autoservice.billing.domain.ItemSnapshot;
import com.autoservice.billing.domain.NaoEncontradoException;
import com.autoservice.billing.domain.NegocioException;
import com.autoservice.billing.domain.OrcamentoStatus;
import com.autoservice.billing.domain.PagamentoStatus;
import com.autoservice.billing.messaging.EventoSaga;
import com.autoservice.billing.messaging.MensagemSaga;
import com.autoservice.billing.messaging.Payloads;
import com.autoservice.billing.messaging.TiposEvento;
import com.autoservice.billing.observability.BillingMetrics;
import com.autoservice.billing.orcamento.OrcamentoEmailSender;
import com.autoservice.billing.orcamento.OrcamentoPdf;
import com.autoservice.billing.pagamento.CriarOrdemMp;
import com.autoservice.billing.pagamento.MercadoPagoClient;
import com.autoservice.billing.pagamento.ResultadoPagamentoMp;
import com.autoservice.billing.persistence.OrcamentoEntity;
import com.autoservice.billing.persistence.OrcamentoItemEntity;
import com.autoservice.billing.persistence.OrcamentoRepository;
import com.autoservice.billing.persistence.PagamentoEntity;
import com.autoservice.billing.persistence.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private final OrcamentoRepository orcamentos;
    private final PagamentoRepository pagamentos;
    private final MensagemSaga mensagemSaga;
    private final MercadoPagoClient mercadoPagoClient;
    private final OrcamentoEmailSender emailSender;
    private final BillingMetrics metrics;

    public BillingService(
            final OrcamentoRepository orcamentos,
            final PagamentoRepository pagamentos,
            final MensagemSaga mensagemSaga,
            final MercadoPagoClient mercadoPagoClient,
            final OrcamentoEmailSender emailSender,
            final BillingMetrics metrics
    ) {
        this.orcamentos = orcamentos;
        this.pagamentos = pagamentos;
        this.mensagemSaga = mensagemSaga;
        this.mercadoPagoClient = mercadoPagoClient;
        this.emailSender = emailSender;
        this.metrics = metrics;
    }

    @Transactional
    public void gerar(final EventoSaga evento) {
        if (evento == null || evento.ordemServicoId() == null) {
            return;
        }
        if (this.orcamentos.findByOrdemServicoId(evento.ordemServicoId()).isPresent()) {
            return;
        }
        final List<ItemSnapshot> itens = Payloads.itens(evento.payload());
        final BigDecimal total = ItemSnapshot.total(itens);
        if (itens.isEmpty() || total.signum() <= 0) {
            this.metrics.registrarOrcamento("falhou");
            publicar(TiposEvento.ORCAMENTO_FALHOU, evento, Map.of("motivo", "valor invalido"));
            return;
        }
        final OrcamentoEntity orcamento = new OrcamentoEntity();
        orcamento.setId(UUID.randomUUID());
        orcamento.setOrdemServicoId(evento.ordemServicoId());
        orcamento.setCorrelationId(evento.correlationId());
        orcamento.setValorTotal(total);
        orcamento.setStatus(OrcamentoStatus.EMITIDO);
        orcamento.setCriadoEm(Instant.now());
        for (final ItemSnapshot item : itens) {
            final OrcamentoItemEntity entity = new OrcamentoItemEntity();
            entity.setId(UUID.randomUUID());
            entity.setDescricao(item.descricao());
            entity.setValor(item.valor());
            entity.setOrcamento(orcamento);
            orcamento.getItens().add(entity);
        }
        final byte[] pdf = OrcamentoPdf.gerar(
                "Orcamento " + evento.ordemServicoId(),
                itens.stream().map(item -> item.descricao() + " " + item.valor()).toList()
        );
        orcamento.setEmailEnviado(this.emailSender.enviar(evento.ordemServicoId(), pdf));
        this.orcamentos.save(orcamento);
        publicar(TiposEvento.ORCAMENTO_EMITIDO, evento, Map.of(
                "orcamentoId", orcamento.getId().toString(),
                "valorTotal", total
        ));
        this.metrics.registrarOrcamento("emitido");
    }

    @Transactional
    public PagamentoResponse aprovar(final UUID ordemServicoId) {
        final OrcamentoEntity orcamento = buscarOrcamento(ordemServicoId);
        if (orcamento.getStatus() == OrcamentoStatus.REPROVADO) {
            throw new NegocioException("Orcamento reprovado");
        }
        final PagamentoEntity existente = this.pagamentos.findByOrdemServicoId(ordemServicoId).orElse(null);
        if (existente != null && existente.getStatus() == PagamentoStatus.APROVADO) {
            return PagamentoResponse.de(existente);
        }
        orcamento.setStatus(OrcamentoStatus.APROVADO);
        final List<ItemSnapshot> itens = orcamento.getItens().stream()
                .map(item -> new ItemSnapshot(item.getDescricao(), item.getValor()))
                .toList();
        final ResultadoPagamentoMp mercadoPago = this.mercadoPagoClient.criarOrdem(new CriarOrdemMp(
                ordemServicoId,
                "Ordem de servico " + ordemServicoId,
                orcamento.getValorTotal(),
                itens
        ));
        final PagamentoEntity pagamento = existente == null ? novoPagamento(orcamento) : existente;
        aplicarMercadoPago(pagamento, mercadoPago);
        this.pagamentos.save(pagamento);
        this.orcamentos.save(orcamento);
        publicarResultado(pagamento, mercadoPago);
        this.metrics.registrarOrcamento("aprovado");
        return PagamentoResponse.de(pagamento);
    }

    @Transactional
    public OrcamentoResponse reprovar(final UUID ordemServicoId) {
        final OrcamentoEntity orcamento = buscarOrcamento(ordemServicoId);
        final PagamentoEntity pagamento = this.pagamentos.findByOrdemServicoId(ordemServicoId).orElse(null);
        if (pagamento != null && pagamento.getStatus() == PagamentoStatus.APROVADO) {
            throw new NegocioException("Pagamento ja aprovado");
        }
        orcamento.setStatus(OrcamentoStatus.REPROVADO);
        this.orcamentos.save(orcamento);
        publicar(TiposEvento.ORCAMENTO_REPROVADO, orcamento.getOrdemServicoId(), orcamento.getCorrelationId(), Map.of(
                "orcamentoId", orcamento.getId().toString()
        ));
        this.metrics.registrarOrcamento("reprovado");
        return OrcamentoResponse.de(orcamento);
    }

    @Transactional
    public void webhook(final Map<String, Object> body) {
        final String mpOrderId = idDoWebhook(body);
        if (mpOrderId == null) {
            return;
        }
        final PagamentoEntity pagamento = this.pagamentos.findByMpOrderId(mpOrderId).orElse(null);
        if (pagamento == null || pagamento.getStatus() == PagamentoStatus.APROVADO || pagamento.getStatus() == PagamentoStatus.CANCELADO) {
            return;
        }
        final ResultadoPagamentoMp mercadoPago = this.mercadoPagoClient.consultar(mpOrderId);
        aplicarMercadoPago(pagamento, mercadoPago);
        this.pagamentos.save(pagamento);
        publicarResultado(pagamento, mercadoPago);
    }

    @Transactional
    public void compensar(final EventoSaga evento) {
        if (evento == null || evento.ordemServicoId() == null) {
            return;
        }
        final PagamentoEntity pagamento = this.pagamentos.findByOrdemServicoId(evento.ordemServicoId()).orElse(null);
        if (pagamento == null) {
            publicar(TiposEvento.PAGAMENTO_CANCELADO, evento, Map.of("motivo", "sem pagamento"));
            return;
        }
        if (pagamento.getStatus() != PagamentoStatus.CANCELADO) {
            this.mercadoPagoClient.cancelarOuEstornar(pagamento.getMpOrderId(), pagamento.getValor(), pagamento.getStatusMp());
            pagamento.setStatus(PagamentoStatus.CANCELADO);
            pagamento.setAtualizadoEm(Instant.now());
            this.pagamentos.save(pagamento);
        }
        publicar(TiposEvento.PAGAMENTO_CANCELADO, evento, Map.of(
                "pagamentoId", pagamento.getId().toString(),
                "mpOrderId", pagamento.getMpOrderId() == null ? "" : pagamento.getMpOrderId()
        ));
        this.metrics.registrarCompensacao();
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse consultarOrcamento(final UUID ordemServicoId) {
        return OrcamentoResponse.de(buscarOrcamento(ordemServicoId));
    }

    @Transactional(readOnly = true)
    public PagamentoResponse consultarPagamento(final UUID ordemServicoId) {
        return PagamentoResponse.de(this.pagamentos.findByOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NaoEncontradoException("Pagamento nao encontrado")));
    }

    private void publicarResultado(final PagamentoEntity pagamento, final ResultadoPagamentoMp mercadoPago) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pagamentoId", pagamento.getId().toString());
        payload.put("mpOrderId", mercadoPago.id());
        payload.put("qrData", mercadoPago.qrData() == null ? "" : mercadoPago.qrData());
        if (mercadoPago.aprovado() && pagamento.getStatus() == PagamentoStatus.APROVADO) {
            publicar(TiposEvento.PAGAMENTO_APROVADO, pagamento.getOrdemServicoId(), pagamento.getCorrelationId(), payload);
            this.metrics.registrarPagamento("aprovado");
        } else if (mercadoPago.recusado() && pagamento.getStatus() == PagamentoStatus.RECUSADO) {
            publicar(TiposEvento.PAGAMENTO_RECUSADO, pagamento.getOrdemServicoId(), pagamento.getCorrelationId(), payload);
            this.metrics.registrarPagamento("recusado");
        }
    }

    private void aplicarMercadoPago(final PagamentoEntity pagamento, final ResultadoPagamentoMp mercadoPago) {
        pagamento.setMpOrderId(mercadoPago.id());
        pagamento.setStatusMp(mercadoPago.status());
        pagamento.setQrData(mercadoPago.qrData());
        pagamento.setAtualizadoEm(Instant.now());
        if (mercadoPago.aprovado()) {
            pagamento.setStatus(PagamentoStatus.APROVADO);
        } else if (mercadoPago.recusado()) {
            pagamento.setStatus(PagamentoStatus.RECUSADO);
        } else {
            pagamento.setStatus(PagamentoStatus.PENDENTE);
        }
    }

    private PagamentoEntity novoPagamento(final OrcamentoEntity orcamento) {
        final PagamentoEntity pagamento = new PagamentoEntity();
        pagamento.setId(UUID.randomUUID());
        pagamento.setOrdemServicoId(orcamento.getOrdemServicoId());
        pagamento.setOrcamentoId(orcamento.getId());
        pagamento.setCorrelationId(orcamento.getCorrelationId());
        pagamento.setValor(orcamento.getValorTotal());
        pagamento.setCriadoEm(Instant.now());
        pagamento.setStatus(PagamentoStatus.PENDENTE);
        return pagamento;
    }

    private OrcamentoEntity buscarOrcamento(final UUID ordemServicoId) {
        return this.orcamentos.findByOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NaoEncontradoException("Orcamento nao encontrado"));
    }

    private void publicar(final String tipo, final EventoSaga origem, final Map<String, Object> payload) {
        publicar(tipo, origem.ordemServicoId(), origem.correlationId(), payload);
    }

    private void publicar(final String tipo, final UUID ordemServicoId, final UUID correlationId, final Map<String, Object> payload) {
        this.mensagemSaga.publicar(EventoSaga.novo(tipo, ordemServicoId, correlationId, payload));
    }

    static String idDoWebhook(final Map<String, Object> body) {
        if (body == null || !(body.get("data") instanceof Map<?, ?> data) || data.get("id") == null) {
            return null;
        }
        final String id = data.get("id").toString();
        return id.isBlank() ? null : id;
    }
}
