# Repositórios

Todos os repositórios usados no projeto vivem na organização
[Microservice-Alex-Carlos-Lucas](https://github.com/Microservice-Alex-Carlos-Lucas).
O repositório `microservices` é o "guarda-chuva" que agrega os outros como **submódulos**.

## Plataforma (raiz)

| Repositório | Descrição |
|-------------|-----------|
| [microservices](https://github.com/Microservice-Alex-Carlos-Lucas/microservices) | Compose, manifests Kubernetes, Jenkins, documentação de grupo, submódulos |

## Microsserviços

| Microsserviço | Responsável | Repositório | Stack |
|---------------|-------------|-------------|-------|
| **Order API** | **Lucas Ikawa** | [order-service](https://github.com/Microservice-Alex-Carlos-Lucas/order-service) | Java · Spring Boot 4 |
| Product API | Carlos | [product-service](https://github.com/Microservice-Alex-Carlos-Lucas/product-service) | Java · Spring Boot 4 |
| Exchange API | Alex Chequer | [exchange](https://github.com/Microservice-Alex-Carlos-Lucas/exchange) | Python · FastAPI |
| Account Service | colaborativo | [account-service](https://github.com/Microservice-Alex-Carlos-Lucas/account-service) | Java · Spring Boot |
| Auth Service | colaborativo | [auth-service](https://github.com/Microservice-Alex-Carlos-Lucas/auth-service) | Java · Spring Boot |
| Gateway Service | colaborativo | [gateway-service](https://github.com/Microservice-Alex-Carlos-Lucas/gateway-service) | Spring Cloud Gateway |

## Bibliotecas de domínio

| Repositório | Descrição |
|-------------|-----------|
| [account](https://github.com/Microservice-Alex-Carlos-Lucas/account) | Tipos compartilhados (Account model/parser) |
| [auth](https://github.com/Microservice-Alex-Carlos-Lucas/auth) | Tipos compartilhados de autenticação |

## Documentação

| Repositório | Conteúdo |
|-------------|----------|
| **este repositório** ([order-service](https://github.com/Microservice-Alex-Carlos-Lucas/order-service)) | Documentação individual + código do Order API |
| [microservices](https://github.com/Microservice-Alex-Carlos-Lucas/microservices) | Documentação de grupo (publicada em [GitHub Pages](https://microservice-alex-carlos-lucas.github.io/microservices/)) |
