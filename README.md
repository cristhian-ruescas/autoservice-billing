# autoservice-billing — Fase 4

Microsserviço de **orçamento e pagamento**. PostgreSQL próprio. Não acessa o banco da OS nem o Mongo da execução.

Parte da entrega **Fase 4** (saga + microsserviços).

## Qualidade

| Item | Status |
|------|--------|
| Testes | `./mvnw verify` |
| Cobertura | JaCoCo **≥ 80%** (gate no build) |
| OpenAPI | [`openapi.yaml`](./openapi.yaml) · Swagger `/swagger-ui.html` |
| Postman | `postman/` |
| CI/CD | `.github/workflows/ci-cd.yml` — Test → Docker → Deploy Kind + health |
| Lint / qualidade | pipeline + Sonar (quando `SONAR_TOKEN` presente) |

## Responsabilidades

- Consome `orcamento.solicitado`, gera PDF do orçamento e publica `orcamento.emitido`
- E-mail opcional (`MAIL_ENABLED=true`)
- Aprovação cria o pagamento; com simulação, a saga avança via webhook / resultado MP
- Reprovação publica `orcamento.reprovado` (sem pagamento)
- Compensação (`compensacao.pagamento.cancelar`) cancela o pagamento e publica `pagamento.cancelado`

## Pagamento (modo simulado)

Homolog e Compose usam **`PAGAMENTO_SIMULADO=true`**: cliente simulado, sem cobrança real.

Para Mercado Pago real: desligar simulação e configurar `MERCADOPAGO_ACCESS_TOKEN` + webhook `POST /pagamentos/webhook`.

## API

| Método | Path |
|--------|------|
| GET | `/orcamentos/{ordemServicoId}` |
| POST | `/orcamentos/{ordemServicoId}/aprovacao` |
| POST | `/orcamentos/{ordemServicoId}/reprovacao` |
| GET | `/pagamentos/{ordemServicoId}` |
| POST | `/pagamentos/webhook` |

## Rodar

```bash
./mvnw verify
./mvnw spring-boot:run
```

Porta `8082`. Malha completa: Compose em `autoservice-os/docker`.

Observabilidade: hub em `autoservice-os/docs/observability/`.

## CI/CD e Kubernetes

```bash
kubectl kustomize k8s
```

Secret de exemplo: `k8s/secret.example.yaml`. Em demonstração, mantenha `PAGAMENTO_SIMULADO=true`.
