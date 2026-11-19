package com.example.bankapp.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed sign-in attempts per username and per client address within a sliding window
 * and locks the key out for a fixed period once the limit is reached. Unknown usernames are
 * counted exactly like existing ones so the lockout itself does not reveal which accounts exist.
 */
@Service
public class LoginAttemptService {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final int maxFailuresPerUser;
    private final int maxFailuresPerClient;
    private final Duration window;
    private final Duration lockout;
    private final Clock clock;

    private final Map<String, Attempts> userAttempts = new ConcurrentHashMap<>();
    private final Map<String, Attempts> clientAttempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${bankapp.security.login.max-failures-per-user:5}") int maxFailuresPerUser,
            @Value("${bankapp.security.login.max-failures-per-client:20}") int maxFailuresPerClient,
            @Value("${bankapp.security.login.failure-window:PT15M}") Duration window,
            @Value("${bankapp.security.login.lockout-duration:PT15M}") Duration lockout,
            ObjectProvider<Clock> clock) {
        this.maxFailuresPerUser = maxFailuresPerUser;
        this.maxFailuresPerClient = maxFailuresPerClient;
        this.window = window;
        this.lockout = lockout;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    public boolean isBlocked(String username, String clientAddress) {
        return isUserBlocked(username) || isClientBlocked(clientAddress);
    }

    public boolean isUserBlocked(String username) {
        return isLocked(userAttempts, userKey(username));
    }

    public boolean isClientBlocked(String clientAddress) {
        return isLocked(clientAttempts, clientKey(clientAddress));
    }

    public void loginFailed(String username, String clientAddress) {
        recordFailure(userAttempts, userKey(username), maxFailuresPerUser);
        recordFailure(clientAttempts, clientKey(clientAddress), maxFailuresPerClient);
    }

    public void loginSucceeded(String username) {
        userAttempts.remove(userKey(username));
    }

    public void clientFailed(String clientAddress) {
        recordFailure(clientAttempts, clientKey(clientAddress), maxFailuresPerClient);
    }

    private boolean isLocked(Map<String, Attempts> attempts, String key) {
        Attempts entry = attempts.get(key);
        return entry != null && entry.isLocked(clock.instant());
    }

    private void recordFailure(Map<String, Attempts> attempts, String key, int maxFailures) {
        Instant now = clock.instant();
        if (attempts.size() > PRUNE_THRESHOLD) {
            attempts.values().removeIf(entry -> entry.isExpired(now));
        }
        attempts.compute(key, (k, entry) -> {
            if (entry == null || entry.isExpired(now)) {
                entry = new Attempts(now);
            }
            entry.failures++;
            if (entry.failures >= maxFailures) {
                entry.lockedUntil = now.plus(lockout);
            }
            return entry;
        });
    }

    private static String userKey(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String clientKey(String clientAddress) {
        return clientAddress == null ? "" : clientAddress;
    }

    private final class Attempts {
        private final Instant windowStart;
        private int failures;
        private Instant lockedUntil;

        private Attempts(Instant windowStart) {
            this.windowStart = windowStart;
        }

        private boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        private boolean isExpired(Instant now) {
            return !isLocked(now) && !now.isBefore(windowStart.plus(window));
        }
    }
}
