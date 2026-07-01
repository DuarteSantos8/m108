package ch.duartesantos.m108.web;

import ch.duartesantos.m108.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin-Bereich — nur mit ROLE_ADMIN erreichbar (URL-Regel in der
 * SecurityFilterChain, zusätzlich Method-Level-Schutz). Demonstriert das
 * Prinzip des minimalen Privilegs.
 */
@Controller
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/admin")
    public String admin(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("users", users.findAll());
        return "admin";
    }
}
