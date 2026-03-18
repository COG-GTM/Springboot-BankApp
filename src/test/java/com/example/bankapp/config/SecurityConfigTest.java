package com.example.bankapp.config;

import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link SecurityConfig}.
 *
 * Uses @WebMvcTest to load only the web layer (no DB, no full context).
 * AccountService is mocked to isolate security configuration behavior.
 */
@WebMvcTest(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private AccountService accountService;

    // ──────────────────────────────────────────────
    // PasswordEncoder bean
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("passwordEncoder bean returns a BCryptPasswordEncoder instance")
    void passwordEncoderReturnsBCrypt() {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("passwordEncoder bean is available in the application context")
    void passwordEncoderBeanInContext() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("passwordEncoder encodes and matches passwords correctly")
    void passwordEncoderEncodesAndMatches() {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        String raw = "testPassword123";
        String encoded = encoder.encode(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }

    // ──────────────────────────────────────────────
    // SecurityFilterChain bean
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("SecurityFilterChain bean is present in the application context")
    void securityFilterChainBeanExists() {
        SecurityFilterChain filterChain = applicationContext.getBean(SecurityFilterChain.class);
        assertThat(filterChain).isNotNull();
    }

    // ──────────────────────────────────────────────
    // CSRF disabled
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("CSRF is disabled - POST without CSRF token is not rejected")
    void csrfIsDisabled() throws Exception {
        // POST to /register without a CSRF token should NOT return 403 Forbidden
        // If CSRF were enabled, a POST without a token would be rejected with 403
        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    // ──────────────────────────────────────────────
    // /register is publicly accessible
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("/register endpoint is publicly accessible (permitAll)")
    void registerEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("/register POST is publicly accessible without authentication")
    void registerPostIsPublic() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "newpass"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    // ──────────────────────────────────────────────
    // All other requests require authentication
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("/dashboard requires authentication - unauthenticated redirects to login")
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("/someOtherPage requires authentication - unauthenticated redirects to login")
    void anyOtherRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/someOtherPage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("/api/accounts requires authentication")
    void apiEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ──────────────────────────────────────────────
    // Custom login page at /login
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Custom login page is configured at /login")
    void customLoginPageConfigured() throws Exception {
        // /login should be accessible without authentication (permitAll)
        mockMvc.perform(get("/login"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Login processing URL is /login (POST)")
    void loginProcessingUrl() throws Exception {
        // POST to /login should be handled by Spring Security's form login
        mockMvc.perform(post("/login")
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should redirect (to /login?error on failure, or /dashboard on success)
                    assertThat(status).isIn(302, 200);
                });
    }

    @Test
    @DisplayName("Successful login redirects to /dashboard")
    void successfulLoginRedirectsToDashboard() throws Exception {
        UserDetails userDetails = new User("testuser",
                new BCryptPasswordEncoder().encode("testpass"),
                Collections.singletonList(new SimpleGrantedAuthority("USER")));
        when(accountService.loadUserByUsername("testuser")).thenReturn(userDetails);

        mockMvc.perform(post("/login")
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Failed login redirects to /login with error")
    void failedLoginRedirectsToLoginError() throws Exception {
        when(accountService.loadUserByUsername(anyString()))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

        mockMvc.perform(post("/login")
                        .param("username", "baduser")
                        .param("password", "badpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ──────────────────────────────────────────────
    // Logout invalidates session
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Logout endpoint invalidates session and redirects to /login?logout")
    void logoutInvalidatesSessionAndRedirects() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("Logout POST also works and redirects to /login?logout")
    void logoutPostRedirects() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // ──────────────────────────────────────────────
    // Frame options set to sameOrigin
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("X-Frame-Options header is set to SAMEORIGIN")
    void frameOptionsSameOrigin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    @DisplayName("X-Frame-Options SAMEORIGIN is present on authenticated endpoints too")
    void frameOptionsSameOriginOnProtectedEndpoints() throws Exception {
        // Even the redirect response should contain the header
        mockMvc.perform(get("/dashboard"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    // ──────────────────────────────────────────────
    // configureGlobal wires AccountService
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("configureGlobal wires AccountService as UserDetailsService - login delegates to it")
    void configureGlobalWiresAccountService() throws Exception {
        // When a login attempt is made, Spring Security should delegate to AccountService
        UserDetails userDetails = new User("serviceuser",
                new BCryptPasswordEncoder().encode("servicepass"),
                Collections.singletonList(new SimpleGrantedAuthority("USER")));
        when(accountService.loadUserByUsername("serviceuser")).thenReturn(userDetails);

        mockMvc.perform(post("/login")
                        .param("username", "serviceuser")
                        .param("password", "servicepass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Verify that AccountService.loadUserByUsername was actually called
        org.mockito.Mockito.verify(accountService).loadUserByUsername("serviceuser");
    }

    @Test
    @DisplayName("configureGlobal uses BCryptPasswordEncoder for password matching")
    void configureGlobalUsesBCryptEncoder() throws Exception {
        BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();
        String encodedPassword = realEncoder.encode("correctpass");

        UserDetails userDetails = new User("encodeduser",
                encodedPassword,
                Collections.singletonList(new SimpleGrantedAuthority("USER")));
        when(accountService.loadUserByUsername("encodeduser")).thenReturn(userDetails);

        // Correct password should succeed
        mockMvc.perform(post("/login")
                        .param("username", "encodeduser")
                        .param("password", "correctpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Wrong password should fail
        mockMvc.perform(post("/login")
                        .param("username", "encodeduser")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
