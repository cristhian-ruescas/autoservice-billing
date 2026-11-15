package com.autoservice.billing.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ObservabilityWebMvcTestConfig {

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    BillingMetrics billingMetrics(final MeterRegistry meterRegistry) {
        return new BillingMetrics(meterRegistry);
    }
}
