package nexus.nexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El nombre debe tener al menos 2 caracteres")
        @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres") String nombre) {
}
