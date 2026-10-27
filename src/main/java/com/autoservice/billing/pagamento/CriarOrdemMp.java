package com.autoservice.billing.pagamento;

import com.autoservice.billing.domain.ItemSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CriarOrdemMp(
        UUID ordemServicoId,
        String descricao,
        BigDecimal valor,
        List<ItemSnapshot> itens
) {
}
