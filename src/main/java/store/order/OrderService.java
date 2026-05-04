package store.order;

import feign.FeignException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import store.order.client.ExchangeClient;
import store.order.client.ExchangeResponse;
import store.order.client.ProductClient;
import store.order.client.ProductResponse;
import store.order.dto.OrderCreatedResponse;
import store.order.dto.OrderDetailResponse;
import store.order.dto.OrderRequest;
import store.order.dto.OrderSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final ExchangeClient exchangeClient;
    private final MeterRegistry meterRegistry;

    private Counter ordersCreated;

    @PostConstruct
    void initMetrics() {
        ordersCreated = Counter.builder("orders.created")
                .description("Total number of orders created")
                .register(meterRegistry);
    }

    @Transactional
    public OrderCreatedResponse create(OrderRequest request, String idAccount) {
        UUID accountId = parseAccount(idAccount);

        Order order = new Order();
        order.setIdAccount(accountId);
        order.setDate(LocalDateTime.now());

        for (OrderRequest.Item item : request.items()) {
            ProductResponse product = fetchProduct(item.idProduct(), idAccount);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setIdProduct(product.id());
            orderItem.setQuantity(item.quantity());
            orderItem.setUnitPrice(product.price());
            order.getItems().add(orderItem);
        }

        Order saved = orderRepository.save(order);
        ordersCreated.increment();
        return toCreated(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> list(String idAccount) {
        UUID accountId = parseAccount(idAccount);
        return orderRepository.findByIdAccount(accountId).stream()
                .map(order -> new OrderSummaryResponse(order.getId(), order.getDate(), totalOf(order)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listAll() {
        return orderRepository.findAll().stream()
                .map(order -> new OrderSummaryResponse(order.getId(), order.getDate(), totalOf(order)))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse get(UUID id, String currency, String idAccount) {
        UUID accountId = parseAccount(idAccount);
        Order order = orderRepository.findByIdAndIdAccount(id, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (currency == null || currency.isBlank() || DEFAULT_CURRENCY.equalsIgnoreCase(currency)) {
            return toDetail(order, DEFAULT_CURRENCY, BigDecimal.ONE);
        }

        ExchangeResponse rate = fetchRate(currency.toUpperCase(), idAccount);
        return toDetail(order, currency.toUpperCase(), rate.buy());
    }

    private ProductResponse fetchProduct(UUID idProduct, String idAccount) {
        try {
            return productClient.getProduct(idProduct, idAccount);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not exist: " + idProduct);
        }
    }

    private ExchangeResponse fetchRate(String currency, String idAccount) {
        try {
            return exchangeClient.getRate(DEFAULT_CURRENCY, currency, idAccount);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Currency not supported: " + currency);
        }
    }

    private OrderCreatedResponse toCreated(Order order) {
        List<OrderCreatedResponse.Item> items = order.getItems().stream()
                .map(item -> new OrderCreatedResponse.Item(
                        item.getId(),
                        new OrderCreatedResponse.Product(item.getIdProduct()),
                        item.getQuantity(),
                        item.getUnitPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BigDecimal total = items.stream()
                .map(OrderCreatedResponse.Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new OrderCreatedResponse(order.getId(), order.getDate(), items, total);
    }

    private OrderDetailResponse toDetail(Order order, String currency, BigDecimal rate) {
        List<OrderDetailResponse.Item> items = order.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .multiply(rate)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new OrderDetailResponse.Item(
                            item.getId(),
                            new OrderDetailResponse.Product(item.getIdProduct()),
                            item.getQuantity(),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(OrderDetailResponse.Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new OrderDetailResponse(order.getId(), order.getDate(), currency, items, total);
    }

    private BigDecimal totalOf(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private UUID parseAccount(String idAccount) {
        try {
            return UUID.fromString(idAccount);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid id-account header");
        }
    }
}
