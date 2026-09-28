package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.application.exception.LoginRateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LoginAttemptService {
    private final int maxAttempts;
    private final Duration attemptWindow;
    private final Duration blockDuration;
    private final int maxTrackedKeys;
    private final Clock clock;
    private final Map<String, AttemptState> attempts;

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}")
            int maxAttempts,

            @Value("${app.security.login.window:PT15M}")
            Duration attemptWindow,

            @Value("${app.security.login.block-duration:PT15M}")
            Duration blockDuration,

            @Value("${app.security.login.max-tracked-keys:10000}")
            int maxTrackedKeys,

            Clock clock
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Login max attempts must be at least 1");
        }

        if (attemptWindow.isZero() || attemptWindow.isNegative()) {
            throw new IllegalArgumentException("Login attempt window must be positive");
        }

        if (blockDuration.isZero() || blockDuration.isNegative()) {
            throw new IllegalArgumentException("Login block duration must be positive");
        }

        if (maxTrackedKeys < 1) {
            throw new IllegalArgumentException("Login max tracked keys must be at least 1");
        }

        this.maxAttempts = maxAttempts;
        this.attemptWindow = attemptWindow;
        this.blockDuration = blockDuration;
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
        this.attempts = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AttemptState> eldest) {
                return size() > LoginAttemptService.this.maxTrackedKeys;
            }
        };
    }

    public synchronized void checkAllowed(String normalizedEmail, String clientAddress) {
        String key = createKey(normalizedEmail, clientAddress);

        AttemptState state = attempts.get(key);

        if (state == null) {
            return;
        }

        Instant now = clock.instant();

        if (state.blockedUntil != null) {
            if (now.isBefore(state.blockedUntil)) {
                long remainingSeconds = Math.max(1, Duration.between(now, state.blockedUntil).toSeconds());

                throw new LoginRateLimitExceededException(remainingSeconds);
            }

            attempts.remove(key);
            return;
        }

        if (!now.isBefore(state.windowStartedAt.plus(attemptWindow))) {
            attempts.remove(key);
        }
    }

    public synchronized void recordFailure(String normalizedEmail, String clientAddress) {
        String key = createKey(normalizedEmail, clientAddress);
        Instant now = clock.instant();
        AttemptState state = attempts.get(key);

        if (state == null || isExpired(state, now)) {
            state = new AttemptState(now);
            attempts.put(key, state);
        }

        if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) {
            return;
        }

        state.failedAttempts++;

        if (state.failedAttempts >= maxAttempts) {
            state.blockedUntil = now.plus(blockDuration);
        }
    }

    public synchronized void recordSuccess(String normalizedEmail, String clientAddress) {
        attempts.remove(createKey(normalizedEmail, clientAddress));
    }

    private boolean isExpired(AttemptState state, Instant now) {
        if (state.blockedUntil != null) {
            return !now.isBefore(state.blockedUntil);
        }

        return !now.isBefore(state.windowStartedAt.plus(attemptWindow));
    }

    private String createKey(String normalizedEmail, String clientAddress) {
        String value = normalizedEmail + "|" + normalizeClientAddress(clientAddress);

        return sha256(value);
    }

    private String normalizeClientAddress(String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            return "unknown";
        }

        return clientAddress.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(result);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static final class AttemptState {
        private int failedAttempts;
        private final Instant windowStartedAt;
        private Instant blockedUntil;

        private AttemptState(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}