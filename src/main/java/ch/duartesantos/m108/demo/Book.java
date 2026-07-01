package ch.duartesantos.m108.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Ausleihe in der Bibliotheksverwaltung (A01 — Broken Access Control).
 * Jedes Buch gehört einem Benutzer ({@code ownerUsername}); ein Benutzer darf
 * nur seine eigenen Ausleihen sehen.
 */
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerUsername;
    private String title;
    private String notes;

    protected Book() {
    }

    public Book(String ownerUsername, String title, String notes) {
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }
}
