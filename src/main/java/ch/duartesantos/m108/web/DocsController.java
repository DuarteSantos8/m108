package ch.duartesantos.m108.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Öffentliche Projekt-Dokumentation (Bewertungskriterien Modul 183):
 * Security-Architektur, sicheres Logging und die OWASP-Risiken-Doku.
 * Bewusst ohne Login erreichbar, damit die Bewertung ohne Konto möglich ist.
 */
@Controller
public class DocsController {

    @GetMapping("/architektur")
    public String architektur() {
        return "docs/architektur";
    }

    @GetMapping("/logging")
    public String logging() {
        return "docs/logging";
    }

    @GetMapping("/doku")
    public String doku() {
        return "docs/doku";
    }
}
