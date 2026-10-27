package com.autoservice.billing.api;

import com.autoservice.billing.persistence.OrcamentoEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID ordemServicoId,
        String status,
        BigDecimal valorTotal,
        boolean emailEnviado
) {

    public static OrcamentoResponse de(final OrcamentoEntity orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getStatus().name(),
                orcamento.getValorTotal(),
                orcamento.isEmailEnviado()
        );
    }
}
