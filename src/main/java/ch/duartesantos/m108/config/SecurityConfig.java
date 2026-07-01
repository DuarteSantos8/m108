package ch.duartesantos.m108.config;

import ch.duartesantos.m108.security.CaptchaService;
import ch.duartesantos.m108.security.LoginAttemptService;
import ch.duartesantos.m108.security.LoginGuardFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Zentrale Spring-Security-Konfiguration (Aufgabe „Architektur").
 *
 * <p><b>Filter-Chain:</b> Die Anfrage durchläuft die Spring-Security-FilterChain
 * (SecurityContext → CSRF → unser {@link LoginGuardFilter} → Username/Password-
 * Authentifizierung → Authorization). Unser Guard-Filter wird bewusst VOR den
 * {@code UsernamePasswordAuthenticationFilter} gehängt, damit Rate-Limit,
 * CAPTCHA und Lockout greifen, bevor ein Passwort verarbeitet wird.
 *
 * <p><b>Least Privilege:</b> Standardmässig ist alles {@code authenticated};
 * öffentlich sind nur Login, CAPTCHA-Bild, statische Assets und der Health-Check.
 * {@code /admin/**} verlangt {@code ROLE_ADMIN}. Methoden-Sicherheit
 * ({@code @PreAuthorize}) erlaubt zusätzlich feingranulare Regeln.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginAttemptService attempts;
    private final CaptchaService captcha;

    public SecurityConfig(LoginAttemptService attempts, CaptchaService captcha) {
        this.attempts = attempts;
        this.captcha = captcha;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // A04 — Cryptographic Failures: BCrypt mit Salt, kein MD5/Klartext.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/captcha.png", "/css/**", "/js/**",
                        "/architektur", "/logging", "/doku",
                        "/favicon.ico", "/error", "/actuator/health").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=bad")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            // CSRF ist aktiv (Default). Alle Formulare senden den _csrf-Token mit.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/health"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sf -> sf.changeSessionId()))
            .headers(this::securityHeaders)
            // Guard (Rate-Limit + CAPTCHA + Lockout) vor die Passwortprüfung hängen.
            .addFilterBefore(new LoginGuardFilter(attempts, captcha),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void securityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; img-src 'self' data:; style-src 'self'; "
                + "script-src 'self'; object-src 'none'; base-uri 'self'; "
                + "form-action 'self'; frame-ancestors 'none'"))
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
            .referrerPolicy(rp -> rp.policy(
                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                    .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .permissionsPolicy(pp -> pp.policy(
                "geolocation=(), microphone=(), camera=()"));
    }

    /**
     * Restriktive CORS-Politik: nur die eigene Origin darf Cross-Origin-Requests
     * stellen. Keine Wildcard-Origin in Kombination mit Credentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://m-108.duarte-santos.ch"));
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
