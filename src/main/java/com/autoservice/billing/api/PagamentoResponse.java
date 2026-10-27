package com.autoservice.billing.api;

import com.autoservice.billing.persistence.PagamentoEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoResponse(
        UUID id,
        UUID ordemServicoId,
        String status,
        String mpOrderId,
        String qrData,
        BigDecimal valor
) {

    public static PagamentoResponse de(final PagamentoEntity pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getOrdemServicoId(),
                pagamento.getStatus().name(),
                pagamento.getMpOrderId(),
                pagamento.getQrData(),
                pagamento.getValor()
        );
    }
}
