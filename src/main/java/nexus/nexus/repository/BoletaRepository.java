package nexus.nexus.repository;

import nexus.nexus.model.Boleta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    /** Pedidos del usuario autenticado (GET /pedidos/mios). */
    @EntityGraph(attributePaths = "detalles")
    List<Boleta> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    @EntityGraph(attributePaths = "detalles")
    List<Boleta> findAllByOrderByFechaDesc();

    /** Sirve para impedir borrar un usuario que tiene pedidos. */
    boolean existsByUsuarioId(Long usuarioId);
}
