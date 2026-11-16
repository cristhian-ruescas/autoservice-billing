# autoservice-billing

Microsserviço de orçamento e pagamento. PostgreSQL próprio. Não acessa o banco da OS nem o Mongo da execução.

## Responsabilidades

- Consome `orcamento.solicitado`, gera PDF do orçamento e publica `orcamento.emitido`
- E-mail opcional (`MAIL_ENABLED=true`)
- Aprovação cria o pagamento e publica `pagamento.aprovado` (ou `pagamento.recusado`)
- Reprovação publica `orcamento.reprovado` (sem pagamento)
- Compensação (`compensacao.pagamento.cancelar`) cancela o pagamento e publica `pagamento.cancelado`

## Pagamento (modo simulado)

Por padrão a malha local e o compose usam **`PAGAMENTO_SIMULADO=true`**: o cliente simulado aprova na hora e a saga segue sem cobrança real.

Para integração externa real, desligue a simulação e configure `MERCADOPAGO_ACCESS_TOKEN` (e webhook público em `POST /pagamentos/webhook`).

## Rodar

```bash
./mvnw verify
./mvnw spring-boot:run
```

Porta `8082`. Swagger: `/swagger-ui.html`. OpenAPI: `openapi.yaml`. Postman: `postman/`.

A malha completa (OS + execução + bancos + RabbitMQ) sobe pelo Compose em `autoservice-os/docker`.

Observabilidade (Datadog): hub em `autoservice-os/docs/observability/`.

## CI/CD e Kubernetes

Pipeline em `.github/workflows/` (testes, qualidade, imagem, deploy).

```bash
kubectl kustomize k8s
```

Secret de exemplo: `k8s/secret.example.yaml` (fora do kustomize). Em ambientes de demonstração, mantenha `PAGAMENTO_SIMULADO=true`.
