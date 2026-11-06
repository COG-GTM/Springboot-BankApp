package com.example.bankapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = SecurityConfig.passwordEncoder();

    @Test
    void passwordUpTo72BytesRoundTrips() {
        String password = "p".repeat(72);
        String hash = encoder.encode(password);
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches("p".repeat(71), hash));
    }

    // CVE-2025-22228: affected BCryptPasswordEncoder versions silently ignore bytes after the 72nd
    @Test
    void passwordLongerThan72BytesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> encoder.encode("p".repeat(72) + "-tail"));
    }
}
