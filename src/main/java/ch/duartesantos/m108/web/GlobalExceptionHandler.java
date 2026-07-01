package ch.duartesantos.m108.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A10 — Mishandling of Exceptional Conditions. Globales Exception-Handling:
 * der Client erhält eine generische Meldung mit einer Korrelations-ID, während
 * der vollständige Stack-Trace nur serverseitig geloggt wird. Niemals werden
 * interne Details (Klassen, Pfade, SQL) nach aussen gegeben.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest req, Model model) {
        log.warn("Zugriff verweigert auf {} — {}", req.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("message", "Kein Zugriff. Dir fehlt die nötige Berechtigung.");
        model.addAttribute("ref", "—");
        return "error-page";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAll(Exception ex, HttpServletRequest req, Model model) {
        String ref = UUID.randomUUID().toString().substring(0, 8);
        // Voller Kontext NUR ins Server-Log (mit Korrelations-ID).
        log.error("Unerwarteter Fehler ref={} pfad={}", ref, req.getRequestURI(), ex);
        model.addAttribute("status", 500);
        model.addAttribute("message", "Es ist ein interner Fehler aufgetreten.");
        model.addAttribute("ref", ref);
        return "error-page";
    }
}
