package ch.duartesantos.m108.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Produkt im Katalog (A05 — Injection). Produkte mit {@code listed = false}
 * sind nicht öffentlich gelistet — eine erfolgreiche SQL-Injection im
 * verwundbaren Suchpfad macht sie trotzdem sichtbar.
 */
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private double price;
    private boolean listed;

    protected Product() {
    }

    public Product(String name, String category, double price, boolean listed) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.listed = listed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public boolean isListed() {
        return listed;
    }
}
