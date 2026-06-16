package com.example.bankapp.config;

import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        securityConfig.accountService = accountService;
    }

    @Test
    void passwordEncoderReturnsBCryptInstance() {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void passwordEncoderRoundTripsRawPassword() {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();

        String hash = encoder.encode("raw");

        assertTrue(encoder.matches("raw", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void securityFilterChainBuildsAndReturnsTheBuiltChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain builtChain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(builtChain);

        SecurityFilterChain result = securityConfig.securityFilterChain(http);

        assertSame(builtChain, result);
        verify(http).build();
    }

    @Test
    void configureGlobalRegistersAccountServiceAsUserDetailsService() throws Exception {
        AuthenticationManagerBuilder auth = mock(AuthenticationManagerBuilder.class, RETURNS_DEEP_STUBS);

        securityConfig.configureGlobal(auth);

        verify(auth).userDetailsService(accountService);
    }
}
