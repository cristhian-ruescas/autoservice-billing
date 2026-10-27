package com.autoservice.billing.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMensagemSagaTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaPeloTipo() {
        final RabbitMensagemSaga saga = new RabbitMensagemSaga(this.rabbitTemplate);
        final EventoSaga evento = EventoSaga.novo(TiposEvento.ORCAMENTO_EMITIDO, UUID.randomUUID(), UUID.randomUUID(), Map.of());

        saga.publicar(evento);

        verify(this.rabbitTemplate).convertAndSend(TiposEvento.ORCAMENTO_EMITIDO, evento);
    }
}
