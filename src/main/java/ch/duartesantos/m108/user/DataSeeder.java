package ch.duartesantos.m108.user;

import ch.duartesantos.m108.demo.Book;
import ch.duartesantos.m108.demo.BookRepository;
import ch.duartesantos.m108.demo.Product;
import ch.duartesantos.m108.demo.ProductRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Befüllt die In-Memory-H2-Datenbank beim Start mit reinen Demo-Daten.
 * Es gibt bewusst keine echten Personendaten. Passwörter werden nur als
 * BCrypt-Hash abgelegt.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final BookRepository books;
    private final ProductRepository products;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, BookRepository books,
                      ProductRepository products, PasswordEncoder encoder) {
        this.users = users;
        this.books = books;
        this.products = products;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        users.saveAll(List.of(
            new AppUser("admin", encoder.encode("Admin!2345"), "ROLE_ADMIN,ROLE_USER", "Administrator"),
            new AppUser("alice", encoder.encode("Alice!2345"), "ROLE_USER", "Alice Beispiel"),
            new AppUser("bob",   encoder.encode("Bob!23456"),  "ROLE_USER", "Bob Muster")
        ));

        books.saveAll(List.of(
            new Book("alice", "Clean Code", "Kapitel 7 offen"),
            new Book("alice", "The Pragmatic Programmer", "bis Ende Monat"),
            new Book("bob",   "Design Patterns (GoF)", "für Projektarbeit"),
            new Book("bob",   "Refactoring", "Notiz: privat")
        ));

        products.saveAll(List.of(
            new Product("USB-C Kabel",        "Zubehör",   9.90,  true),
            new Product("Mechanische Tastatur","Eingabe",  89.00, true),
            new Product("27\" Monitor",        "Display",  279.00, true),
            new Product("Webcam HD",           "Video",    49.00, true),
            // Nicht gelistet — sollte nur via Injection auftauchen:
            new Product("INTERN Prototyp X1",  "Geheim",   0.00,  false),
            new Product("INTERN Mitarbeiter-Rabattcode", "Geheim", 0.00, false)
        ));

        log.info("Demo-Daten geladen: {} Benutzer, {} Bücher, {} Produkte",
                users.count(), books.count(), products.count());
    }
}
