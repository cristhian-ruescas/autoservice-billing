package com.autoservice.billing.api;

import com.autoservice.billing.config.SecurityConfig;
import com.autoservice.billing.domain.NaoEncontradoException;
import com.autoservice.billing.domain.NegocioException;
import com.autoservice.billing.security.JwtService;
import com.autoservice.billing.service.BillingService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrcamentoController.class, PagamentoController.class})
@Import({SecurityConfig.class, JwtService.class, ApiExceptionHandler.class, com.autoservice.billing.observability.ObservabilityWebMvcTestConfig.class})
@TestPropertySource(properties = "autoservice.jwt.secret=test-secret-test-secret-test-secret")
class BillingApiTest {

    private static final String SEGREDO = "test-secret-test-secret-test-secret";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BillingService billingService;

    @Test
    void endpointsAutenticadosEWebhookPublico() throws Exception {
        final UUID ordemId = UUID.randomUUID();
        when(this.billingService.consultarOrcamento(ordemId)).thenReturn(
                new OrcamentoResponse(UUID.randomUUID(), ordemId, "EMITIDO", new BigDecimal("10.00"), false)
        );
        when(this.billingService.aprovar(ordemId)).thenReturn(
                new PagamentoResponse(UUID.randomUUID(), ordemId, "APROVADO", "ORD", "qr", new BigDecimal("10.00"))
        );
        when(this.billingService.reprovar(ordemId)).thenReturn(
                new OrcamentoResponse(UUID.randomUUID(), ordemId, "REPROVADO", new BigDecimal("10.00"), true)
        );
        when(this.billingService.consultarPagamento(ordemId)).thenReturn(
                new PagamentoResponse(UUID.randomUUID(), ordemId, "APROVADO", "ORD", "qr", new BigDecimal("10.00"))
        );

        this.mvc.perform(get("/orcamentos/" + ordemId).header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMITIDO"));
        this.mvc.perform(post("/orcamentos/" + ordemId + "/aprovacao").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mpOrderId").value("ORD"));
        this.mvc.perform(post("/orcamentos/" + ordemId + "/reprovacao").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk());
        this.mvc.perform(get("/pagamentos/" + ordemId).header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk());
        this.mvc.perform(post("/pagamentos/webhook").contentType(MediaType.APPLICATION_JSON).content("{\"data\":{\"id\":\"ORD\"}}"))
                .andExpect(status().isOk());
        this.mvc.perform(get("/orcamentos/" + ordemId)).andExpect(status().isUnauthorized());
        this.mvc.perform(get("/orcamentos/" + ordemId).header("Authorization", "Bearer token-ruim"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void traduzExcecoes() throws Exception {
        when(this.billingService.consultarOrcamento(any())).thenThrow(new NaoEncontradoException("ausente"));
        when(this.billingService.aprovar(any())).thenThrow(new NegocioException("reprovado"));
        final String auth = "Bearer " + token();

        this.mvc.perform(get("/orcamentos/" + UUID.randomUUID()).header("Authorization", auth))
                .andExpect(status().isNotFound());
        this.mvc.perform(post("/orcamentos/" + UUID.randomUUID() + "/aprovacao").header("Authorization", auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("reprovado"));
    }

    private static String token() {
        return JWT.create()
                .withSubject("admin")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
                .sign(Algorithm.HMAC256(SEGREDO));
    }
}
