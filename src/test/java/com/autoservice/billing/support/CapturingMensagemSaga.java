package com.autoservice.billing.support;

import com.autoservice.billing.messaging.EventoSaga;
import com.autoservice.billing.messaging.MensagemSaga;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Profile("test")
public class CapturingMensagemSaga implements MensagemSaga {

    private final List<EventoSaga> eventos = new CopyOnWriteArrayList<>();

    @Override
    public void publicar(final EventoSaga evento) {
        this.eventos.add(evento);
    }

    public List<EventoSaga> eventos() {
        return this.eventos;
    }
}
