package com.autoservice.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ItemSnapshot(String descricao, BigDecimal valor) {

    public ItemSnapshot {
        valor = valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal total(final List<ItemSnapshot> itens) {
        return itens.stream()
                .map(ItemSnapshot::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
