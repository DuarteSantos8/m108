# OWASP Demo-Lab — Modul 183 (Java Spring Security)

Eine Spring-Boot-Applikation, die ausgewählte **OWASP-Risiken zuerst als
Schwachstelle vorführt und direkt daneben mit der Gegenmassnahme absichert**.
Dazu kommen vollständige Authentifizierung/Autorisierung (Spring Security),
CAPTCHA + Brute-Force-Schutz, sicheres Logging und eine dokumentierte
Security-Architektur.

**Live:** <https://m-108.duarte-santos.ch>

> ⚠️ **Lern-Sandbox.** Alle Daten liegen in einer flüchtigen In-Memory-H2-Datenbank
> (reine Demo-Daten, keine echten Personendaten). Die bewusst „verwundbaren" Pfade
> wirken ausschliesslich auf diese Sandbox: H2 läuft ohne Stacked Queries, es gibt
> keinen Datei- oder Host-Zugriff. Beim Neustart ist alles zurückgesetzt.

---

## Testkonten

| Benutzer | Passwort     | Rolle              |
|----------|--------------|--------------------|
| `alice`  | `Alice!2345` | `ROLE_USER`        |
| `bob`    | `Bob!23456`  | `ROLE_USER`        |
| `admin`  | `Admin!2345` | `ROLE_ADMIN`, `ROLE_USER` |

Beim Login ist ein CAPTCHA Pflicht. Nach **5 Fehlversuchen** wird der Account
für **15 Minuten** gesperrt; zusätzlich greift ein **IP-Rate-Limit** (10 Versuche
/ 60 s).

---

## Umgesetzte OWASP-Risiken

| ID  | Risiko | Demo (Schwachstelle) | Gegenmassnahme |
|-----|--------|----------------------|----------------|
| A01 | Broken Access Control | IDOR: fremde Ausleihen über User-ID lesen | Ownership-Check + `@PreAuthorize`, Default-Deny |
| A05 | Injection | SQL-Injection via String-Verkettung (`%' OR '1'='1`) | Parameterbindung (Spring Data JPA `@Query`) |
| A04 | Cryptographic Failures | Klartext / MD5-Passwortspeicherung | BCrypt (`BCryptPasswordEncoder`) + HTTPS/HSTS |
| A02 | Security Misconfiguration | offene Actuator-Endpunkte, fehlende Header | Exposure-Limit, CSP/HSTS/X-Frame-Options |
| A07 | Authentication Failures | kein Lockout / Brute-Force | Account-Lockout, IP-Rate-Limit, CAPTCHA |
| A10 | Mishandling of Exceptions | Stack-Trace im HTTP-Response | `@ControllerAdvice`, generische Meldung + Ref-ID |
| A09 | Security Logging Failures | (Querschnitt) | dedizierter Audit-Logger, keine Secrets im Log |

Ausführliche Beschreibung je Risiko: [`docs/OWASP-Risiken.md`](docs/OWASP-Risiken.md).
Security-Architektur (Filter-Chain, Least Privilege, CORS/CSRF):
[`docs/Architektur.md`](docs/Architektur.md).

---

## Lokal starten

Voraussetzung: JDK 21.

```bash
mvn spring-boot:run
# oder
mvn -DskipTests package && java -jar target/m108-owasp.jar
```

App läuft auf <http://localhost:8080>. Tests: `mvn test` (9 Integrationstests
gegen die echte SecurityFilterChain inkl. CAPTCHA/Lockout/Rollen).

## Mit Docker

Das Image ist bewusst **runtime-only** — das Fat-JAR wird ausserhalb gebaut und
hineinkopiert (klein, schnell, wenig Disk):

```bash
mvn -DskipTests package
docker build -t m108-owasp .
docker run -p 8080:8080 m108-owasp
```

---

## Technologie

Spring Boot 3.3 · Spring Security 6 · Thymeleaf · Spring Data JPA · H2 (in-memory)
· SLF4J/Logback · Java 21.

## Deployment (homelab)

JAR wird auf dem `pve`-Host gebaut, ein schlankes `eclipse-temurin:21-jre`-Image
auf der Docker-VM betrieben. Public-Zugang über Cloudflare-Tunnel → Traefik
(Host-Routing) → Container. Die App ist nur über den HTTPS-Tunnel erreichbar;
Traefik reicht via `X-Forwarded-Proto=https` das echte Schema durch, damit HSTS
gesetzt und Redirects korrekt gebildet werden.
