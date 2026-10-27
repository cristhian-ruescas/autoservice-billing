package com.autoservice.billing.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoSaga(
        UUID eventId,
        String eventType,
        UUID correlationId,
        UUID ordemServicoId,
        Map<String, Object> payload
) {

    public static EventoSaga novo(
            final String eventType,
            final UUID ordemServicoId,
            final UUID correlationId,
            final Map<String, Object> payload
    ) {
        return new EventoSaga(UUID.randomUUID(), eventType, correlationId, ordemServicoId, payload == null ? Map.of() : payload);
    }
}
