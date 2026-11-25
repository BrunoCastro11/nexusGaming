package nexus.nexus.service;

import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.*;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.RefreshToken;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;
import nexus.nexus.repository.RefreshTokenRepository;
import nexus.nexus.repository.UsuarioRepository;
import nexus.nexus.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-ttl-seconds}")
    private long refreshTtlSeconds;

    @Transactional
    public AuthResponse registrar(RegisterRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(req.nombre().trim())
                .apellidos(req.apellidos().trim())
                .correo(correo)
                .password(passwordEncoder.encode(req.password()))
                .rol(Rol.CLIENTE)
                .build());
        return emitirSesion(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(req.correo().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return emitirSesion(usuario);
    }

    /** Rotación: el refresh token usado se revoca y se entrega uno nuevo. */
    @Transactional
    public TokenResponse refrescar(RefreshRequest req) {
        RefreshToken actual = refreshTokenRepository.findByToken(req.refreshToken())
                .filter(t -> !t.getRevocado() && t.getExpiraEn().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado"));
        actual.setRevocado(true);
        Usuario usuario = actual.getUsuario();
        String nuevoRefresh = crearRefreshToken(usuario);
        return new TokenResponse(jwtService.generarToken(usuario), nuevoRefresh);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No autenticado o sesión expirada"));
    }

    private AuthResponse emitirSesion(Usuario usuario) {
        String refresh = crearRefreshToken(usuario);
        return new AuthResponse(jwtService.generarToken(usuario), refresh, UsuarioResponse.de(usuario));
    }

    private String crearRefreshToken(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .usuario(usuario)
                .expiraEn(Instant.now().plusSeconds(refreshTtlSeconds))
                .build());
        return token;
    }
}
