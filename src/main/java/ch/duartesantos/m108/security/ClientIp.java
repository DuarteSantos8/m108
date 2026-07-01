package ch.duartesantos.m108.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ermittelt die Client-IP hinter dem vertrauenswürdigen Proxy-Pfad
 * (Cloudflare → cloudflared → Traefik). Cloudflare setzt {@code CF-Connecting-IP},
 * Traefik reicht {@code X-Forwarded-For} weiter.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) {
            return cf.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
