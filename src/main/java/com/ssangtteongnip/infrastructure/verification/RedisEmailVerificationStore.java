package com.ssangtteongnip.infrastructure.verification;

import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.EmailVerificationStore;
import com.ssangtteongnip.domain.user.VerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {

    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    @Override
    public boolean tryAcquireCooldown(Email email) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(createKey("cooldown", email), "1", COOLDOWN_TTL));
    }

    @Override
    public void releaseCooldown(Email email) {
        redis.delete(createKey("cooldown", email));
    }

    @Override
    public void saveCode(Email email, VerificationCode code) {
        redis.opsForValue().set(createKey("code", email), code.value(), CODE_TTL);
        redis.delete(createKey("attempts", email));
    }

    @Override
    public Optional<VerificationCode> findCode(Email email) {
        return Optional.ofNullable(redis.opsForValue().get(createKey("code", email))).map(VerificationCode::new);
    }

    @Override
    public long recordAttempt(Email email) {
        String attemptsKey = createKey("attempts", email);
        long attempts = Objects.requireNonNull(redis.opsForValue().increment(attemptsKey));
        if (attempts == 1) {
            redis.expire(attemptsKey, CODE_TTL);
        }
        return attempts;
    }

    @Override
    public void deleteCode(Email email) {
        redis.delete(List.of(createKey("code", email), createKey("attempts", email)));
    }

    @Override
    public void markVerified(Email email) {
        redis.opsForValue().set(createKey("verified", email), "1", VERIFIED_TTL);
    }

    @Override
    public boolean isVerified(Email email) {
        return Boolean.TRUE.equals(redis.hasKey(createKey("verified", email)));
    }

    @Override
    public void clearVerified(Email email) {
        redis.delete(createKey("verified", email));
    }

    private String createKey(String kind, Email email) {
        return "email-verify:" + kind + ":" + email.value();
    }
}
