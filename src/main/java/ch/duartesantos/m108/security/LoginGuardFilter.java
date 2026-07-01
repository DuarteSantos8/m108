package ch.duartesantos.m108.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Läuft vor dem {@code UsernamePasswordAuthenticationFilter} und setzt drei
 * Schutzschichten durch, BEVOR überhaupt ein Passwort geprüft wird:
 *
 * <ol>
 *   <li>IP-Rate-Limit gegen Credential Stuffing,</li>
 *   <li>CAPTCHA gegen automatisierte Angriffe,</li>
 *   <li>Account-Lockout gegen Brute-Force.</li>
 * </ol>
 *
 * Schlägt eine Prüfung fehl, wird zurück auf {@code /login} mit Fehlergrund
 * geleitet und die eigentliche Authentifizierung gar nicht erst ausgeführt.
 */
public class LoginGuardFilter extends OncePerRequestFilter {

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final RequestMatcher LOGIN_POST =
            new AntPathRequestMatcher("/login", "POST");

    private final LoginAttemptService attempts;
    private final CaptchaService captcha;

    public LoginGuardFilter(LoginAttemptService attempts, CaptchaService captcha) {
        this.attempts = attempts;
        this.captcha = captcha;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!LOGIN_POST.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClientIp.of(request);
        String username = request.getParameter("username");

        if (attempts.ipRateLimitExceeded(ip)) {
            audit.warn("LOGIN_RATE_LIMIT ip={} user={}", ip, username);
            redirect(request, response, "ratelimit");
            return;
        }

        if (attempts.isLocked(username)) {
            audit.warn("LOGIN_BLOCKED_LOCKED user={} ip={}", username, ip);
            redirect(request, response, "locked");
            return;
        }

        if (!captcha.validate(request.getSession(), request.getParameter("captcha"))) {
            audit.warn("LOGIN_CAPTCHA_FAIL user={} ip={}", username, ip);
            redirect(request, response, "captcha");
            return;
        }

        chain.doFilter(request, response);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login?error=" + reason);
    }
}
