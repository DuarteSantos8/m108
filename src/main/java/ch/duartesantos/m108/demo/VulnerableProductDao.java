package ch.duartesantos.m108.demo;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * BEWUSST VERWUNDBAR (A05 — Injection). Diese Klasse demonstriert die
 * Schwachstelle: die Sucheingabe wird direkt in den SQL-String verkettet.
 * Eingabe wie {@code ' OR '1'='1} hebelt den Filter aus und legt auch
 * ungelistete Produkte offen.
 *
 * <p>Sandbox-Absicherung: arbeitet ausschliesslich auf der In-Memory-H2-Tabelle
 * {@code product} mit reinen Demo-Daten. H2 ist ohne {@code allowMultiQueries}
 * konfiguriert, sodass keine gestapelten Statements (Stacked Queries) möglich
 * sind. Dieser Pfad ist NUR zu Lehrzwecken vorhanden — die produktive Suche
 * läuft über {@link ProductRepository#searchListed}.
 */
@Repository
public class VulnerableProductDao {

    private final JdbcTemplate jdbc;

    public VulnerableProductDao(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public List<Product> searchUnsafe(String term) {
        // ❌ String-Verkettung — klassische SQL-Injection
        return jdbc.query(renderSql(term), (rs, rowNum) -> new Product(
                rs.getString("name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getBoolean("listed")));
    }

    /** Gibt das tatsächlich ausgeführte SQL zurück, damit die Demo es anzeigen kann. */
    public String renderSql(String term) {
        return "SELECT id, name, category, price, listed FROM product "
                + "WHERE listed = TRUE AND name LIKE '%" + term + "%'";
    }

    /** Hilfsobjekt für die View, das auch die Anzahl Treffer kennt. */
    public List<String> searchUnsafeNames(String term) {
        List<String> names = new ArrayList<>();
        for (Product p : searchUnsafe(term)) {
            names.add(p.getName());
        }
        return names;
    }
}
