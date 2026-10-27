package com.autoservice.billing.messaging;

public interface MensagemSaga {

    void publicar(EventoSaga evento);
}
