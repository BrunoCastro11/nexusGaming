package nexus.nexus.dto;

public record AuthResponse(String token, String refreshToken, UsuarioResponse usuario) {
}
