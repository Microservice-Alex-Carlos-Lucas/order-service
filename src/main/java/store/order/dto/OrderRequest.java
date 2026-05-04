package store.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotNull UUID idProduct,
            @Positive int quantity
    ) {
    }
}
