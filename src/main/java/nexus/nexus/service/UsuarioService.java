package nexus.nexus.service;

import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.UsuarioRequest;
import nexus.nexus.dto.UsuarioResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;
import nexus.nexus.repository.BoletaRepository;
import nexus.nexus.repository.RefreshTokenRepository;
import nexus.nexus.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int LARGO_MINIMO_PASSWORD = 6;

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        String correo = req.correo().trim().toLowerCase();
        Rol rol = parsearRol(req.rol());
        if (req.password() == null || req.password().length() < LARGO_MINIMO_PASSWORD) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(req.nombre().trim())
                .apellidos(req.apellidos().trim())
                .correo(correo)
                .password(passwordEncoder.encode(req.password()))
                .rol(rol)
                .build());
        return UsuarioResponse.de(usuario);
    }

    /**
     * Edita al usuario. Si la contraseña viene vacía no se cambia.
     * Cambiar la contraseña o el rol cierra las sesiones abiertas de ese usuario.
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, String correoQuienEdita) {
        Usuario usuario = buscar(id);
        String correo = req.correo().trim().toLowerCase();
        Rol rol = parsearRol(req.rol());

        usuarioRepository.findByCorreo(correo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
                });
        boolean esUnoMismo = usuario.getCorreo().equalsIgnoreCase(correoQuienEdita);
        if (esUnoMismo && rol != usuario.getRol()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol");
        }

        boolean cerrarSesiones = rol != usuario.getRol();
        usuario.setNombre(req.nombre().trim());
        usuario.setApellidos(req.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < LARGO_MINIMO_PASSWORD) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres");
            }
            usuario.setPassword(passwordEncoder.encode(req.password()));
            cerrarSesiones = true;
        }
        if (cerrarSesiones) {
            refreshTokenRepository.deleteByUsuarioId(id);
        }
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, String correoQuienElimina) {
        Usuario usuario = buscar(id);
        if (usuario.getCorreo().equalsIgnoreCase(correoQuienElimina)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta");
        }
        if (boletaRepository.existsByUsuarioId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: el usuario tiene pedidos asociados");
        }
        refreshTokenRepository.deleteByUsuarioId(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Rol parsearRol(String texto) {
        try {
            return Rol.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rol inválido");
        }
    }
}
