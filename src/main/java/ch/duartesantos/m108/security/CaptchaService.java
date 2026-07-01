package ch.duartesantos.m108.security;

import jakarta.servlet.http.HttpSession;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/**
 * Selbstgebautes Bild-CAPTCHA (A06/Aufgabe 2.2 — Schutz vor automatisierten
 * Angriffen). Erzeugt einen verzerrten 5-stelligen Code als PNG und legt die
 * Lösung in der HTTP-Session ab. Beim Login wird die Eingabe gegen die Session
 * geprüft. Kein externer Dienst, keine Drittbibliothek.
 */
@Service
public class CaptchaService {

    public static final String SESSION_KEY = "CAPTCHA_ANSWER";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 5;
    private static final int WIDTH = 180;
    private static final int HEIGHT = 60;

    private final SecureRandom random = new SecureRandom();

    public String newCode(HttpSession session) {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        session.setAttribute(SESSION_KEY, code.toString());
        return code.toString();
    }

    public boolean validate(HttpSession session, String input) {
        Object expected = session.getAttribute(SESSION_KEY);
        // Einmal-Verwendung: Lösung nach der Prüfung verwerfen.
        session.removeAttribute(SESSION_KEY);
        return expected != null && input != null
                && expected.toString().equalsIgnoreCase(input.trim());
    }

    public byte[] render(String code) throws IOException {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Störlinien
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(random.nextInt(200) + 30, random.nextInt(200) + 30, random.nextInt(200) + 30));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // Verzerrte Zeichen
        int x = 18;
        for (char c : code.toCharArray()) {
            AffineTransform original = g.getTransform();
            double angle = (random.nextDouble() - 0.5) * 0.7;
            g.rotate(angle, x, HEIGHT / 2.0);
            g.setColor(new Color(random.nextInt(120), random.nextInt(120), random.nextInt(120)));
            g.setFont(new Font("SansSerif", Font.BOLD, 34 + random.nextInt(8)));
            g.drawString(String.valueOf(c), x, 42 + random.nextInt(8) - 4);
            g.setTransform(original);
            x += 30;
        }

        // Störpunkte
        for (int i = 0; i < 120; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
