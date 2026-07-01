package ch.duartesantos.m108;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.duartesantos.m108.security.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-End-Test der echten SecurityFilterChain: CAPTCHA, Lockout, Rollen,
 * und die OWASP-Demos. Das CAPTCHA wird über die Session ausgelesen, damit der
 * Login-Pfad inklusive Guard-Filter durchlaufen wird.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTest {

    @Autowired
    private MockMvc mvc;

    /** Holt ein CAPTCHA-Bild und gibt die in der Session hinterlegte Lösung zurück. */
    private String solveCaptcha(MockHttpSession session) throws Exception {
        mvc.perform(get("/captcha.png").session(session)).andExpect(status().isOk());
        HttpSession s = session;
        return (String) s.getAttribute(CaptchaService.SESSION_KEY);
    }

    private MockHttpSession loginAs(String user, String pass) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String captcha = solveCaptcha(session);
        mvc.perform(post("/login").session(session).with(csrf())
                        .param("username", user)
                        .param("password", pass)
                        .param("captcha", captcha))
                .andExpect(redirectedUrl("/dashboard"));
        return session;
    }

    @Test
    void publicHomeIsReachable() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OWASP")));
    }

    @Test
    void protectedPageRedirectsToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void documentationPagesArePublic() throws Exception {
        mvc.perform(get("/architektur")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Filter-Chain")));
        mvc.perform(get("/logging")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SECURITY_AUDIT")));
        mvc.perform(get("/doku")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Broken Access Control")));
    }

    @Test
    void loginFailsWithWrongCaptcha() throws Exception {
        MockHttpSession session = new MockHttpSession();
        solveCaptcha(session);
        mvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "alice")
                        .param("password", "Alice!2345")
                        .param("captcha", "WRONG"))
                .andExpect(redirectedUrl("/login?error=captcha"));
    }

    @Test
    void loginSucceedsWithValidCaptcha() throws Exception {
        loginAs("alice", "Alice!2345");
    }

    @Test
    void a01VulnerableLeaksOtherUsersData() throws Exception {
        MockHttpSession session = loginAs("alice", "Alice!2345");
        mvc.perform(get("/demo/a01").session(session)
                        .param("mode", "vulnerable").param("owner", "bob"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Design Patterns")));
    }

    @Test
    void a01AdminViewForbiddenForUser() throws Exception {
        MockHttpSession session = loginAs("alice", "Alice!2345");
        mvc.perform(get("/demo/a01/admin-view").session(session).param("owner", "bob"))
                .andExpect(status().isForbidden());
    }

    @Test
    void a05InjectionRevealsHiddenProducts() throws Exception {
        MockHttpSession session = loginAs("alice", "Alice!2345");
        mvc.perform(get("/demo/a05").session(session)
                        .param("mode", "vulnerable").param("q", "%' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INTERN")));
    }

    @Test
    void a05SecureSearchHidesInternalProducts() throws Exception {
        MockHttpSession session = loginAs("alice", "Alice!2345");
        mvc.perform(get("/demo/a05").session(session)
                        .param("mode", "secure").param("q", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("INTERN Prototyp"))));
    }

    @Test
    void adminPageForbiddenForUserAllowedForAdmin() throws Exception {
        MockHttpSession userSession = loginAs("alice", "Alice!2345");
        mvc.perform(get("/admin").session(userSession)).andExpect(status().isForbidden());

        MockHttpSession adminSession = loginAs("admin", "Admin!2345");
        mvc.perform(get("/admin").session(adminSession)).andExpect(status().isOk());
    }
}
