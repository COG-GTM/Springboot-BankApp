package com.example.bankapp.security;

import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LoginEnumerationAndThrottlingTest.MutableClockConfig.class)
class LoginEnumerationAndThrottlingTest {

    private static final String PASSWORD = "Correct-Horse-1";
    private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MutableClock clock;

    @SpyBean
    private PasswordEncoder passwordEncoder;

    @Test
    void unknownUsernameStillRunsThePasswordEncoder() throws Exception {
        String ip = newIp();
        clearInvocations(passwordEncoder);

        login("ghost-" + UUID.randomUUID(), "wrong-password", ip)
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        verify(passwordEncoder, atLeastOnce()).matches(eq("wrong-password"), anyString());
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost-" + UUID.randomUUID()))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void accountIsLockedAfterFiveFailuresWithinAMinute() throws Exception {
        String user = registeredUser();
        String ip = newIp();

        for (int i = 0; i < 5; i++) {
            login(user, "wrong-" + i, ip).andExpect(redirectedUrl("/login?error"));
            clock.advance(Duration.ofSeconds(10));
        }

        login(user, PASSWORD, ip)
                .andExpect(redirectedUrl("/login?locked"))
                .andExpect(unauthenticated());
        login(user, PASSWORD, newIp())
                .andExpect(redirectedUrl("/login?locked"))
                .andExpect(unauthenticated());

        clock.advance(Duration.ofMinutes(16));
        login(user, PASSWORD, ip)
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(user));
    }

    @Test
    void unknownUsernameIsLockedTheSameWayAsAnExistingOne() throws Exception {
        String ghost = "ghost-" + UUID.randomUUID();
        String ip = newIp();

        for (int i = 0; i < 5; i++) {
            login(ghost, "wrong-" + i, ip).andExpect(redirectedUrl("/login?error"));
        }

        login(ghost, "wrong-again", ip).andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void successfulLoginResetsTheAccountFailureCount() throws Exception {
        String user = registeredUser();
        String ip = newIp();

        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 4; i++) {
                login(user, "wrong-" + i, ip).andExpect(redirectedUrl("/login?error"));
            }
            login(user, PASSWORD, ip).andExpect(redirectedUrl("/dashboard"));
        }
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() throws Exception {
        String user = registeredUser();
        String ip = newIp();

        for (int i = 0; i < 4; i++) {
            login(user, "wrong-" + i, ip).andExpect(redirectedUrl("/login?error"));
        }
        clock.advance(Duration.ofMinutes(16));
        for (int i = 0; i < 4; i++) {
            login(user, "wrong-" + i, ip).andExpect(redirectedUrl("/login?error"));
        }

        login(user, PASSWORD, ip).andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void clientAddressIsBlockedAfterTwentyFailuresAcrossUsernames() throws Exception {
        String user = registeredUser();
        String attackerIp = newIp();

        for (int i = 0; i < 20; i++) {
            login("spray-" + UUID.randomUUID(), "wrong", attackerIp).andExpect(redirectedUrl("/login?error"));
        }

        login(user, PASSWORD, attackerIp)
                .andExpect(redirectedUrl("/login?locked"))
                .andExpect(unauthenticated());
        login(user, PASSWORD, newIp())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(user));
    }

    @Test
    void registrationRejectsBlankUsername() throws Exception {
        register("   ", PASSWORD, newIp())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username is required."));
    }

    @Test
    void registrationRejectsEmptyShortAndBlankPasswords() throws Exception {
        for (String password : new String[]{"", "short", "        "}) {
            String user = "user-" + UUID.randomUUID();
            register(user, password, newIp())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("error"));
            assertThat(accountRepository.findByUsername(user)).isEmpty();
        }
    }

    @Test
    void registrationAcceptsAPasswordOfMinimumLength() throws Exception {
        String user = "user-" + UUID.randomUUID();
        register(user, "12345678", newIp()).andExpect(redirectedUrl("/login"));
        assertThat(accountRepository.findByUsername(user)).isPresent();
    }

    @Test
    void repeatedDuplicateRegistrationsFromOneAddressAreThrottled() throws Exception {
        String user = registeredUser();
        String ip = newIp();

        for (int i = 0; i < 20; i++) {
            register(user, PASSWORD, ip)
                    .andExpect(view().name("register"))
                    .andExpect(model().attribute("error", "User already present."));
        }

        register("fresh-" + UUID.randomUUID(), PASSWORD, ip)
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Too many attempts. Try again later."));
    }

    private String registeredUser() {
        String user = "user-" + UUID.randomUUID();
        accountService.registerAccount(user, PASSWORD);
        return user;
    }

    private ResultActions login(String username, String password, String ip) throws Exception {
        return mockMvc.perform(post("/login").param("username", username).param("password", password).with(from(ip)));
    }

    private ResultActions register(String username, String password, String ip) throws Exception {
        return mockMvc.perform(post("/register").param("username", username).param("password", password).with(from(ip)));
    }

    private static String newIp() {
        int n = IP_SEQ.getAndIncrement();
        return "10.0." + (n / 250) + "." + (n % 250 + 1);
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class MutableClockConfig {
        @Bean
        MutableClock testClock() {
            return new MutableClock();
        }
    }
}
