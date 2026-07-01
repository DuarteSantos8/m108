# Security-Architektur

## Überblick

```
Browser ──HTTPS──> Cloudflare ──Tunnel──> cloudflared ──> Traefik ──HTTP──> Spring-Boot-App (8080)
   (TLS am Edge)     (WAF/CDN)              (X-Forwarded-Proto=https gesetzt)      │
                                                                                  ├─ SecurityFilterChain
                                                                                  ├─ Thymeleaf-Views
                                                                                  └─ H2 (in-memory, Demo-Daten)
```

TLS endet bei Cloudflare. Da die App nur über den HTTPS-Tunnel erreichbar ist,
setzt Traefik `X-Forwarded-Proto: https`; Spring (`forward-headers-strategy:
framework`) erkennt dadurch HTTPS und setzt HSTS / bildet korrekte Redirects.

## Spring Security Filter-Chain

Jede Anfrage durchläuft die Filter-Chain (vereinfacht, in Reihenfolge):

1. `SecurityContextHolderFilter` — lädt den Security-Kontext aus der Session.
2. `CsrfFilter` — prüft den `_csrf`-Token bei state-changing Requests (POST).
3. **`LoginGuardFilter`** *(eigener Filter)* — nur bei `POST /login`: IP-Rate-Limit
   → CAPTCHA → Account-Lockout. Schlägt etwas fehl, wird auf `/login?error=…`
   umgeleitet, **bevor** ein Passwort verarbeitet wird.
4. `UsernamePasswordAuthenticationFilter` — eigentliche Authentifizierung gegen
   `AppUserDetailsService` (+ `BCryptPasswordEncoder`).
5. `ExceptionTranslationFilter` — wandelt `AccessDeniedException`/
   `AuthenticationException` in 403/Redirect zum Login.
6. `AuthorizationFilter` — setzt die URL-Autorisierungsregeln durch.

Eingehängt über `http.addFilterBefore(loginGuardFilter,
UsernamePasswordAuthenticationFilter.class)` in `config/SecurityConfig.java`.

## Least Privilege (minimales Privileg)

- **Default-Deny:** `anyRequest().authenticated()` — alles ist gesperrt, ausser
  explizit freigegeben (`/`, `/login`, `/captcha.png`, statische Assets,
  `/actuator/health`).
- `/admin/**` verlangt `ROLE_ADMIN`.
- **Method-Level Security** (`@EnableMethodSecurity` + `@PreAuthorize`) für
  feingranulare Regeln zusätzlich zur URL-Ebene.
- Demo-Konten haben nur die nötigen Rollen; der Docker-Prozess läuft als
  non-root User (`app`).

## CSRF

Standardmässig **aktiv** (Synchronizer-Token). Alle Formulare senden den
`_csrf`-Token über Thymeleaf automatisch mit. Nachweis: `POST /login` ohne Token
→ HTTP 403. Nur `/actuator/health` ist von CSRF ausgenommen (idempotenter
GET-Healthcheck).

## CORS

Restriktiv konfiguriert (`CorsConfigurationSource`): erlaubte Origin nur
`https://m-108.duarte-santos.ch`, Methoden `GET`/`POST`, keine Wildcard-Origin in
Kombination mit `allowCredentials`.

## Security-Header

Zentral im `HeadersConfigurer` gesetzt:

| Header | Wert |
|--------|------|
| Content-Security-Policy | `default-src 'self'; img-src 'self' data:; script-src 'self'; object-src 'none'; frame-ancestors 'none'` |
| Strict-Transport-Security | `max-age=31536000; includeSubDomains` |
| X-Frame-Options | `DENY` |
| X-Content-Type-Options | `nosniff` |
| Referrer-Policy | `strict-origin-when-cross-origin` |
| Permissions-Policy | `geolocation=(), microphone=(), camera=()` |

## Inbetriebnahme

- Fat-JAR auf dem `pve`-Host gebaut, runtime-only `eclipse-temurin:21-jre`-Image
  auf der Docker-VM (non-root, Healthcheck auf `/actuator/health`,
  `mem_limit 384m`, JVM `-Xmx256m`).
- Keine Secrets im Image/Repo; In-Memory-DB ohne Persistenz.
- Public ausschliesslich über den Cloudflare-Tunnel (keine offenen Ports am Host).
