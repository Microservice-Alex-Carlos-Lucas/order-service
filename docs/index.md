# Order API — Documentação Individual

**Aluno:** Lucas Ikawa
**Grupo:** Alex Chequer · Carlos · Lucas Ikawa
**Disciplina:** Plataformas, Microserviços, DevOps e APIs — Insper 2026.1
**Instrutor:** Humberto Sandmann

---

## O que é este site

Documentação individual da minha contribuição para o projeto da disciplina:
o microsserviço **Order API**, parte da plataforma de e-commerce desenvolvida
em grupo. Aqui você encontra:

- [Documentação dos exercícios](exercicios/index.md) realizados durante o curso
- [Documentação do projeto em grupo](projeto/index.md) (visão geral, arquitetura, apresentação)
- [Documentação do microsserviço Order](individual/index.md) (responsabilidade individual)
- Os **[bottlenecks implementados](individual/bottlenecks.md)** no Order API
- [Links para todos os repositórios](repositorios.md) usados pelo grupo

## Distribuição de microsserviços

| Membro | Microsserviço | Stack |
|--------|---------------|-------|
| Alex Chequer | Exchange API | Python · FastAPI |
| Carlos | Product API | Java · Spring Boot |
| **Lucas Ikawa** | **Order API** | **Java · Spring Boot** |

Os serviços compartilhados (Account, Auth, Gateway) foram desenvolvidos colaborativamente.

## Visão rápida do Order API

API REST que gerencia pedidos do usuário autenticado. Integra com **product-service**
(via OpenFeign) para validar produto e capturar preço, e com **exchange-service**
(via OpenFeign) para conversão de moeda nos totais retornados.

```mermaid
sequenceDiagram
    autonumber
    participant U as Usuário
    participant G as Gateway
    participant O as Order API
    participant P as Product API
    participant E as Exchange API

    U->>G: POST /orders {items}
    G->>O: encaminha + injeta id-account
    loop para cada item
        O->>P: GET /products/{id}
        P-->>O: {id, name, price, unit}
    end
    O->>O: persiste em orders.order
    O-->>U: 201 Created {id, items, total}

    U->>G: GET /orders/{id}?currency=BRL
    G->>O: encaminha + id-account
    O->>E: GET /exchanges/USD/BRL
    E-->>O: {sell, buy, date}
    O-->>U: 200 OK {currency: BRL, total convertido}
```

Detalhes em [Order API → Visão geral](individual/index.md).
