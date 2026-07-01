package ch.duartesantos.m108.web;

import ch.duartesantos.m108.security.CaptchaService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liefert das CAPTCHA-Bild. Bei jedem Abruf wird ein neuer Code erzeugt und in
 * der Session hinterlegt; das Bild darf nicht gecacht werden.
 */
@RestController
public class CaptchaController {

    private final CaptchaService captcha;

    public CaptchaController(CaptchaService captcha) {
        this.captcha = captcha;
    }

    @GetMapping("/captcha.png")
    public ResponseEntity<byte[]> captcha(HttpSession session) throws IOException {
        String code = captcha.newCode(session);
        byte[] png = captcha.render(code);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(png);
    }
}
