package com.autoservice.billing.messaging;

import com.autoservice.billing.service.BillingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingEventoListenerTest {

    @Mock
    private BillingService billingService;

    @Test
    void encaminhaOrcamentoECompensacao() {
        final BillingEventoListener listener = new BillingEventoListener(this.billingService);
        final EventoSaga orcamento = EventoSaga.novo(TiposEvento.ORCAMENTO_SOLICITADO, UUID.randomUUID(), UUID.randomUUID(), Map.of());
        final EventoSaga compensacao = EventoSaga.novo(TiposEvento.COMPENSAR_PAGAMENTO, UUID.randomUUID(), UUID.randomUUID(), Map.of());

        listener.receber(null);
        listener.receber(orcamento);
        listener.receber(compensacao);
        listener.receber(EventoSaga.novo("outro", UUID.randomUUID(), UUID.randomUUID(), Map.of()));

        verify(this.billingService).gerar(orcamento);
        verify(this.billingService).compensar(compensacao);
    }

    @Test
    void falhaNaoVoltaParaAFila() {
        final BillingEventoListener listener = new BillingEventoListener(this.billingService);
        final EventoSaga orcamento = EventoSaga.novo(TiposEvento.ORCAMENTO_SOLICITADO, UUID.randomUUID(), UUID.randomUUID(), Map.of());
        doThrow(new IllegalStateException("boom")).when(this.billingService).gerar(orcamento);

        listener.receber(orcamento);
    }
}
