package com.autoservice.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autoservice.mercadopago")
public class MercadoPagoProperties {

    private String baseUrl = "https://api.mercadopago.com";
    private String accessToken = "";
    private String externalPosId = "autoservice-pos";

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getExternalPosId() {
        return this.externalPosId;
    }

    public void setExternalPosId(final String externalPosId) {
        this.externalPosId = externalPosId;
    }
}
