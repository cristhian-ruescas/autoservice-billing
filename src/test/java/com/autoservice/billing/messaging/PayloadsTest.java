package com.autoservice.billing.messaging;

import com.autoservice.billing.domain.ItemSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadsTest {

    @Test
    void leItensENumeros() {
        assertThat(Payloads.itens(null)).isEmpty();
        assertThat(Payloads.itens(Map.of("itens", "nao-lista"))).isEmpty();
        assertThat(Payloads.itens(Map.of("itens", List.of(Map.of("descricao", "oleo"))))).isEmpty();
        assertThat(Payloads.itens(Map.of("itens", List.of(
                Map.of("descricao", "oleo", "valor", new BigDecimal("10.50")),
                Map.of("descricao", "filtro", "valor", 20),
                Map.of("descricao", "fluido", "valor", "3.5"),
                "ignorado"
        )))).hasSize(3);
        assertThat(Payloads.decimal(null)).isEqualByComparingTo("0");
        assertThat(new ItemSnapshot("x", null).valor()).isNull();
        assertThat(ItemSnapshot.total(List.of(new ItemSnapshot("a", new BigDecimal("1.005"))))).isEqualByComparingTo("1.01");
    }
}
