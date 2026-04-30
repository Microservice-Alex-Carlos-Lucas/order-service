# Order Service — Handoff

Hi Lucas! This repo has the full project scaffold ready. You only need to write
the business logic. Below is a clear picture of what's done and what you need to build.

---

## What's already done

| Item | Location |
|------|----------|
| Spring Boot 4.0.3 / Java 25 project | `pom.xml` |
| PostgreSQL + Flyway config (schema: `orders`) | `src/main/resources/application.yaml` |
| **OpenFeign** already set up (to call product + exchange) | `pom.xml` + `application.yaml` |
| Inter-service URLs pre-configured | `PRODUCT_SERVICE_URL`, `EXCHANGE_SERVICE_URL` in yaml + k8s configmap |
| Prometheus metrics at `/actuator/prometheus` | `pom.xml` + `application.yaml` |
| Dockerfile (`eclipse-temurin:25`) | `Dockerfile` |
| Jenkins CI/CD pipeline (`cheqr/order`) | `Jenkinsfile` |
| Kubernetes manifests | `k8s/` (secrets, configmap, deployment, service) |
| Gateway route `/orders/**` → this service | already pushed to gateway-service |
| Docker Compose integration with `depends_on: [db, product, exchange]` | `api/compose.yaml` |

---

## What you need to implement

### 1. Flyway migrations

Create `src/main/resources/db/migration/V1__create_orders_tables.sql`:

```sql
CREATE TABLE IF NOT EXISTS orders.order (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    id_account UUID      NOT NULL,
    date       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders.order_item (
    id         UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    id_order   UUID    NOT NULL REFERENCES orders.order(id),
    id_product UUID    NOT NULL,
    quantity   INT     NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);
```

### 2. File structure

```
src/main/java/store/order/
├── OrderApplication.java         ← already exists, @EnableFeignClients included
├── Order.java                    ← @Entity
├── OrderItem.java                ← @Entity
├── OrderRepository.java          ← extends JpaRepository<Order, UUID>
├── OrderItemRepository.java
├── OrderService.java             ← business logic
├── OrderController.java          ← REST endpoints
├── dto/
│   ├── OrderRequest.java         ← { items: [{ idProduct, quantity }] }
│   ├── OrderSummaryResponse.java ← { id, date, total }
│   └── OrderDetailResponse.java  ← { id, date, currency, items, total }
└── client/
    ├── ProductClient.java        ← Feign client → product-service
    └── ExchangeClient.java       ← Feign client → exchange-service
```

### 3. Feign clients

**ProductClient** — calls the product-service:

```java
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable UUID id,
                               @RequestHeader("id-account") String idAccount);
}
```

`ProductResponse` record: `{ UUID id, String name, BigDecimal price, String unit }`

**ExchangeClient** — calls the exchange-service:

```java
@FeignClient(name = "exchange-service")
public interface ExchangeClient {
    @GetMapping("/exchanges/{from}/{to}")
    ExchangeResponse getRate(@PathVariable String from,
                             @PathVariable String to,
                             @RequestHeader("id-account") String idAccount);
}
```

`ExchangeResponse` record: `{ BigDecimal sell, BigDecimal buy, String date, String idAccount }`

The URLs are injected automatically from `application.yaml` via the `spring.cloud.openfeign.client.config` block already configured there.

### 4. REST endpoints to implement

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/orders` | Required | Create order for the authenticated user |
| `GET` | `/orders` | Required | List all orders for the authenticated user |
| `GET` | `/orders/{id}` | Required | Get order details; accepts `?currency=BRL` |

**POST /orders — request body:**
```json
{
  "items": [
    { "idProduct": "0195abfb-7074-73a9-9d26-b4b9fbaab0a8", "quantity": 2 },
    { "idProduct": "0195abfe-e416-7052-be3b-27cdaf12a984", "quantity": 1 }
  ]
}
```

For each item, call `ProductClient.getProduct(idProduct)` to get the price, then persist.

**GET /orders — response:**
```json
[
  { "id": "...", "date": "2025-09-01T12:30:00", "total": 26.44 },
  { "id": "...", "date": "2025-10-09T03:21:57", "total": 18.60 }
]
```

Only return orders whose `id_account` matches the current user.

**GET /orders/{id}?currency=BRL — response (USD, default):**
```json
{
  "id": "...",
  "date": "2025-09-01T12:30:00",
  "currency": "USD",
  "items": [
    { "id": "...", "product": { "id": "..." }, "quantity": 2, "total": 20.24 }
  ],
  "total": 26.44
}
```

When `?currency=BRL` is provided:
1. Call `ExchangeClient.getRate("USD", "BRL", idAccount)`
2. Multiply totals by `response.buy` (the buy rate)
3. Return `"currency": "BRL"` in the response

Return `404` if the order doesn't belong to the current user.

### 5. Reading the authenticated user

The gateway injects `id-account` on every authenticated request:

```java
@PostMapping("/orders")
public ResponseEntity<OrderDetailResponse> create(
        @RequestBody OrderRequest request,
        @RequestHeader("id-account") String idAccount) {
    // idAccount is the UUID of the logged-in user
}
```

Store `idAccount` as the `id_account` column in the `orders.order` table to filter orders
per user later.

---

## Running locally

```bash
# 1. Start DB + product + exchange (from microservices root)
docker compose -f api/compose.yaml up db product exchange -d

# 2. Copy and fill in env vars
cp .env.example .env
# Edit .env: set DATABASE_HOST=localhost, PRODUCT_SERVICE_URL=http://localhost:8081
# (adjust port if product-service runs on a different port locally)

# 3. Run the service
mvn spring-boot:run
```

Service starts at `http://localhost:8080`.

## Building and pushing Docker image

```bash
mvn -B -DskipTests clean package
docker build -t cheqr/order .
```

The Jenkinsfile handles this automatically on push.

## K8s

Manifests are in `k8s/`. The configmap already has `PRODUCT_SERVICE_URL=http://product:8080`
and `EXCHANGE_SERVICE_URL=http://exchange:8000` — these match the k8s service names exactly,
so no changes needed there.

Update the base64-encoded credentials in `k8s/secrets.yaml` before deploying:
```bash
echo -n "yourpassword" | base64
```

---

## Questions?

Check the root repo docs: https://github.com/Microservice-Alex-Carlos-Lucas/microservices
or ping Alex.
