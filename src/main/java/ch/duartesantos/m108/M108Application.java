package ch.duartesantos.m108;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OWASP Demo-Lab — Modul 183 (Java Spring Security).
 *
 * Eine bewusst als Lern-Sandbox gebaute Applikation: jedes OWASP-Risiko wird
 * zuerst als Schwachstelle vorgeführt und direkt daneben mit der Gegenmassnahme
 * abgesichert. Alle Demo-Daten liegen in einer In-Memory-H2-Datenbank — es gibt
 * keine echten Personendaten und keinen Pfad zum Host.
 */
@SpringBootApplication
public class M108Application {
    public static void main(String[] args) {
        SpringApplication.run(M108Application.class, args);
    }
}
