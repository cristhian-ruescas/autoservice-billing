package com.autoservice.billing.messaging;

import com.autoservice.billing.config.RabbitConfig;
import com.autoservice.billing.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class BillingEventoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingEventoListener.class);

    private final BillingService billingService;

    public BillingEventoListener(final BillingService billingService) {
        this.billingService = billingService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void receber(final EventoSaga evento) {
        if (evento == null || evento.eventType() == null) {
            return;
        }
        try {
            switch (evento.eventType()) {
                case TiposEvento.ORCAMENTO_SOLICITADO -> this.billingService.gerar(evento);
                case TiposEvento.COMPENSAR_PAGAMENTO -> this.billingService.compensar(evento);
                default -> {
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao tratar evento {}", evento.eventId(), exception);
        }
    }
}
