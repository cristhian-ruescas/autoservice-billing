package com.autoservice.billing.pagamento;

public record ResultadoPagamentoMp(
        String id,
        String status,
        String qrData,
        boolean aprovado,
        boolean recusado
) {
}
