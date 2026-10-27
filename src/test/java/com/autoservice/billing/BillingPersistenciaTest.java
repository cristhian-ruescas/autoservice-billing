package com.autoservice.billing;

import com.autoservice.billing.messaging.EventoSaga;
import com.autoservice.billing.messaging.TiposEvento;
import com.autoservice.billing.service.BillingService;
import com.autoservice.billing.support.CapturingMensagemSaga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BillingPersistenciaTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private CapturingMensagemSaga mensagens;

    @Test
    void fluxoSimuladoAprovaECompensa() {
        final UUID ordemId = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        this.billingService.gerar(EventoSaga.novo(
                TiposEvento.ORCAMENTO_SOLICITADO,
                ordemId,
                correlationId,
                Map.of("itens", List.of(Map.of("descricao", "Pastilha", "valor", new BigDecimal("180.00"))))
        ));

        assertThat(this.billingService.aprovar(ordemId).status()).isEqualTo("APROVADO");
        this.billingService.compensar(EventoSaga.novo(TiposEvento.COMPENSAR_PAGAMENTO, ordemId, correlationId, Map.of()));

        assertThat(this.billingService.consultarPagamento(ordemId).status()).isEqualTo("CANCELADO");
        assertThat(this.mensagens.eventos()).extracting(EventoSaga::eventType).contains(
                TiposEvento.ORCAMENTO_EMITIDO,
                TiposEvento.PAGAMENTO_APROVADO,
                TiposEvento.PAGAMENTO_CANCELADO
        );
    }
}
