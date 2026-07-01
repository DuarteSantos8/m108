package ch.duartesantos.m108.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Anwendungsbenutzer. Das Passwort wird ausschliesslich als BCrypt-Hash
 * gespeichert (A04 — Cryptographic Failures). Die Rollen werden als kommaseparierte
 * Liste von Authorities gehalten (z.B. "ROLE_USER" oder "ROLE_ADMIN").
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Immer ein BCrypt-Hash, niemals Klartext. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String roles;

    @Column(nullable = false)
    private String displayName;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String roles, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public String getDisplayName() {
        return displayName;
    }
}
