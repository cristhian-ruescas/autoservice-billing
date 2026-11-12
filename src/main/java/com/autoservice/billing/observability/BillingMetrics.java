package com.autoservice.billing.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BillingMetrics {

    public static final String METRIC_ORCAMENTO = "autoservice.billing.orcamento";
    public static final String METRIC_PAGAMENTO = "autoservice.billing.pagamento";
    public static final String METRIC_COMPENSACAO = "autoservice.billing.compensacao";
    public static final String METRIC_ERRO_API = "autoservice.billing.api.erro";

    private final MeterRegistry meterRegistry;

    public BillingMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public void registrarOrcamento(final String resultado) {
        meterRegistry.counter(METRIC_ORCAMENTO, "resultado", resultado).increment();
    }

    public void registrarPagamento(final String resultado) {
        meterRegistry.counter(METRIC_PAGAMENTO, "resultado", resultado).increment();
    }

    public void registrarCompensacao() {
        meterRegistry.counter(METRIC_COMPENSACAO).increment();
    }

    public void registrarErroApi(final String tipo) {
        meterRegistry.counter(METRIC_ERRO_API, "tipo", tipo == null ? "desconhecido" : tipo).increment();
    }
}
