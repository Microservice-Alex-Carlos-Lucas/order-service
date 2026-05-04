package store.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotEmpty @Size(max = 100) @Valid List<Item> items
) {
    public record Item(
            @NotNull UUID idProduct,
            @Positive @Max(10_000) int quantity
    ) {
    }
}
