# OWASP-Risiken — Schwachstelle, Demo, Gegenmassnahme

Pro Risiko nach dem geforderten Schema: *Was ist die Schwachstelle? Wie wurde sie
demonstriert? Welche Gegenmassnahme wurde implementiert?* Alle Demos laufen in der
In-Memory-Sandbox (siehe README).

---

## A01 — Broken Access Control (Bibliotheksverwaltung)

**Schwachstelle.** Zugriffsentscheidungen werden an eine ID aus dem Request
festgemacht statt am angemeldeten Benutzer (IDOR — Insecure Direct Object
Reference). Wer die ID manipuliert, sieht fremde Daten.

**Demo.** `/demo/a01` → „verwundbar": als `alice` angemeldet den Owner `bob`
eingeben. Der Endpunkt `GET /demo/a01?mode=vulnerable&owner=bob` liefert Bobs
Ausleihen aus, obwohl Alice angemeldet ist.

**Gegenmassnahme.**
- Der sichere Pfad (`mode=secure`) ignoriert den Owner-Parameter und liest
  ausschliesslich die Daten des authentifizierten Principals
  (`books.findByOwnerUsername(auth.getName())`).
- Privilegierte Sichten (`/demo/a01/admin-view`) sind per **Method-Level
  Security** geschützt: `@PreAuthorize("hasRole('ADMIN')")`. Ein `ROLE_USER`
  erhält 403.
- Default-Deny in der Filter-Chain (`anyRequest().authenticated()`).

Code: `web/DemoController.java`, `config/SecurityConfig.java`.

---

## A05 — Injection (Produktkatalog mit Suche)

**Schwachstelle.** Die Sucheingabe wird direkt in den SQL-String verkettet.
Dadurch lässt sich die WHERE-Bedingung umschreiben.

**Demo.** `/demo/a05` → „verwundbar" mit Payload `%' OR '1'='1`. Das ausgeführte
SQL wird angezeigt:

```sql
SELECT id, name, category, price, listed FROM product
WHERE listed = TRUE AND name LIKE '%%' OR '1'='1'
```

Durch das `OR '1'='1'` wird der `listed = TRUE`-Filter ausgehebelt; die beiden
intern markierten Produkte (`INTERN Prototyp X1`, `INTERN Mitarbeiter-Rabattcode`)
tauchen auf, obwohl sie nicht gelistet sind.

**Gegenmassnahme.** Der sichere Pfad nutzt **Parameterbindung** über Spring Data
JPA mit benanntem Parameter — Eingaben werden nie als SQL interpretiert:

```java
@Query("select p from Product p where p.listed = true "
     + "and lower(p.name) like lower(concat('%', :term, '%'))")
List<Product> searchListed(@Param("term") String term);
```

Zusätzlich: H2 ohne Stacked Queries, Demo auf reine SELECTs der `product`-Tabelle
begrenzt. Code: `demo/VulnerableProductDao.java` (Gegenbeispiel),
`demo/ProductRepository.java` (sicher).

---

## A04 — Cryptographic Failures (Passwortspeicherung)

**Schwachstelle.** Passwörter im Klartext oder mit schnellem, ungesalzenem
Hash (MD5). Bei einem DB-Leak sind die Passwörter sofort bzw. per Rainbow-Table
/ GPU trivial wiederherstellbar; gleiches Passwort ergibt gleichen Hash.

**Demo.** `/demo/a04` → Demo-Zeichenkette eingeben und Klartext, MD5 und BCrypt
nebeneinander vergleichen. BCrypt ist bei jedem Aufruf anders (Salt).

**Gegenmassnahme.** `BCryptPasswordEncoder` als `PasswordEncoder`-Bean; echte
Anmeldedaten werden **nur als BCrypt-Hash** gespeichert (siehe Admin-Seite, dort
sind nur Hashes sichtbar). HTTPS wird via Cloudflare erzwungen, HSTS-Header
gesetzt. Code: `config/SecurityConfig.java`, `user/DataSeeder.java`,
`demo/CryptoDemoService.java`.

