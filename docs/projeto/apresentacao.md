# Apresentação do projeto

## Vídeo (2-3 minutos)

!!! warning "Substituir antes da entrega"
    Faça upload do vídeo (YouTube/Drive/Loom) e cole o link/embed abaixo.

<!-- YouTube: substitua VIDEO_ID -->
<iframe
  width="100%"
  height="420"
  src="https://www.youtube.com/embed/VIDEO_ID"
  title="Apresentação Store Platform — Insper 2026.1"
  frameborder="0"
  allowfullscreen></iframe>

[Abrir vídeo em nova aba](https://www.youtube.com/watch?v=VIDEO_ID)

## Roteiro do vídeo (2-3 min)

1. **Quem somos e o que entregamos** (≈20 s) — apresentação do grupo, breve
   descrição da plataforma e divisão de responsabilidades.
2. **Arquitetura geral** (≈30 s) — diagrama com gateway, microsserviços,
   bancos por serviço, integração inter-serviço.
3. **Demo do fluxo principal** (≈60 s):
    - login no gateway → JWT
    - cria 2 produtos (Carlos · Product API)
    - cria pedido com esses produtos (Lucas · **Order API**)
    - lista pedidos
    - consulta o detalhe do pedido em **BRL**, mostrando o `id-account` filtrando os pedidos
4. **Bottlenecks implementados** (≈30 s) — cada membro destaca pelo menos um bottleneck:
    - Alex: caching de exchange rates
    - Carlos: caching de produtos (Redis)
    - Lucas: observabilidade (Prometheus/Micrometer counter) + validação de payload
5. **Próximos passos** (≈10 s) — AWS/EKS, load testing, custos.

## Slides

!!! warning "Substituir antes da entrega"
    Hospedar slides no Google Drive / Speaker Deck e linkar abaixo.

- [Slides — Store Platform (placeholder)](#)

## Demonstração ao vivo (script)

```bash
# 1. Sobe a stack completa
cd microservices
docker compose --env-file .env -f api/compose.yaml up -d

# 2. Cadastra produtos (Product API via gateway)
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{"name":"Coffee","price":12.00,"unit":"kg"}'

# 3. Cria pedido (Order API)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{"items":[{"idProduct":"<uuid>","quantity":2}]}'

# 4. Consulta detalhe convertido em BRL
curl "http://localhost:8080/orders/<order-id>?currency=BRL" \
  -H "Authorization: Bearer $JWT"

# 5. Métricas de pedidos criados
curl http://localhost:8080/actuator/prometheus | grep orders_total
```
