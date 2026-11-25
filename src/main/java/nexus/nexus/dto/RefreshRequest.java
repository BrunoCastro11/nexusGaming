package nexus.nexus.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token inválido o expirado") String refreshToken) {
}
