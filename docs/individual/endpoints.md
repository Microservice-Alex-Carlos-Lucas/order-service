# Endpoints

Todos os endpoints exigem **autenticação** — o Gateway injeta o header `id-account`
(UUID do usuário) em todas as chamadas autenticadas. Opcionalmente o `id-role`
controla autorização (`user` por padrão; `admin` desbloqueia o endpoint
administrativo `GET /orders/all`).

| Método | Rota | Autenticação | Descrição |
|--------|------|--------------|-----------|
| `POST` | `/orders` | `user` ou `admin` | Cria pedido |
| `GET` | `/orders` | `user` ou `admin` | Lista pedidos do usuário |
| `GET` | `/orders/{id}` | `user` ou `admin` | Detalha pedido (`?currency=` opcional) |
| `GET` | `/orders/all` | **`admin` apenas** | Lista pedidos de todos os usuários |

## POST /orders

Cria um pedido associado ao `id-account` autenticado. Para cada item, o serviço
chama `ProductClient.getProduct(idProduct)` para validar existência e capturar
o `unit_price` em **USD** (preço armazenado).

**Request**

```http
POST /orders
Content-Type: application/json
id-account: 00000000-0000-0000-0000-000000000001
id-role: user

{
  "items": [
    { "idProduct": "dc783683-c321-43c6-9861-b76876974d2c", "quantity": 2 },
    { "idProduct": "a675fa10-8cd8-42f7-a61d-ad187e44bd16", "quantity": 1 }
  ]
}
```

**Response (201)**

```json
{
  "id": "6e5f4ae5-b6d1-4003-ab95-42a1b6adef64",
  "date": "2026-05-03T22:38:09.59312",
  "items": [
    { "id": "...", "product": { "id": "dc783683-..." }, "quantity": 2, "total": 24.00 },
    { "id": "...", "product": { "id": "a675fa10-..." }, "quantity": 1, "total": 3.00 }
  ],
  "total": 27.00
}
```

| Status | Quando |
|--------|--------|
| **201** | Pedido criado com sucesso |
| **400** | Algum `idProduct` não existe (Product API → 404 traduzido para 400) |
| **400** | Validação falha (lista vazia, `quantity ≤ 0`, mais de 100 items, etc.) — corpo é `ProblemDetail` com mapa `errors` |
| **403** | `id-role` inválido |

## GET /orders

```http
GET /orders
id-account: 00000000-0000-0000-0000-000000000001
id-role: user
```

```json
[
  { "id": "...", "date": "2026-05-03T22:26:28.745446", "total": 27.00 },
  { "id": "...", "date": "2026-05-03T22:38:09.59312",  "total": 12.00 }
]
```

Retorna **somente** os pedidos cujo `id_account` casa com o header.

## GET /orders/{id}?currency=BRL

```http
GET /orders/d6f9cf19-342b-45f9-b153-117b17f7e044?currency=BRL
id-account: 00000000-0000-0000-0000-000000000001
```

```json
{
  "id": "d6f9cf19-342b-45f9-b153-117b17f7e044",
  "date": "2026-05-03T22:26:28.745446",
  "currency": "BRL",
  "items": [
    { "id": "...", "product": { "id": "dc783683-..." }, "quantity": 2, "total": 118.98 },
    { "id": "...", "product": { "id": "a675fa10-..." }, "quantity": 1, "total":  14.87 }
  ],
  "total": 133.85
}
```

| Status | Quando |
|--------|--------|
| **200** | Pedido detalhado (USD por padrão; convertido se `currency` enviado) |
| **404** | Pedido não pertence ao usuário do `id-account` |
| **422** | Moeda não suportada (Exchange retorna erro) |

A taxa usada é `response.buy` do Exchange (`GET /exchanges/USD/{currency}`).
A conversão é aplicada **por item** com `setScale(2, HALF_UP)` para preservar
consistência item-a-item; o total é a soma desses valores arredondados.

## GET /orders/all

Endpoint administrativo — exige `id-role: admin`. Útil para *ops*/*reporting*.

```http
GET /orders/all
id-account: 00000000-0000-0000-0000-000000000001
id-role: admin
```

| Status | Quando |
|--------|--------|
| **200** | Lista resumida de **todos** os pedidos (qualquer usuário) |
| **403** | `id-role` ausente, `user`, ou inválido |

## Erros — formato `ProblemDetail`

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/orders",
  "errors": {
    "items": "não deve estar vazio",
    "items[0].quantity": "deve ser maior que 0"
  }
}
```

Implementado em `GlobalExceptionHandler.java` estendendo `ResponseEntityExceptionHandler`.

## Documentação interativa

- [`/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html) — UI do SpringDoc
- [`/v3/api-docs`](http://localhost:8080/v3/api-docs) — JSON OpenAPI 3.x
