package com.autoservice.billing.pagamento;

import com.autoservice.billing.domain.ItemSnapshot;
import com.autoservice.billing.domain.NegocioException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestMercadoPagoClientTest {

    private WireMockServer server;
    private RestMercadoPagoClient client;

    @BeforeEach
    void subir() {
        this.server = new WireMockServer(wireMockConfig().dynamicPort());
        this.server.start();
        this.client = new RestMercadoPagoClient(
                RestClient.builder()
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .baseUrl(this.server.baseUrl())
                        .build(),
                "autoservice-pos"
        );
    }

    @AfterEach
    void derrubar() {
        this.server.stop();
    }

    @Test
    void criaOrdemAprovadaComQr() {
        this.server.stubFor(post(urlEqualTo("/v1/orders")).willReturn(okJson("""
                {"id":"ORD1","status":"approved","qr_data":"QR-DIRETO"}
                """)));

        final ResultadoPagamentoMp resultado = this.client.criarOrdem(comando());

        assertThat(resultado.aprovado()).isTrue();
        assertThat(resultado.qrData()).isEqualTo("QR-DIRETO");
        this.server.verify(postRequestedFor(urlEqualTo("/v1/orders"))
                .withHeader("X-Idempotency-Key", equalTo("8f1c6a0e-1c2b-4d3e-9f01-223344556677")));
    }

    @Test
    void leQrAninhadoEStatusRecusado() {
        this.server.stubFor(get(urlPathMatching("/v1/orders/.*")).willReturn(okJson("""
                {"id":"ORD2","status":"rejected","type_response":{"qr_data":"QR-TIPO"}}
                """)));

        final ResultadoPagamentoMp resultado = this.client.consultar("ORD2");

        assertThat(resultado.recusado()).isTrue();
        assertThat(resultado.qrData()).isEqualTo("QR-TIPO");
    }

    @Test
    void leQrDaConfiguracaoEStatusPendente() {
        this.server.stubFor(get(urlEqualTo("/v1/orders/ORD3")).willReturn(okJson("""
                {"id":"ORD3","status":"created","config":{"qr":{"qr_data":"QR-CFG"}}}
                """)));

        final ResultadoPagamentoMp resultado = this.client.consultar("ORD3");

        assertThat(resultado.aprovado()).isFalse();
        assertThat(resultado.recusado()).isFalse();
        assertThat(resultado.qrData()).isEqualTo("QR-CFG");
    }

    @Test
    void estornaPagamentoAprovadoECancelaOrdemAberta() {
        this.server.stubFor(post(urlPathMatching("/v1/orders/.*/refund")).willReturn(aResponse().withStatus(201)));
        this.server.stubFor(post(urlPathMatching("/v1/orders/.*/cancel")).willReturn(aResponse().withStatus(200)));

        this.client.cancelarOuEstornar("ORD1", new BigDecimal("10.00"), "processed");
        this.client.cancelarOuEstornar("ORD1", new BigDecimal("10.00"), "created");

        this.server.verify(postRequestedFor(urlEqualTo("/v1/orders/ORD1/refund")));
        this.server.verify(postRequestedFor(urlEqualTo("/v1/orders/ORD1/cancel")));
    }

    @Test
    void errosHttpERespostaSemIdFalham() {
        this.server.stubFor(post(urlEqualTo("/v1/orders")).willReturn(aResponse().withStatus(422)));
        assertThatThrownBy(() -> this.client.criarOrdem(comando())).isInstanceOf(NegocioException.class);

        this.server.stubFor(get(urlEqualTo("/v1/orders/ORD9")).willReturn(aResponse().withStatus(404)));
        assertThatThrownBy(() -> this.client.consultar("ORD9")).isInstanceOf(NegocioException.class);

        this.server.stubFor(post(urlEqualTo("/v1/orders/ORD1/cancel")).willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> this.client.cancelarOuEstornar("ORD1", new BigDecimal("1.00"), "created"))
                .isInstanceOf(NegocioException.class);

        assertThatThrownBy(() -> RestMercadoPagoClient.mapear(null)).isInstanceOf(NegocioException.class);
    }

    @Test
    void mapeiaStatusProcessadoAccreditedExpiredERefunded() {
        assertThat(RestMercadoPagoClient.mapear(json("{\"id\":\"A\",\"status\":\"accredited\"}")).aprovado()).isTrue();
        assertThat(RestMercadoPagoClient.mapear(json("{\"id\":\"B\",\"status\":\"expired\"}")).recusado()).isTrue();
        assertThat(RestMercadoPagoClient.mapear(json("{\"id\":\"C\",\"status\":\"canceled\"}")).recusado()).isTrue();
        assertThat(RestMercadoPagoClient.mapear(json("{\"id\":\"D\",\"status\":\"refunded\"}")).recusado()).isTrue();
        assertThat(RestMercadoPagoClient.mapear(json("{\"id\":\"E\",\"status\":\"cancelled\",\"qr_data\":\"\"}")).qrData()).isNull();
    }

    private static com.fasterxml.jackson.databind.JsonNode json(final String raw) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static CriarOrdemMp comando() {
        return new CriarOrdemMp(
                UUID.fromString("8f1c6a0e-1c2b-4d3e-9f01-223344556677"),
                "OS",
                new BigDecimal("10.00"),
                List.of(new ItemSnapshot("oleo", new BigDecimal("10.00")))
        );
    }
}
