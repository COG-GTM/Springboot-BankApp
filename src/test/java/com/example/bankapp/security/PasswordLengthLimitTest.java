package com.example.bankapp.security;

import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// CVE-2025-22228: BCrypt only uses the first 72 bytes of a password
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordLengthLimitTest {

    private static final String PREFIX_72 = "a".repeat(72);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void springSecurityIncludesCve202522228Fix() {
        String[] v = SpringSecurityCoreVersion.getVersion().split("\\.");
        int major = Integer.parseInt(v[0]);
        int minor = Integer.parseInt(v[1]);
        int patch = Integer.parseInt(v[2].replaceAll("\\D.*", ""));
        boolean fixed = major > 6
                || (major == 6 && minor >= 5)
                || (major == 6 && minor == 4 && patch >= 4)
                || (major == 6 && minor == 3 && patch >= 8);
        assertTrue(fixed, "Spring Security " + SpringSecurityCoreVersion.getVersion() + " is affected by CVE-2025-22228");
    }

    @Test
    void encoderRejectsPasswordLongerThan72Bytes() {
        assertThrows(IllegalArgumentException.class, () -> passwordEncoder.encode(PREFIX_72 + "secret"));
    }

    @Test
    void registrationRejects73BytePassword() throws Exception {
        mockMvc.perform(post("/register").param("username", "len73").param("password", PREFIX_72 + "x"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("passwordError", "Password must be at most 72 bytes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Password must be at most 72 bytes")));
        assertFalse(accountRepository.findByUsername("len73").isPresent());
    }

    @Test
    void registrationCountsBytesNotCharacters() throws Exception {
        String password = "\u20ac".repeat(25); // 25 chars, 75 UTF-8 bytes
        assertEquals(75, password.getBytes(StandardCharsets.UTF_8).length);
        mockMvc.perform(post("/register").param("username", "euro").param("password", password))
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("passwordError"));
        assertFalse(accountRepository.findByUsername("euro").isPresent());
    }

    @Test
    void longPasswordPrefixCannotBeUsedToLogIn() throws Exception {
        mockMvc.perform(post("/register").param("username", "alice").param("password", PREFIX_72 + "secret"));

        mockMvc.perform(formLogin("/login").user("alice").password(PREFIX_72 + "guess"))
                .andExpect(unauthenticated());
    }

    @Test
    void passwordOf72BytesStillWorks() throws Exception {
        mockMvc.perform(post("/register").param("username", "len72").param("password", PREFIX_72))
                .andExpect(redirectedUrl("/login"));
        assertTrue(accountRepository.findByUsername("len72").isPresent());

        mockMvc.perform(formLogin("/login").user("len72").password(PREFIX_72))
                .andExpect(authenticated().withUsername("len72"));
        mockMvc.perform(formLogin("/login").user("len72").password("a".repeat(71) + "b"))
                .andExpect(unauthenticated());
    }
}
