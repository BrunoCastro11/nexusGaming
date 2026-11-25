package nexus.nexus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientePedido(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 80, message = "El nombre es demasiado largo") String nombre,
        @NotBlank(message = "Los apellidos del cliente son obligatorios")
        @Size(max = 80, message = "Los apellidos son demasiado largos") String apellidos,
        @Email(message = "El correo del cliente no es válido")
        @Size(max = 120, message = "El correo es demasiado largo") String correo) {
}
