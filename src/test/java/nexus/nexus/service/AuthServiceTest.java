package nexus.nexus.service;

import nexus.nexus.dto.*;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.RefreshToken;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;
import nexus.nexus.repository.RefreshTokenRepository;
import nexus.nexus.repository.UsuarioRepository;
import nexus.nexus.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService service;

    private Usuario admin;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTtlSeconds", 3600L);
        admin = Usuario.builder().id(1L).nombre("admin").apellidos("admin")
                .correo("admin@gmail.com").password("hash").rol(Rol.ADMIN).build();
    }

    // --- login ---

    @Test
    void loginCorrectoEntregaTokenRefreshTokenYUsuario() {
        when(usuarioRepository.findByCorreo("admin@gmail.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(admin)).thenReturn("jwt-falso");

        // El correo se normaliza: sin espacios y en minúscula
        AuthResponse resp = service.login(new LoginRequest("  Admin@Gmail.com ", "admin123"));

        assertEquals("jwt-falso", resp.token());
        assertFalse(resp.refreshToken().isBlank());
        assertEquals("ADMIN", resp.usuario().rol());
    }

    @Test
    void loginConClaveIncorrectaDa401() {
        when(usuarioRepository.findByCorreo("admin@gmail.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("admin@gmail.com", "mala")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(jwtService, never()).generarToken(any(Usuario.class));
    }

    @Test
    void loginConCorreoInexistenteDa401() {
        when(usuarioRepository.findByCorreo("nadie@gmail.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("nadie@gmail.com", "x")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    // --- registro ---

    @Test
    void registrarCreaUnClienteConLaClaveEncriptada() {
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto1")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("jwt-falso");

        service.registrar(new RegisterRequest("Ana", "Perez", "ANA@gmail.com", "secreto1"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals(Rol.CLIENTE, guardado.getValue().getRol());
        assertEquals("hash-nuevo", guardado.getValue().getPassword());
        assertEquals("ana@gmail.com", guardado.getValue().getCorreo());
    }

    @Test
    void registrarConCorreoExistenteDa409() {
        when(usuarioRepository.existsByCorreoIgnoreCase("admin@gmail.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.registrar(new RegisterRequest("Otro", "Admin", "admin@gmail.com", "secreto1")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // --- refresh con rotación ---

    @Test
    void refrescarRevocaElTokenUsadoYEntregaUnoNuevo() {
        RefreshToken usado = RefreshToken.builder().token("abc").usuario(admin)
                .expiraEn(Instant.now().plusSeconds(3600)).build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(usado));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(admin)).thenReturn("jwt-nuevo");

        TokenResponse resp = service.refrescar(new RefreshRequest("abc"));

        assertTrue(usado.getRevocado());
        assertEquals("jwt-nuevo", resp.token());
        assertNotEquals("abc", resp.refreshToken());
    }

    @Test
    void refrescarConTokenYaRevocadoDa401() {
        RefreshToken revocado = RefreshToken.builder().token("abc").usuario(admin)
                .expiraEn(Instant.now().plusSeconds(3600)).revocado(true).build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(revocado));

        ApiException ex = assertThrows(ApiException.class, () -> service.refrescar(new RefreshRequest("abc")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refrescarConTokenExpiradoDa401() {
        RefreshToken vencido = RefreshToken.builder().token("abc").usuario(admin)
                .expiraEn(Instant.now().minusSeconds(10)).build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(vencido));

        ApiException ex = assertThrows(ApiException.class, () -> service.refrescar(new RefreshRequest("abc")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void refrescarConTokenInexistenteDa401() {
        when(refreshTokenRepository.findByToken("zzz")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.refrescar(new RefreshRequest("zzz")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
