package ch.duartesantos.m108.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * A09 — Security Logging. Schreibt sicherheitsrelevante Authentifizierungs-
 * Ereignisse in den dedizierten Audit-Logger {@code SECURITY_AUDIT} und
 * aktualisiert den Fehlversuchs-Zähler.
 *
 * <p>Wichtig (A09): Es wird NIE das Passwort, das CAPTCHA oder ein Token
 * geloggt — nur Benutzername, Ergebnis und Fehlertyp.
 */
@Component
public class AuthEventListener {

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final LoginAttemptService attempts;

    public AuthEventListener(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        attempts.loginSucceeded(username);
        audit.info("LOGIN_SUCCESS user={}", username);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = String.valueOf(event.getAuthentication().getPrincipal());
        attempts.loginFailed(username);
        int left = attempts.remainingAttempts(username);
        audit.warn("LOGIN_FAILURE user={} reason={} remainingAttempts={}",
                username, event.getException().getClass().getSimpleName(), left);
        if (attempts.isLocked(username)) {
            audit.warn("ACCOUNT_LOCKED user={} lockSeconds={}",
                    username, attempts.remainingLockSeconds(username));
        }
    }
}
