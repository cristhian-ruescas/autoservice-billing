package com.autoservice.billing.pagamento;

import com.autoservice.billing.domain.ItemSnapshot;
import com.autoservice.billing.domain.NegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

public class RestMercadoPagoClient implements MercadoPagoClient {

    private final RestClient restClient;
    private final String externalPosId;
    private final ObjectMapper objectMapper;

    public RestMercadoPagoClient(final RestClient restClient, final String externalPosId) {
        this(restClient, externalPosId, new ObjectMapper());
    }

    public RestMercadoPagoClient(final RestClient restClient, final String externalPosId, final ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.externalPosId = externalPosId;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResultadoPagamentoMp criarOrdem(final CriarOrdemMp comando) {
        final ObjectNode body = corpoOrdem(comando);
        try {
            final JsonNode resposta = this.restClient.post()
                    .uri("/v1/orders")
                    .header("X-Idempotency-Key", comando.ordemServicoId().toString())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return mapear(resposta);
        } catch (RestClientResponseException exception) {
            throw new NegocioException("Mercado Pago recusou a ordem: " + exception.getStatusCode().value());
        }
    }

    @Override
    public ResultadoPagamentoMp consultar(final String mpOrderId) {
        try {
            final JsonNode resposta = this.restClient.get()
                    .uri("/v1/orders/{id}", mpOrderId)
                    .retrieve()
                    .body(JsonNode.class);
            return mapear(resposta);
        } catch (RestClientResponseException exception) {
            throw new NegocioException("Mercado Pago nao devolveu a ordem: " + exception.getStatusCode().value());
        }
    }

    @Override
    public void cancelarOuEstornar(final String mpOrderId, final BigDecimal valor, final String statusMp) {
        final boolean estorno = statusMp != null && (
                statusMp.equalsIgnoreCase("approved")
                        || statusMp.equalsIgnoreCase("processed")
                        || statusMp.equalsIgnoreCase("accredited")
        );
        try {
            if (estorno) {
                final ObjectNode body = this.objectMapper.createObjectNode();
                body.put("amount", dinheiro(valor));
                this.restClient.post()
                        .uri("/v1/orders/{id}/refund", mpOrderId)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
            } else {
                this.restClient.post()
                        .uri("/v1/orders/{id}/cancel", mpOrderId)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .retrieve()
                        .toBodilessEntity();
            }
        } catch (RestClientResponseException exception) {
            throw new NegocioException("Mercado Pago nao cancelou a ordem: " + exception.getStatusCode().value());
        }
    }

    private ObjectNode corpoOrdem(final CriarOrdemMp comando) {
        final ObjectNode body = this.objectMapper.createObjectNode();
        body.put("type", "qr");
        body.put("total_amount", dinheiro(comando.valor()));
        body.put("description", comando.descricao());
        body.put("external_reference", comando.ordemServicoId().toString());
        body.put("expiration_time", "PT30M");
        final ArrayNode payments = this.objectMapper.createArrayNode();
        final ObjectNode payment = this.objectMapper.createObjectNode();
        payment.put("amount", dinheiro(comando.valor()));
        payments.add(payment);
        final ObjectNode transactions = this.objectMapper.createObjectNode();
        transactions.set("payments", payments);
        body.set("transactions", transactions);
        final ObjectNode qr = this.objectMapper.createObjectNode();
        qr.put("external_pos_id", this.externalPosId);
        qr.put("mode", "dynamic");
        final ObjectNode config = this.objectMapper.createObjectNode();
        config.set("qr", qr);
        body.set("config", config);
        final ArrayNode items = this.objectMapper.createArrayNode();
        for (final ItemSnapshot item : comando.itens()) {
            final ObjectNode node = this.objectMapper.createObjectNode();
            node.put("title", item.descricao());
            node.put("unit_price", dinheiro(item.valor()));
            node.put("quantity", 1);
            node.put("unit_measure", "unit");
            items.add(node);
        }
        body.set("items", items);
        return body;
    }

    static ResultadoPagamentoMp mapear(final JsonNode root) {
        if (root == null || root.path("id").asText().isBlank()) {
            throw new NegocioException("Resposta do Mercado Pago sem id da ordem");
        }
        final String status = root.path("status").asText("created");
        String qr = texto(root, "qr_data");
        if (qr == null) {
            qr = root.path("type_response").path("qr_data").asText(null);
        }
        if (qr == null || qr.isBlank()) {
            qr = root.path("config").path("qr").path("qr_data").asText(null);
        }
        final String normalizado = status.toLowerCase(Locale.ROOT);
        final boolean aprovado = normalizado.equals("processed") || normalizado.equals("approved") || normalizado.equals("accredited");
        final boolean recusado = normalizado.equals("rejected")
                || normalizado.equals("cancelled")
                || normalizado.equals("canceled")
                || normalizado.equals("expired")
                || normalizado.equals("refunded");
        return new ResultadoPagamentoMp(root.path("id").asText(), status, qr, aprovado, recusado);
    }

    private static String texto(final JsonNode root, final String campo) {
        final JsonNode node = root.get(campo);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText();
    }

    private static String dinheiro(final BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
