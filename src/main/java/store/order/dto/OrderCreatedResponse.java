package store.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedResponse(
        UUID id,
        LocalDateTime date,
        List<Item> items,
        BigDecimal total
) {
    public record Item(
            UUID id,
            Product product,
            int quantity,
            BigDecimal total
    ) {
    }

    public record Product(
            UUID id
    ) {
    }
}
