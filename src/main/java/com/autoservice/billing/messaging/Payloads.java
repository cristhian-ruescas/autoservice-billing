package com.autoservice.billing.messaging;

import com.autoservice.billing.domain.ItemSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Payloads {

    private Payloads() {
    }

    public static List<ItemSnapshot> itens(final Map<String, Object> payload) {
        if (payload == null || !(payload.get("itens") instanceof List<?> lista)) {
            return List.of();
        }
        final List<ItemSnapshot> itens = new ArrayList<>();
        for (final Object item : lista) {
            if (item instanceof Map<?, ?> map && map.get("descricao") != null && map.get("valor") != null) {
                itens.add(new ItemSnapshot(map.get("descricao").toString(), decimal(map.get("valor"))));
            }
        }
        return itens;
    }

    public static BigDecimal decimal(final Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }
}
