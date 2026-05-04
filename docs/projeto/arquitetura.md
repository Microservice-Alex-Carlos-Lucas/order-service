# Arquitetura

## Visão lógica

```mermaid
graph LR
    internet([Internet]) -->|request| gateway

    subgraph trusted[Trusted Layer]
        gateway --> auth
        gateway --> account
        gateway --> product
        gateway --> order
        gateway --> exchange
        account --> db[(PostgreSQL)]
        product --> db
        order --> db
    end

    order -->|Feign| product
    order -->|Feign| exchange
    exchange -->|HTTP| awesomeapi([AwesomeAPI - terceiros])
```

| Camada | Componente | Tecnologia |
|--------|-----------|------------|
| Borda | Gateway | Spring Cloud Gateway (WebFlux) |
| Identidade | Auth | Spring Boot · JWT |
| Domínio | Account · Product · Order | Spring Boot 4 · PostgreSQL · Flyway |
| Domínio | Exchange | FastAPI · proxy AwesomeAPI |
| Persistência | Database por serviço | PostgreSQL 17, schema-por-serviço |
| Observabilidade | Métricas | Prometheus (`/actuator/prometheus`) |
| Empacotamento | Containers | Docker · Compose · Kubernetes |

## Padrões adotados

- **API Gateway** centraliza CORS e injeta o header `id-account` (UUID) em rotas autenticadas
- **Database per service**: cada serviço tem seu próprio *schema* no Postgres (`accounts`, `products`, `orders`)
- **Migrations** versionadas com Flyway (V1__create_..._table.sql)
- **Inter-service via OpenFeign** (síncrono, declarativo) com URLs injetadas por env
  (`PRODUCT_SERVICE_URL`, `EXCHANGE_SERVICE_URL`)
- **Observabilidade**: cada serviço expõe `/actuator/prometheus` (Java) ou `/metrics` (Python)
- **Autorização** delegada ao gateway (validação JWT) + headers `id-account` / `id-role`
  consumidos por cada serviço

## Fluxo de criação de pedido

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuário
    participant G as Gateway
    participant O as Order API
    participant P as Product API
    participant DB as orders.order

    U->>G: POST /orders {items}
    G->>G: valida JWT
    G->>O: encaminha + injeta id-account
    loop por item
        O->>P: GET /products/{id}
        alt produto existe
            P-->>O: 200 {price}
        else
            P-->>O: 404
            O-->>U: 400 Product does not exist
        end
    end
    O->>DB: INSERT order + order_item (USD)
    DB-->>O: ok
    O-->>U: 201 Created
```

## Fluxo de conversão de moeda

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuário
    participant O as Order API
    participant E as Exchange API
    participant A as AwesomeAPI

    U->>O: GET /orders/{id}?currency=BRL
    O->>O: busca order (id-account scope) ou 404
    alt currency = USD ou ausente
        O-->>U: 200 totals em USD
    else
        O->>E: GET /exchanges/USD/BRL
        E->>A: GET last/USD-BRL
        A-->>E: {sell, buy}
        E-->>O: {sell, buy, date}
        Note over O: total *= rate.buy
        O-->>U: 200 totals em BRL
    end
```

## Decisões de design relevantes para o Order

- **Preço armazenado em USD** no `order_item.unit_price` — moeda única de armazenamento;
  conversão é aplicada na leitura.
- **Persistência transacional** (`@Transactional` em `create`) — todos os `order_item`
  ou nenhum.
- **Falha em produto inexistente** mapeada para `400 BAD_REQUEST` (a página do
  exercício pede esse status).
- **Falha de exchange** (moeda inválida ou upstream indisponível) mapeada para
  `422 UNPROCESSABLE_ENTITY` (também conforme a página).
- **Filtragem por `id_account`** garante que um usuário só enxergue os próprios
  pedidos (respondemos `404`, não `403`, pra não vazar existência).