---

## A02 — Security Misconfiguration (Actuator & Header)

**Schwachstelle.** Unkonfiguriert exponiert Spring Boot Actuator Endpunkte wie
`/actuator/env` (kann Secrets enthalten) oder `/actuator/shutdown` (DoS). Fehlende
Security-Header (CSP, HSTS, X-Frame-Options) öffnen XSS/Clickjacking-Fläche.

**Demo / Nachweis.** `/demo/a02` erklärt das Gegenbeispiel und verlinkt den Test:
`/actuator/health` → 200 (`UP`), `/actuator/env` und `/actuator/beans` →
gesperrt. Header prüfbar mit `curl -I https://m-108.duarte-santos.ch/`.

**Gegenmassnahme.**
- Actuator-Exposure auf `health` begrenzt, `shutdown` deaktiviert
  (`application.yml`).
- Security-Header zentral in `HeadersConfigurer`: Content-Security-Policy,
  Strict-Transport-Security, X-Frame-Options `DENY`, X-Content-Type-Options
  `nosniff`, Referrer-Policy, Permissions-Policy. H2-Konsole deaktiviert.

Code: `config/SecurityConfig.java`, `application.yml`.

---

## A07 — Authentication Failures (Brute-Force / Credential Stuffing)

**Schwachstelle.** Ohne Schutz lassen sich Passwörter automatisiert durchprobieren
(Brute-Force) bzw. geleakte Zugangsdaten breit testen (Credential Stuffing).

**Demo.** Direkt am Login erlebbar: nach 5 falschen Versuchen meldet die Seite
„Konto vorübergehend gesperrt"; sehr viele Versuche von einer IP lösen die
Rate-Limit-Meldung aus.

**Gegenmassnahme.** `LoginGuardFilter` läuft **vor** der Passwortprüfung und setzt
drei Schichten durch: IP-Rate-Limit, CAPTCHA, Account-Lockout (5 Versuche →
15 Min). Fehlversuche werden über `AuthEventListener` gezählt; erfolgreicher
Login setzt den Zähler zurück. Code: `security/LoginGuardFilter.java`,
`security/LoginAttemptService.java`, `security/CaptchaService.java`.

---

## A10 — Mishandling of Exceptional Conditions

**Schwachstelle.** Stack-Traces im HTTP-Response verraten interne Klassen, Pfade
und Versionen — wertvolle Aufklärung für Angreifer.

**Demo.** `/demo/a10` → beide Buttons teilen durch 0. „verwundbar" zeigt, wie ein
geleakter Stack-Trace im Response aussähe; „sicher" löst dieselbe Exception aus,
die generisch behandelt wird.

**Gegenmassnahme.** Globales `@ControllerAdvice` (`GlobalExceptionHandler`) gibt
dem Client nur eine generische Meldung + Korrelations-ID; der vollständige
Stack-Trace landet ausschliesslich im Server-Log. Zusätzlich
`server.error.include-stacktrace=never`. Code: `web/GlobalExceptionHandler.java`,
`application.yml`.

---

## A09 — Security Logging (Querschnitt)

**Schwachstelle.** Ohne Logging von Login-Versuchen fehlen Audit-Trails; werden
Passwörter/Tokens geloggt, entsteht ein neues Leck.

**Gegenmassnahme.** Dedizierter Audit-Logger `SECURITY_AUDIT` (eigener Logback-
Appender, JSON-ähnliche Zeilen für Logstash/Loki) protokolliert
`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `ACCOUNT_LOCKED`, `LOGIN_RATE_LIMIT`,
`LOGIN_CAPTCHA_FAIL` — **niemals** Passwort, CAPTCHA oder Token. Log-Level
DEBUG/INFO/WARN/ERROR werden sinnvoll eingesetzt. Code:
`security/AuthEventListener.java`, `logback-spring.xml`.
