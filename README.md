# autoservice-billing

Microsserviço de orçamento e pagamento. PostgreSQL próprio. Não acessa o banco da OS nem o Mongo da execução.

Recebe `orcamento.solicitado`, gera um PDF, tenta enviar por e-mail (desligado se `MAIL_ENABLED` não for `true`) e publica `orcamento.emitido`. A aprovação chama o Mercado Pago. A reprovação publica `orcamento.reprovado` e não cria pagamento.

## Mercado Pago

Cliente HTTP em `RestMercadoPagoClient`:

- `POST /v1/orders` cria a ordem QR (`type=qr`, `external_pos_id`, itens e total).
- `GET /v1/orders/{id}` confirma o status no webhook.
- `POST /v1/orders/{id}/refund` estorna status `approved`, `processed` ou `accredited`.
- `POST /v1/orders/{id}/cancel` cancela os demais.

`POST /pagamentos/webhook` é público e idempotente. Só publica `pagamento.aprovado` quando a consulta da ordem confirma o status. O access token não entra no repositório: variável `MERCADOPAGO_ACCESS_TOKEN`.

Sem token, suba com `PAGAMENTO_SIMULADO=true` (padrão local e do Compose). O cliente simulado aprova na hora e devolve um QR fictício, para a saga andar. Uma cobrança escaneada no app do Mercado Pago continua dependendo do token sandbox e de uma URL HTTPS pública para o webhook.

## Rodar

```bash
./mvnw verify
./mvnw spring-boot:run
```

Porta `8082`. Swagger em `/swagger-ui.html`. OpenAPI em `openapi.yaml`. Collection em `postman/`.

A malha com OS, execução, Postgres, Mongo e RabbitMQ sobe pelo Compose do repositório `autoservice-os` (`docker/docker-compose.yml`).

Observabilidade Datadog (dashboard/monitors compartilhados): repositório `autoservice-os`, pasta `docs/observability/`.

## Kubernetes

```bash
kubectl kustomize k8s
```

No manifest de cluster, `PAGAMENTO_SIMULADO` fica `false` e o token vai no Secret (`k8s/secret.example.yaml`, fora do kustomize). O apply no EKS depende do ambiente AWS da fase 3.
