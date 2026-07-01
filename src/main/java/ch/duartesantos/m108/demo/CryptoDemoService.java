package ch.duartesantos.m108.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * A04 — Cryptographic Failures. Zeigt am selben Eingabe-Passwort den Unterschied
 * zwischen unsicherer (Klartext / MD5) und sicherer (BCrypt) Speicherung.
 * Arbeitet nur auf der vom Benutzer eingegebenen Demo-Zeichenkette, nie auf
 * echten Anmeldedaten.
 */
@Service
public class CryptoDemoService {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String plaintext(String input) {
        return input;
    }

    public String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String bcrypt(String input) {
        return bcrypt.encode(input);
    }
}
