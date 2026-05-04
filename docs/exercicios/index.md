# Exercícios da disciplina

Esta página agrega os exercícios feitos ao longo da disciplina, conforme a página
oficial de [Plataforma — Microsserviços](https://insper.github.io/platform/).

!!! note "Referência"
    Lista oficial de exercícios: [insper.github.io/platform/exercises](https://insper.github.io/platform/).

## Account Service

Serviço de cadastro/consulta de contas — entrega coletiva.

- Documentação: [microservices › services/account.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/services/account.md)
- Código-fonte: [account-service](https://github.com/Microservice-Alex-Carlos-Lucas/account-service)
- Modelo compartilhado: [account](https://github.com/Microservice-Alex-Carlos-Lucas/account)

## Auth Service

Emissão e validação de JWTs — entrega coletiva.

- Documentação: [microservices › services/auth.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/services/auth.md)
- Código-fonte: [auth-service](https://github.com/Microservice-Alex-Carlos-Lucas/auth-service)
- Modelo compartilhado: [auth](https://github.com/Microservice-Alex-Carlos-Lucas/auth)

## API Gateway

Spring Cloud Gateway com rotas para todos os microsserviços e injeção do header
`id-account` em rotas autenticadas — entrega coletiva.

- Documentação: [microservices › services/gateway.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/services/gateway.md)
- Código-fonte: [gateway-service](https://github.com/Microservice-Alex-Carlos-Lucas/gateway-service)

## Exchange API

Conversão de moedas (proxy à AwesomeAPI). Implementação do Alex.

- Documentação: [microservices › services/exchange.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/services/exchange.md)
- Código-fonte: [exchange](https://github.com/Microservice-Alex-Carlos-Lucas/exchange)

## Product API

CRUD de produtos. Implementação do Carlos.

- Documentação: [microservices › services/product.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/services/product.md)
- Código-fonte: [product-service](https://github.com/Microservice-Alex-Carlos-Lucas/product-service)

## Order API — minha entrega individual

API de pedidos com integração inter-serviço (Feign), conversão de moeda,
RBAC, observabilidade e Swagger.

- Documentação detalhada: [Order API › Visão geral](../individual/index.md)
- Endpoints: [Order API › Endpoints](../individual/endpoints.md)
- Bottlenecks: [Order API › Bottlenecks](../individual/bottlenecks.md)
- Testes: [Order API › Testes](../individual/testes.md)
- Código-fonte: [order-service](https://github.com/Microservice-Alex-Carlos-Lucas/order-service)

## Infraestrutura e DevOps

| Tópico | Status | Documentação |
|--------|--------|--------------|
| Docker / Compose | ✅ | [microservices › api/compose.yaml](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/api/compose.yaml) |
| CI/CD (Jenkins) | 🔄 | [microservices › infra/cicd.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/infra/cicd.md) |
| AWS / EKS | ⏳ | [microservices › infra/eks.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/infra/eks.md) |
| Load testing | ⏳ | [microservices › load-testing.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/load-testing.md) |
| Custos / SLA | ⏳ | [microservices › costs.md](https://github.com/Microservice-Alex-Carlos-Lucas/microservices/blob/main/docs/costs.md) |
