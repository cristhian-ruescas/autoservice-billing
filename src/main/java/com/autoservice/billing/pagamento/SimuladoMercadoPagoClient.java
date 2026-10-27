package com.autoservice.billing.pagamento;

import java.math.BigDecimal;

public class SimuladoMercadoPagoClient implements MercadoPagoClient {

    @Override
    public ResultadoPagamentoMp criarOrdem(final CriarOrdemMp comando) {
        return new ResultadoPagamentoMp(
                "sim-" + comando.ordemServicoId(),
                "approved",
                "qr-simulado-" + comando.ordemServicoId(),
                true,
                false
        );
    }

    @Override
    public ResultadoPagamentoMp consultar(final String mpOrderId) {
        return new ResultadoPagamentoMp(mpOrderId, "approved", "qr-simulado", true, false);
    }

    @Override
    public void cancelarOuEstornar(final String mpOrderId, final BigDecimal valor, final String statusMp) {
        // Pagamento local: nao ha chamada externa para cancelar.
    }
}
