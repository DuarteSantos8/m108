package ch.duartesantos.m108.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * A07 — Authentication Failures. Schutz gegen Brute-Force und Credential
 * Stuffing über zwei Mechanismen:
 *
 * <ul>
 *   <li>Account-Lockout: nach {@value #MAX_ATTEMPTS} Fehlversuchen wird der
 *       Benutzername für {@value #LOCK_MINUTES} Minuten gesperrt.</li>
 *   <li>IP-Rate-Limit: pro IP sind maximal {@value #IP_MAX_PER_WINDOW}
 *       Login-Versuche je {@value #IP_WINDOW_SECONDS} s erlaubt — bremst
 *       verteilte Versuche über viele Benutzernamen (Credential Stuffing).</li>
 * </ul>
 *
 * In-Memory-Zähler genügen für die Demo; produktiv würde man Redis o.ä. nutzen,
 * damit der Schutz über mehrere Instanzen hinweg greift.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final int LOCK_MINUTES = 15;
    static final int IP_MAX_PER_WINDOW = 10;
    static final int IP_WINDOW_SECONDS = 60;

    private record Attempts(int count, Instant lockedUntil) {
    }

    private record Window(int count, Instant resetAt) {
    }

    private final ConcurrentHashMap<String, Attempts> byUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> byIp = new ConcurrentHashMap<>();

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    public void loginFailed(String username) {
        byUser.compute(key(username), (k, v) -> {
            int count = (v == null ? 0 : v.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS
                    ? Instant.now().plus(Duration.ofMinutes(LOCK_MINUTES))
                    : null;
            return new Attempts(count, lockedUntil);
        });
    }

    public void loginSucceeded(String username) {
        byUser.remove(key(username));
    }

    public boolean isLocked(String username) {
        Attempts a = byUser.get(key(username));
        if (a == null || a.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(a.lockedUntil())) {
            byUser.remove(key(username));
            return false;
        }
        return true;
    }

    public long remainingLockSeconds(String username) {
        Attempts a = byUser.get(key(username));
        if (a == null || a.lockedUntil() == null) {
            return 0;
        }
        long secs = Duration.between(Instant.now(), a.lockedUntil()).getSeconds();
        return Math.max(secs, 0);
    }

    public int remainingAttempts(String username) {
        Attempts a = byUser.get(key(username));
        int used = a == null ? 0 : a.count();
        return Math.max(MAX_ATTEMPTS - used, 0);
    }

    /** Registriert einen Login-Versuch pro IP und meldet, ob das Limit überschritten ist. */
    public boolean ipRateLimitExceeded(String ip) {
        Window w = byIp.compute(ip == null ? "" : ip, (k, v) -> {
            Instant now = Instant.now();
            if (v == null || now.isAfter(v.resetAt())) {
                return new Window(1, now.plus(Duration.ofSeconds(IP_WINDOW_SECONDS)));
            }
            return new Window(v.count() + 1, v.resetAt());
        });
        return w.count() > IP_MAX_PER_WINDOW;
    }
}
