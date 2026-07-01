package ch.duartesantos.m108.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByOwnerUsername(String ownerUsername);
}
