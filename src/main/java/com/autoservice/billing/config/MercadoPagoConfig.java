package com.autoservice.billing.config;

import com.autoservice.billing.pagamento.MercadoPagoClient;
import com.autoservice.billing.pagamento.RestMercadoPagoClient;
import com.autoservice.billing.pagamento.SimuladoMercadoPagoClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class MercadoPagoConfig {

    @Bean
    public MercadoPagoClient mercadoPagoClient(
            final RestClient.Builder builder,
            final MercadoPagoProperties properties,
            @org.springframework.beans.factory.annotation.Value("${autoservice.pagamento.simulado:true}") final boolean simulado
    ) {
        if (simulado) {
            return new SimuladoMercadoPagoClient();
        }
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new IllegalStateException("Defina MERCADOPAGO_ACCESS_TOKEN ou PAGAMENTO_SIMULADO=true");
        }
        final RestClient client = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAccessToken())
                .build();
        return new RestMercadoPagoClient(client, properties.getExternalPosId());
    }
}
