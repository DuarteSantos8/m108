package ch.duartesantos.m108.web;

import ch.duartesantos.m108.demo.Book;
import ch.duartesantos.m108.demo.BookRepository;
import ch.duartesantos.m108.demo.CryptoDemoService;
import ch.duartesantos.m108.demo.Product;
import ch.duartesantos.m108.demo.ProductRepository;
import ch.duartesantos.m108.demo.VulnerableProductDao;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Die OWASP-Demonstrationen. Jede Demo zeigt zuerst die Schwachstelle und
 * direkt daneben die Gegenmassnahme. Alles arbeitet auf den Sandbox-Demo-Daten.
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final BookRepository books;
    private final ProductRepository products;
    private final VulnerableProductDao vulnerableDao;
    private final CryptoDemoService crypto;

    public DemoController(BookRepository books, ProductRepository products,
                          VulnerableProductDao vulnerableDao, CryptoDemoService crypto) {
        this.books = books;
        this.products = products;
        this.vulnerableDao = vulnerableDao;
        this.crypto = crypto;
    }

    // ── A01 — Broken Access Control ─────────────────────────────────────────
    @GetMapping("/a01")
    public String a01(Authentication auth, Model model,
                      @RequestParam(required = false) String owner,
                      @RequestParam(required = false) String mode) {
        String me = auth.getName();
        model.addAttribute("me", me);
        model.addAttribute("requestedOwner", owner);
        model.addAttribute("mode", mode);

        if ("vulnerable".equals(mode) && owner != null) {
            // ❌ IDOR: gibt die Ausleihen eines BELIEBIGEN Benutzers zurück.
            log.warn("A01 verwundbarer Zugriff: {} liest Daten von {}", me, owner);
            model.addAttribute("result", books.findByOwnerUsername(owner));
        } else if ("secure".equals(mode)) {
            // ✅ Nur die eigenen Daten — Owner-Parameter wird ignoriert.
            model.addAttribute("result", books.findByOwnerUsername(me));
        }
        return "demo/a01";
    }

    /**
     * Legitimer Admin-Zugriff auf fremde Daten — abgesichert per Method-Level
     * Security. Ein Benutzer ohne ADMIN-Rolle erhält hier 403 (Least Privilege).
     */
    @GetMapping("/a01/admin-view")
    @PreAuthorize("hasRole('ADMIN')")
    public String a01AdminView(Authentication auth, Model model,
                               @RequestParam String owner) {
        model.addAttribute("me", auth.getName());
        model.addAttribute("mode", "admin");
        model.addAttribute("requestedOwner", owner);
        model.addAttribute("result", books.findByOwnerUsername(owner));
        return "demo/a01";
    }

    // ── A05 — Injection ─────────────────────────────────────────────────────
    @GetMapping("/a05")
    public String a05(Model model,
                      @RequestParam(required = false) String q,
                      @RequestParam(required = false) String mode) {
        model.addAttribute("q", q);
        model.addAttribute("mode", mode);

        if (q != null && "vulnerable".equals(mode)) {
            // ❌ SQL-Injection durch String-Verkettung.
            model.addAttribute("sql", vulnerableDao.renderSql(q));
            model.addAttribute("result", vulnerableDao.searchUnsafe(q));
        } else if (q != null && "secure".equals(mode)) {
            // ✅ Parameterbindung (Spring Data JPA @Query).
            model.addAttribute("sql",
                "... where p.listed = true and lower(p.name) like lower(concat('%', :term, '%'))  [:term = bind]");
            model.addAttribute("result", products.searchListed(q));
        }
        return "demo/a05";
    }

    // ── A04 — Cryptographic Failures ────────────────────────────────────────
    @GetMapping("/a04")
    public String a04(Model model, @RequestParam(required = false) String pw) {
        if (pw != null && !pw.isBlank()) {
            model.addAttribute("input", pw);
            model.addAttribute("plaintext", crypto.plaintext(pw));
            model.addAttribute("md5", crypto.md5(pw));
            model.addAttribute("bcrypt", crypto.bcrypt(pw));
        }
        return "demo/a04";
    }

    // ── A02 — Security Misconfiguration ─────────────────────────────────────
    @GetMapping("/a02")
    public String a02() {
        return "demo/a02";
    }

    // ── A10 — Mishandling of Exceptional Conditions ─────────────────────────
    @GetMapping("/a10")
    public String a10() {
        return "demo/a10";
    }

    /** ❌ Verwundbar: leakt die interne Fehlermeldung an den Client. */
    @GetMapping("/a10/leak")
    public String a10Leak(@RequestParam(defaultValue = "0") int divisor, Model model) {
        try {
            int result = 100 / divisor;
            model.addAttribute("leak", "Ergebnis: " + result);
        } catch (Exception e) {
            // Anti-Pattern: interne Details nach aussen geben.
            model.addAttribute("leak",
                "java.lang.ArithmeticException: " + e.getMessage()
                + "\n\tat ch.duartesantos.m108.web.DemoController.a10Leak(DemoController.java)"
                + "\n\tat jdk.internal.reflect.DirectMethodHandleAccessor.invoke(...)"
                + "\n\t[... vollständiger Stack-Trace mit Klassen, Versionen, Pfaden ...]");
        }
        return "demo/a10";
    }

    /** ✅ Sicher: löst dieselbe Exception aus, der GlobalExceptionHandler antwortet generisch. */
    @GetMapping("/a10/safe")
    public String a10Safe(@RequestParam(defaultValue = "0") int divisor) {
        int result = 100 / divisor; // wirft ArithmeticException -> @ControllerAdvice
        return "redirect:/demo/a10?result=" + result;
    }
}
