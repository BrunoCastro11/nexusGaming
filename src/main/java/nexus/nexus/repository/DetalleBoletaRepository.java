package nexus.nexus.repository;

import nexus.nexus.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    /** Sirve para impedir borrar un producto que ya fue vendido. */
    boolean existsByProductoId(Long productoId);
}
