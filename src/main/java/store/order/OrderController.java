package store.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import store.order.dto.OrderCreatedResponse;
import store.order.dto.OrderDetailResponse;
import store.order.dto.OrderRequest;
import store.order.dto.OrderSummaryResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreatedResponse create(
            @RequestHeader("id-account") String idAccount,
            @Valid @RequestBody OrderRequest request
    ) {
        return service.create(request, idAccount);
    }

    @GetMapping
    public List<OrderSummaryResponse> list(
            @RequestHeader("id-account") String idAccount
    ) {
        return service.list(idAccount);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse get(
            @RequestHeader("id-account") String idAccount,
            @PathVariable UUID id,
            @RequestParam(required = false) String currency
    ) {
        return service.get(id, currency, idAccount);
    }
}
