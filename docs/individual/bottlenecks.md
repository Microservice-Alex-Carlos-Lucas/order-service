# Bottlenecks implementados

A página de exercícios pede **ao menos 2 bottlenecks por integrante**. No
Order API entreguei **três**, todos rodando e verificáveis localmente.

Em microsserviços, "bottleneck" pode significar (a) um gargalo conhecido
e mitigado, (b) um mecanismo defensivo que impede gargalos sob carga, ou
(c) instrumentação para *identificar* novos gargalos. Os três abaixo
cobrem essas três interpretações.

---

## Bottleneck 1 — Observabilidade com Micrometer + Prometheus

**Categoria:** Identificação de gargalos · *capacity planning*

### Problema

Sem métricas, é impossível saber onde a aplicação está lenta sob carga.
A própria página de exercícios destaca *"Observability (metrics, logs)"*
como Nice to Have — e bottlenecks só podem ser otimizados se forem antes
**medidos**.

### Solução

1. Habilitei o **Spring Boot Actuator + micrometer-registry-prometheus**
   (`pom.xml`) — todas as métricas padrão (HTTP, JVM, Hikari, sistema)
   ficam expostas em `/actuator/prometheus`.
2. Adicionei um **counter customizado** `orders.created` em
   `OrderService.java`:

```java
@PostConstruct
void initMetrics() {
    ordersCreated = Counter.builder("orders.created")
            .description("Total number of orders created")
            .register(meterRegistry);
}

@Transactional
public OrderCreatedResponse create(OrderRequest request, String idAccount) {
    /* ... persiste ... */
    ordersCreated.increment();
    return toCreated(saved);
}
```

3. `application.yaml` libera o endpoint Prometheus:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

### Verificação

```bash
$ curl -s http://localhost:8080/actuator/prometheus | grep orders_total
# HELP orders_total Total number of orders created
# TYPE orders_total counter
orders_total 1.0
```

(Micrometer remove o sufixo `_created` por convenção Prometheus e adiciona `_total`.)

### O que isso desbloqueia

- **Dashboards Grafana**: throughput de pedidos, p95 do endpoint POST,
  taxa de erro 4xx/5xx por rota
- **Alertas**: queda no `rate(orders_total[5m])` ou aumento no
  `http_server_requests_seconds{outcome="SERVER_ERROR"}`
- **HPA do Kubernetes**: pode escalar com base em métricas customizadas

### Arquivos relevantes

- `pom.xml` (linhas com `micrometer-registry-prometheus`)
- `src/main/java/store/order/OrderService.java`
- `src/main/resources/application.yaml`

---

## Bottleneck 2 — Defesa contra payload abusivo (validação + ProblemDetail)

**Categoria:** Mitigação preventiva · *fail fast*

### Problema

O `POST /orders` faz uma chamada **síncrona Feign** ao Product Service
**para cada item** do pedido. Um cliente malicioso ou bug no front-end
poderia enviar `items: [...10000...]`, causando:

- **N+1 chamadas Feign** ao Product (10.000 round-trips)
- **10.000 INSERTs** numa transação enorme — lock e *bloat* no Postgres
- Latência alta na resposta — outros pedidos legítimos sofrem na fila

Sem proteção, esse é um **DoS trivial**.

### Solução

Validação declarativa **antes** de qualquer trabalho ser feito (controllers
rejeitam o payload no `@Valid` do Spring sem entrar no service):

```java
public record OrderRequest(
        @NotEmpty @Size(max = 100) @Valid List<Item> items
) {
    public record Item(
            @NotNull UUID idProduct,
            @Positive @Max(10_000) int quantity
    ) {}
}
```

Limites escolhidos:

| Constraint | Valor | Por quê |
|------------|-------|---------|
| `items.@NotEmpty` | obrigatório | Pedido vazio não faz sentido |
| `items.@Size(max = 100)` | 100 | Volume realista de carrinho; limita Feign N+1 |
| `quantity.@Positive` | > 0 | Quantidade negativa/zero é erro |
| `quantity.@Max(10_000)` | 10.000 | Limite de sanidade — evita overflow em multiplicação |

Quando a validação falha, um `@RestControllerAdvice` global converte o
`MethodArgumentNotValidException` num `ProblemDetail` estruturado:

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders h, HttpStatusCode s, WebRequest r) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                fieldErrors.put(e.getField(), e.getDefaultMessage()));

        ProblemDetail body = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        body.setTitle("Validation failed");
        body.setDetail("One or more fields are invalid");
        body.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
