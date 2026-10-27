package com.autoservice.billing.messaging;

public final class TiposEvento {

    public static final String ORCAMENTO_SOLICITADO = "orcamento.solicitado";
    public static final String ORCAMENTO_EMITIDO = "orcamento.emitido";
    public static final String ORCAMENTO_FALHOU = "orcamento.falhou";
    public static final String ORCAMENTO_REPROVADO = "orcamento.reprovado";
    public static final String PAGAMENTO_APROVADO = "pagamento.aprovado";
    public static final String PAGAMENTO_RECUSADO = "pagamento.recusado";
    public static final String PAGAMENTO_CANCELADO = "pagamento.cancelado";
    public static final String COMPENSAR_PAGAMENTO = "compensacao.pagamento.cancelar";
    public static final String EXCHANGE = "autoservice.saga";

    private TiposEvento() {
    }
}
