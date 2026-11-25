package nexus.nexus.dto;

import jakarta.validation.constraints.*;

public record ConsolaRequest(
        @NotBlank(message = "El nombre debe tener al menos 3 caracteres")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres") String nombre,
        @NotNull(message = "El precio debe ser mayor a 0")
        @Positive(message = "El precio debe ser mayor a 0") Integer precio,
        @NotBlank(message = "La categoría es obligatoria") String categoria,
        @NotNull(message = "Stock inválido")
        @Min(value = 0, message = "Stock inválido") Integer stock,
        Boolean oferta,
        @Min(value = 1, message = "El descuento debe estar entre 1 y 90")
        @Max(value = 90, message = "El descuento debe estar entre 1 y 90") Integer descuento,
        @Size(max = 300, message = "La imagen es demasiado larga") String imagen) {
}
