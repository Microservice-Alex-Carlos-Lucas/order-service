package store.order.client;

import java.math.BigDecimal;

public record ExchangeResponse(
        BigDecimal sell,
        BigDecimal buy,
        String date,
        String idAccount
) {
}
