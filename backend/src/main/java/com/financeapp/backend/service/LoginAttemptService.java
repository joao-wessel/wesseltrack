package com.financeapp.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.max-login-attempts}") int maxAttempts,
            @Value("${app.security.lock-duration-minutes}") long lockDurationMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
    }

    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(key.toLowerCase());
        if (state == null) {
            return false;
        }
        if (state.blockedUntil != null && Instant.now().isAfter(state.blockedUntil)) {
            attempts.remove(key.toLowerCase());
            return false;
        }
        return state.blockedUntil != null;
    }

    public void loginSucceeded(String key) {
        attempts.remove(key.toLowerCase());
    }

    public void loginFailed(String key) {
        attempts.compute(key.toLowerCase(), (k, current) -> {
            AttemptState next = current == null ? new AttemptState() : current;
            next.failures++;
            if (next.failures >= maxAttempts) {
                next.blockedUntil = Instant.now().plus(lockDuration);
            }
            return next;
        });
    }

    private static class AttemptState {
        private int failures;
        private Instant blockedUntil;
    }
}
