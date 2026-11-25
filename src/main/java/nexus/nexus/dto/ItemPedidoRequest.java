package nexus.nexus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "Falta el producto") Long consolaId,
        @NotNull(message = "La cantidad debe ser al menos 1")
        @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer cantidad) {
}
