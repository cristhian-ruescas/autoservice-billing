package com.autoservice.billing.pagamento;

import java.math.BigDecimal;

public interface MercadoPagoClient {

    ResultadoPagamentoMp criarOrdem(CriarOrdemMp comando);

    ResultadoPagamentoMp consultar(String mpOrderId);

    void cancelarOuEstornar(String mpOrderId, BigDecimal valor, String statusMp);
}
