package nexus.nexus.service;

import nexus.nexus.dto.UsuarioRequest;
import nexus.nexus.dto.UsuarioResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;
import nexus.nexus.repository.BoletaRepository;
import nexus.nexus.repository.RefreshTokenRepository;
import nexus.nexus.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock BoletaRepository boletaRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UsuarioService service;

    private Usuario admin() {
        return Usuario.builder().id(1L).nombre("admin").apellidos("admin")
                .correo("admin@gmail.com").password("hash").rol(Rol.ADMIN).build();
    }

    private Usuario cliente() {
        return Usuario.builder().id(2L).nombre("Vicente").apellidos("Vasquez")
                .correo("vicente6@gmail.com").password("hash").rol(Rol.CLIENTE).build();
    }

    // --- crear ---

    @Test
    void crearGuardaElUsuarioConLaClaveEncriptadaYElRolEnMayuscula() {
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("ana12345")).thenReturn("hash-ana");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resp = service.crear(
                new UsuarioRequest("Ana", "Perez", "ANA@gmail.com", "vendedor", "ana12345"));

        assertEquals("VENDEDOR", resp.rol());
        assertEquals("ana@gmail.com", resp.correo());
    }

    @Test
    void crearConClaveCortaDa400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(new UsuarioRequest("Ana", "Perez", "ana@gmail.com", "CLIENTE", "123")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void crearConRolInvalidoDa400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(new UsuarioRequest("Ana", "Perez", "ana@gmail.com", "SUPERMAN", "ana12345")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Rol inválido", ex.getMessage());
    }

    @Test
    void crearConCorreoExistenteDa409() {
        when(usuarioRepository.existsByCorreoIgnoreCase("admin@gmail.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(new UsuarioRequest("Otro", "Admin", "admin@gmail.com", "ADMIN", "secreto1")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // --- eliminar ---

    @Test
    void unAdminNoPuedeEliminarSuPropiaCuenta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin()));

        ApiException ex = assertThrows(ApiException.class, () -> service.eliminar(1L, "admin@gmail.com"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void noSePuedeEliminarUnUsuarioConPedidos() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(cliente()));
        when(boletaRepository.existsByUsuarioId(2L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.eliminar(2L, "admin@gmail.com"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void eliminarCierraLasSesionesYBorraAlUsuario() {
        Usuario usuario = cliente();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(boletaRepository.existsByUsuarioId(2L)).thenReturn(false);

        service.eliminar(2L, "admin@gmail.com");

        verify(refreshTokenRepository).deleteByUsuarioId(2L);
        verify(usuarioRepository).delete(usuario);
    }

    // --- actualizar ---

    @Test
    void unAdminNoPuedeCambiarSuPropioRol() {
        Usuario admin = admin();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByCorreo("admin@gmail.com")).thenReturn(Optional.of(admin));

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizar(1L,
                new UsuarioRequest("admin", "admin", "admin@gmail.com", "CLIENTE", null), "admin@gmail.com"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void cambiarLaClaveCierraLasSesionesDelUsuario() {
        Usuario usuario = cliente();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("vicente6@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.actualizar(2L,
                new UsuarioRequest("Vicente", "Vasquez", "vicente6@gmail.com", "CLIENTE", "nueva123"),
                "admin@gmail.com");

        assertEquals("hash-nuevo", usuario.getPassword());
        verify(refreshTokenRepository).deleteByUsuarioId(2L);
    }

    @Test
    void editarSinClaveNoLaCambiaNiCierraSesiones() {
        Usuario usuario = cliente();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("vicente6@gmail.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.actualizar(2L,
                new UsuarioRequest("Vicente", "Vasquez", "vicente6@gmail.com", "CLIENTE", ""),
                "admin@gmail.com");

        assertEquals("hash", usuario.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenRepository, never()).deleteByUsuarioId(anyLong());
    }
}
