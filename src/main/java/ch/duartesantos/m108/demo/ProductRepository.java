package ch.duartesantos.m108.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Sichere Suche (A05-Gegenmassnahme): Parameterbindung statt
     * String-Verkettung. Nur gelistete Produkte werden zurückgegeben.
     */
    @Query("select p from Product p where p.listed = true and lower(p.name) like lower(concat('%', :term, '%'))")
    List<Product> searchListed(@Param("term") String term);
}
