package nexus.nexus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Crear o editar un usuario desde el panel de administración. En edición, la contraseña es opcional. */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre es demasiado largo") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos son demasiado largos") String apellidos,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 120, message = "El correo es demasiado largo") String correo,
        @NotBlank(message = "Rol inválido") String rol,
        @Size(max = 100, message = "La contraseña es demasiado larga") String password) {
}
