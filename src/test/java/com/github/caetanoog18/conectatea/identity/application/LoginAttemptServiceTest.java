package com.github.caetanoog18.conectatea.identity.application;

import com.github.caetanoog18.conectatea.identity.application.exception.LoginRateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {
    @Test
    void shouldBlockAfterMaximumFailures() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = service(clock);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");

        assertThatThrownBy(() -> service.checkAllowed("user@example.com", "127.0.0.1"))
                .isInstanceOf(LoginRateLimitExceededException.class);
    }

    @Test
    void successfulLoginShouldResetFailures() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = service(clock);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");

        service.recordSuccess("user@example.com", "127.0.0.1");

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");

        assertThatCode(() -> service.checkAllowed("user@example.com", "127.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void blockShouldExpire() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = service(clock);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");

        clock.advance(Duration.ofMinutes(16));

        assertThatCode(() -> service.checkAllowed("user@example.com", "127.0.0.1"))
                .doesNotThrowAnyException();
    }

    private LoginAttemptService service(Clock clock) {
        return new LoginAttemptService(
                3, Duration.ofMinutes(10), Duration.ofMinutes(15), 100, clock);
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-09-22T00:00:00Z");

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
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}