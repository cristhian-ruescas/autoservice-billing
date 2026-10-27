package com.autoservice.billing.service;

import com.autoservice.billing.domain.ItemSnapshot;
import com.autoservice.billing.domain.NaoEncontradoException;
import com.autoservice.billing.domain.NegocioException;
import com.autoservice.billing.domain.OrcamentoStatus;
import com.autoservice.billing.domain.PagamentoStatus;
import com.autoservice.billing.messaging.EventoSaga;
import com.autoservice.billing.messaging.MensagemSaga;
import com.autoservice.billing.messaging.TiposEvento;
import com.autoservice.billing.orcamento.OrcamentoEmailSender;
import com.autoservice.billing.pagamento.MercadoPagoClient;
import com.autoservice.billing.pagamento.ResultadoPagamentoMp;
import com.autoservice.billing.persistence.OrcamentoEntity;
import com.autoservice.billing.persistence.OrcamentoItemEntity;
import com.autoservice.billing.persistence.OrcamentoRepository;
import com.autoservice.billing.persistence.PagamentoEntity;
import com.autoservice.billing.persistence.PagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private OrcamentoRepository orcamentos;
    @Mock
    private PagamentoRepository pagamentos;
    @Mock
    private MensagemSaga mensagemSaga;
    @Mock
    private MercadoPagoClient mercadoPagoClient;
    @Mock
    private OrcamentoEmailSender emailSender;
    @InjectMocks
    private BillingService service;

    private final UUID ordemId = UUID.randomUUID();
    private final UUID correlationId = UUID.randomUUID();

    @Test
    void geraOrcamentoEPublicaEvento() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        when(this.emailSender.enviar(any(), any())).thenReturn(true);

        this.service.gerar(evento(TiposEvento.ORCAMENTO_SOLICITADO, Map.of(
                "itens", List.of(Map.of("descricao", "oleo", "valor", new BigDecimal("80.00")))
        )));

        final ArgumentCaptor<EventoSaga> publicado = ArgumentCaptor.forClass(EventoSaga.class);
        verify(this.mensagemSaga).publicar(publicado.capture());
        assertThat(publicado.getValue().eventType()).isEqualTo(TiposEvento.ORCAMENTO_EMITIDO);
    }

    @Test
    void geraIgnoraDuplicadoEValorInvalido() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.EMITIDO)));
        this.service.gerar(evento(TiposEvento.ORCAMENTO_SOLICITADO, Map.of()));
        this.service.gerar(null);

        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        this.service.gerar(evento(TiposEvento.ORCAMENTO_SOLICITADO, Map.of("itens", List.of(Map.of("descricao", "sem valor")))));

        verify(this.mensagemSaga).publicar(any());
    }

    @Test
    void aprovaComMercadoPagoAprovado() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.EMITIDO)));
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        when(this.mercadoPagoClient.criarOrdem(any())).thenReturn(new ResultadoPagamentoMp("ORD1", "approved", "qr", true, false));

        final var resposta = this.service.aprovar(this.ordemId);

        assertThat(resposta.status()).isEqualTo("APROVADO");
        assertThat(resposta.qrData()).isEqualTo("qr");
        verify(this.mensagemSaga).publicar(any());
    }

    @Test
    void aprovaPagamentoPendenteNaoAvancaSaga() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.EMITIDO)));
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        when(this.mercadoPagoClient.criarOrdem(any())).thenReturn(new ResultadoPagamentoMp("ORD2", "created", null, false, false));

        assertThat(this.service.aprovar(this.ordemId).status()).isEqualTo("PENDENTE");
        verify(this.mensagemSaga, never()).publicar(any());
    }

    @Test
    void aprovaPagamentoRecusado() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.EMITIDO)));
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        when(this.mercadoPagoClient.criarOrdem(any())).thenReturn(new ResultadoPagamentoMp("ORD3", "rejected", null, false, true));

        assertThat(this.service.aprovar(this.ordemId).status()).isEqualTo("RECUSADO");
    }

    @Test
    void aprovarDeNovoNaoCriaOutraOrdem() {
        final PagamentoEntity pagamento = pagamento(PagamentoStatus.APROVADO);
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.APROVADO)));
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(pagamento));

        assertThat(this.service.aprovar(this.ordemId).mpOrderId()).isEqualTo("ORD-EXISTE");
        verify(this.mercadoPagoClient, never()).criarOrdem(any());
    }

    @Test
    void naoAprovaOrcamentoReprovado() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.REPROVADO)));

        assertThatThrownBy(() -> this.service.aprovar(this.ordemId)).isInstanceOf(NegocioException.class);
    }

    @Test
    void reprovaENaoReprovaPagamentoAprovado() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(orcamento(OrcamentoStatus.EMITIDO)));
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        assertThat(this.service.reprovar(this.ordemId).status()).isEqualTo("REPROVADO");

        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(pagamento(PagamentoStatus.APROVADO)));
        assertThatThrownBy(() -> this.service.reprovar(this.ordemId)).isInstanceOf(NegocioException.class);
    }

    @Test
    void webhookAprovaPagamentoPendente() {
        final PagamentoEntity pagamento = pagamento(PagamentoStatus.PENDENTE);
        when(this.pagamentos.findByMpOrderId("ORD-EXISTE")).thenReturn(Optional.of(pagamento));
        when(this.mercadoPagoClient.consultar("ORD-EXISTE")).thenReturn(new ResultadoPagamentoMp("ORD-EXISTE", "processed", "qr2", true, false));

        this.service.webhook(Map.of("type", "order", "data", Map.of("id", "ORD-EXISTE")));

        assertThat(pagamento.getStatus()).isEqualTo(PagamentoStatus.APROVADO);
    }

    @Test
    void webhookIgnoraCorpoInvalidoOuPagamentoEncerrado() {
        this.service.webhook(null);
        this.service.webhook(Map.of("data", Map.of("id", " ")));
        when(this.pagamentos.findByMpOrderId("ORD-EXISTE")).thenReturn(Optional.of(pagamento(PagamentoStatus.CANCELADO)));
        this.service.webhook(Map.of("data", Map.of("id", "ORD-EXISTE")));
        verify(this.mercadoPagoClient, never()).consultar(any());
    }

    @Test
    void compensacaoCancelaNoMercadoPago() {
        final PagamentoEntity pagamento = pagamento(PagamentoStatus.APROVADO);
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(pagamento));

        this.service.compensar(evento(TiposEvento.COMPENSAR_PAGAMENTO, Map.of()));

        verify(this.mercadoPagoClient).cancelarOuEstornar("ORD-EXISTE", pagamento.getValor(), "approved");
        assertThat(pagamento.getStatus()).isEqualTo(PagamentoStatus.CANCELADO);
    }

    @Test
    void compensacaoSemPagamentoAindaConfirma() {
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());

        this.service.compensar(evento(TiposEvento.COMPENSAR_PAGAMENTO, Map.of()));
        this.service.compensar(null);

        verify(this.mensagemSaga).publicar(any());
    }

    @Test
    void compensacaoJaCanceladaNaoChamaMercadoPagoDeNovo() {
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.of(pagamento(PagamentoStatus.CANCELADO)));

        this.service.compensar(evento(TiposEvento.COMPENSAR_PAGAMENTO, Map.of()));

        verify(this.mercadoPagoClient, never()).cancelarOuEstornar(any(), any(), any());
    }

    @Test
    void consultasInexistentes() {
        when(this.orcamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        when(this.pagamentos.findByOrdemServicoId(this.ordemId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> this.service.consultarOrcamento(this.ordemId)).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> this.service.consultarPagamento(this.ordemId)).isInstanceOf(NaoEncontradoException.class);
    }

    private EventoSaga evento(final String tipo, final Map<String, Object> payload) {
        return EventoSaga.novo(tipo, this.ordemId, this.correlationId, payload);
    }

    private OrcamentoEntity orcamento(final OrcamentoStatus status) {
        final OrcamentoEntity orcamento = new OrcamentoEntity();
        orcamento.setId(UUID.randomUUID());
        orcamento.setOrdemServicoId(this.ordemId);
        orcamento.setCorrelationId(this.correlationId);
        orcamento.setStatus(status);
        orcamento.setValorTotal(new BigDecimal("80.00"));
        final OrcamentoItemEntity item = new OrcamentoItemEntity();
        item.setId(UUID.randomUUID());
        item.setDescricao("oleo");
        item.setValor(new BigDecimal("80.00"));
        item.setOrcamento(orcamento);
        orcamento.getItens().add(item);
        return orcamento;
    }

    private PagamentoEntity pagamento(final PagamentoStatus status) {
        final PagamentoEntity pagamento = new PagamentoEntity();
        pagamento.setId(UUID.randomUUID());
        pagamento.setOrdemServicoId(this.ordemId);
        pagamento.setOrcamentoId(UUID.randomUUID());
        pagamento.setCorrelationId(this.correlationId);
        pagamento.setStatus(status);
        pagamento.setMpOrderId("ORD-EXISTE");
        pagamento.setStatusMp("approved");
        pagamento.setValor(new BigDecimal("80.00"));
        pagamento.setQrData("qr");
        return pagamento;
    }
}
