package nexus.nexus.repository;

import nexus.nexus.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Trae la categoría junto con el producto para no hacer una consulta por fila. */
    @Override
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();

    /** Sirve para impedir borrar una categoría que tiene productos. */
    boolean existsByCategoriaId(Long categoriaId);

    /**
     * Descuenta stock de forma atómica: solo actualiza si alcanza el stock.
     * Devuelve 0 si no alcanzó, así dos pagos simultáneos no dejan el stock en negativo.
     */
    @Modifying
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
