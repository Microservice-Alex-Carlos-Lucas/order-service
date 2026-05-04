# Testes manuais

Roteiro de validação fim-a-fim que rodei localmente. Pré-requisitos:

- `db`, `product` e `exchange` no compose (com portas publicadas)
- `mvn spring-boot:run` no diretório do `order-service`
- 2 produtos cadastrados via Product API

```bash
ACC="00000000-0000-0000-0000-000000000001"
COFFEE_ID=$(curl -s -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -H "id-account: $ACC" \
  -d '{"name":"Coffee","price":12.00,"unit":"kg"}' | jq -r .id)
SUGAR_ID=$(curl -s -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -H "id-account: $ACC" \
  -d '{"name":"Sugar","price":3.00,"unit":"kg"}' | jq -r .id)
```

## Cenários cobertos

| # | Cenário | Esperado | Verificado |
|---|---------|----------|-----------|
| 1 | `POST /orders` válido (2×Coffee + 1×Sugar) | 201 · total `27.00` USD | ✅ |
| 2 | `GET /orders` lista o pedido | array com 1 entry, `total: 27.00` | ✅ |
| 3 | `GET /orders/{id}` (sem currency) | 200 · `currency: USD` | ✅ |
| 4 | `GET /orders/{id}?currency=BRL` | 200 · `currency: BRL` · totals convertidos | ✅ |
| 5 | `GET /orders/{id}` com `id-account` diferente | **404** | ✅ |
| 6 | `POST /orders` com produto inexistente | **400** | ✅ |
| 7 | `GET /orders/{id}?currency=ZZZ` | **422** | ✅ |
| 8 | `POST /orders` com `items: []` | **400** + ProblemDetail com `errors.items` | ✅ |
| 9 | `POST /orders` com `quantity: 0` | **400** + ProblemDetail com `errors.items[0].quantity` | ✅ |
| 10 | `GET /orders/all` sem `id-role` | **403** | ✅ |
| 11 | `GET /orders/all` com `id-role: admin` | 200 · todos os pedidos do sistema | ✅ |
| 12 | `/actuator/prometheus` expõe `orders_total` | Counter incrementa após cada `POST` | ✅ |
| 13 | `/swagger-ui/index.html` + `/v3/api-docs` | UI 302→200, JSON 200 | ✅ |

## Comandos

```bash
# 1. POST válido
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "id-account: $ACC" \
  -d "{\"items\":[{\"idProduct\":\"$COFFEE_ID\",\"quantity\":2},{\"idProduct\":\"$SUGAR_ID\",\"quantity\":1}]}"

# 2. GET list
curl -s -H "id-account: $ACC" http://localhost:8080/orders

# 3. GET detail (USD)
ORDER_ID=$(curl -s -H "id-account: $ACC" http://localhost:8080/orders | jq -r '.[0].id')
curl -s -H "id-account: $ACC" "http://localhost:8080/orders/$ORDER_ID"

# 4. GET detail (BRL)
curl -s -H "id-account: $ACC" "http://localhost:8080/orders/$ORDER_ID?currency=BRL"

# 5. 404 — outro usuário
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "id-account: 00000000-0000-0000-0000-000000000099" \
  "http://localhost:8080/orders/$ORDER_ID"

# 6. 400 — produto inexistente
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "id-account: $ACC" \
  -d '{"items":[{"idProduct":"00000000-0000-0000-0000-000000000000","quantity":1}]}'

# 7. 422 — moeda inválida
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "id-account: $ACC" \
  "http://localhost:8080/orders/$ORDER_ID?currency=ZZZ"

# 8/9. Validação
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "id-account: $ACC" \
  -d '{"items":[]}' | jq

# 10/11. RBAC
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "id-account: $ACC" http://localhost:8080/orders/all
curl -s -H "id-account: $ACC" -H "id-role: admin" http://localhost:8080/orders/all | jq

# 12. Prometheus
curl -s http://localhost:8080/actuator/prometheus | grep orders

# 13. OpenAPI
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs
open http://localhost:8080/swagger-ui/index.html
```

## Como rodar a stack para os testes

```bash
# microservices: sobe DB, Product e Exchange
cd microservices
docker compose --env-file .env -f api/compose.yaml up db product exchange -d

# order-service: roda local com mvn (mais fácil de iterar/debugar)
cd ../order-service
set -a; source .env; set +a
mvn spring-boot:run
```

## Próximos passos (load testing)

A próxima entrega do grupo é **load testing** (15 % do projeto). Cenários
candidatos para o Order:

- **Spike** de `POST /orders` para confirmar que `orders_total` cresce linearmente
  e que o p95 de latência fica controlado
- **Soak** com mistura de `POST /orders` + `GET /orders/{id}?currency=BRL`
  para medir o impacto da chamada ao Exchange
- **Carga adversarial** com `items` no limite (`@Size(max=100)`) para verificar que
  a defesa do Bottleneck 2 mantém latência baixa em rejeição