```

### Verificação

```bash
$ curl -s -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -H "id-account: $ACC" \
    -d '{"items":[]}'

{
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/orders",
  "errors": { "items": "não deve estar vazio" }
}
```

```bash
$ curl -s -X POST http://localhost:8080/orders \
    -d '{"items":[{"idProduct":"...","quantity":0}]}'

{ "errors": { "items[0].quantity": "deve ser maior que 0" } }
```

### Impacto

- **Custo de uma requisição abusiva** cai de O(N) chamadas Feign + O(N) INSERTs
  para apenas o parsing do JSON e um `400` imediato.
- **Diagnóstico fica trivial**: o cliente recebe exatamente quais campos
  falharam, não uma string genérica.

### Arquivos relevantes

- `src/main/java/store/order/dto/OrderRequest.java`
- `src/main/java/store/order/GlobalExceptionHandler.java`

---

## Bottleneck 3 — Tradução defensiva de erros do Exchange (422)

**Categoria:** Resiliência inter-serviço · *bulkhead*

### Problema

`GET /orders/{id}?currency=ZZZ` chamava o Exchange Service via Feign. Se a
moeda não é suportada, o Exchange retorna **502** (porque a API upstream
AwesomeAPI devolve 404 para `USD-ZZZ`). Sem tratamento, esse 502 vazaria
para o cliente como **500 Internal Server Error**, mascarando que o
problema é **input do cliente**, não falha do serviço.

```text
$ curl http://localhost:8000/exchanges/USD/ZZZ
{"detail":"Exchange rate unavailable: 404 Client Error: Not Found ..."}
HTTP 502
```

A página de exercícios é explícita: *"422: Currency code unsupported"*.

### Solução

Captura *qualquer* `FeignException` na chamada ao Exchange e traduz
para `422 UNPROCESSABLE_ENTITY`, sinalizando que o **input do cliente** é
inválido, não o serviço:

```java
private ExchangeResponse fetchRate(String currency, String idAccount) {
    try {
        return exchangeClient.getRate(DEFAULT_CURRENCY, currency, idAccount);
    } catch (FeignException e) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Currency not supported: " + currency);
    }
}
```

Mesmo padrão para `ProductClient.getProduct`, mas mapeado para `400` (a
spec do exercício diz: *"400: Product does not exist"*):

```java
private ProductResponse fetchProduct(UUID idProduct, String idAccount) {
    try {
        return productClient.getProduct(idProduct, idAccount);
    } catch (FeignException.NotFound e) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Product does not exist: " + idProduct);
    }
}
```

### Verificação

```bash
$ curl -o /dev/null -w "HTTP %{http_code}\n" \
    -H "id-account: $ACC" \
    "http://localhost:8080/orders/$ORDER_ID?currency=ZZZ"
HTTP 422

$ curl -o /dev/null -w "HTTP %{http_code}\n" \
    -X POST http://localhost:8080/orders \
    -H "id-account: $ACC" \
    -H "Content-Type: application/json" \
    -d '{"items":[{"idProduct":"00000000-0000-0000-0000-000000000000","quantity":1}]}'
HTTP 400
```

### Impacto

- **Falha no terceiro (AwesomeAPI)** ou **input ruim** não viram 5xx —
  o cliente sabe se o problema está com ele (4xx) ou com o servidor (5xx).
- **Métricas de erro** ficam confiáveis: `http_server_requests` com
  `outcome="SERVER_ERROR"` representa falhas reais, não erros do cliente.
- **Painel de SLO** pode contar 4xx separado de 5xx.

### Arquivos relevantes

- `src/main/java/store/order/OrderService.java` (`fetchProduct`, `fetchRate`)

---

## Resumo

| # | Bottleneck | Tipo | Verificação |
|---|------------|------|-------------|
| 1 | Observabilidade — counter `orders_total` | Identificação | `curl /actuator/prometheus \| grep orders` |
| 2 | Validação + ProblemDetail global | Defesa preventiva | `POST /orders {"items":[]}` retorna 400 estruturado |
| 3 | Tradução de erros Feign (4xx/5xx → 400/422) | Resiliência | `GET /orders/{id}?currency=ZZZ` retorna 422 |

Cada bottleneck tem testes manuais documentados em [Order API › Testes](testes.md).
