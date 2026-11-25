package nexus.nexus.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;

class JwtServiceTest {

    private static final String SECRETO = "una-clave-de-prueba-con-mas-de-32-caracteres-ok";

    private Usuario admin;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder().id(1L).nombre("admin").apellidos("admin")
                .correo("admin@gmail.com").rol(Rol.ADMIN).build();
    }

    @Test
    void generaUnTokenQueSePuedeLeerConSusDatos() {
        JwtService jwt = new JwtService(SECRETO, 900);

        Claims claims = jwt.leer(jwt.generarToken(admin));

        assertEquals("admin@gmail.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("rol", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void rechazaUnTokenConElPayloadAlterado() {
        JwtService jwt = new JwtService(SECRETO, 900);
        String[] partes = jwt.generarToken(admin).split("\\.");
        String payloadFalso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker@gmail.com\",\"rol\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String tokenAlterado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThrows(JwtException.class, () -> jwt.leer(tokenAlterado));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        JwtService otro = new JwtService("otra-clave-distinta-de-mas-de-32-caracteres-xyz", 900);
        JwtService jwt = new JwtService(SECRETO, 900);

        assertThrows(JwtException.class, () -> jwt.leer(otro.generarToken(admin)));
    }

    @Test
    void rechazaUnTokenExpirado() {
        JwtService expirado = new JwtService(SECRETO, -60);

        assertThrows(JwtException.class, () -> expirado.leer(expirado.generarToken(admin)));
    }

    @Test
    void noPermiteUnSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 900));
    }
}
