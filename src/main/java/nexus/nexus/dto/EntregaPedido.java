package nexus.nexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntregaPedido(
        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 150, message = "La calle es demasiado larga") String calle,
        @Size(max = 20, message = "El departamento es demasiado largo") String departamento,
        @NotBlank(message = "La región es obligatoria")
        @Size(max = 100, message = "La región es demasiado larga") String region,
        @NotBlank(message = "La comuna es obligatoria")
        @Size(max = 100, message = "La comuna es demasiado larga") String comuna,
        @Size(max = 300, message = "Las indicaciones son demasiado largas") String indicaciones) {
}
