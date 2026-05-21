# order-service

RESTful API for managing orders in the store platform. Calls product-service for product
details and exchange-service for currency conversion.

## Stack

- Java 25 / Spring Boot 4.0.3
- PostgreSQL (schema: `orders`)
- Flyway migrations
- OpenFeign clients for product-service and exchange-service
- Prometheus metrics at `/actuator/prometheus`

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/orders` | Create an order for the authenticated user |
| `GET` | `/orders` | List all orders for the authenticated user |
| `GET` | `/orders/{id}` | Get order details (optional `?currency=BRL`) |

## Running locally

```bash
cp .env.example .env
# Edit .env with product and exchange service URLs
mvn spring-boot:run
```

## Docker

```bash
mvn -B -DskipTests clean package
docker build -t cheqr/order .
docker run -p 8080:8080 --env-file .env cheqr/order
```

## Inter-service communication

| Service | Default URL | Env var |
|---------|------------|---------|
| product-service | `http://product:8080` | `PRODUCT_SERVICE_URL` |
| exchange-service | `http://exchange:8000` | `EXCHANGE_SERVICE_URL` |

## Documentation (MkDocs)

Individual student documentation lives in [`docs/`](./docs/) and is built with
[MkDocs Material](https://squidfunk.github.io/mkdocs-material/). Includes:
project overview, architecture, endpoints, **bottlenecks**, presentation/video
placeholders, and links to every group repository.

### Build locally

```bash
pip install -r docs-requirements.txt
mkdocs serve     # http://localhost:8000
```

### Deploy to GitHub Pages

A push to `main` triggers `.github/workflows/docs.yml` which runs `mkdocs gh-deploy`.
The site is published at:

> https://microservice-alex-carlos-lucas.github.io/order-service/
