package nexus.nexus.dto;

import nexus.nexus.model.Usuario;

/** Usuario público: nunca incluye la contraseña. */
public record UsuarioResponse(Long id, String nombre, String apellidos, String correo, String rol) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(), u.getRol().name());
    }
}
