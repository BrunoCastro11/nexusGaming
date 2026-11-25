package nexus.nexus.repository;

import nexus.nexus.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Cierra todas las sesiones de un usuario (al cambiar su clave o rol, o al borrarlo). */
    void deleteByUsuarioId(Long usuarioId);
}
