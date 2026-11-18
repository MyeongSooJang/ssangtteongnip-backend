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
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key("cooldown", email), "1", COOLDOWN_TTL));
    }

    @Override
    public void releaseCooldown(Email email) {
        redis.delete(key("cooldown", email));
    }

    @Override
    public void saveCode(Email email, VerificationCode code) {
        redis.opsForValue().set(key("code", email), code.value(), CODE_TTL);
        redis.delete(key("attempts", email));
    }

    @Override
    public Optional<VerificationCode> findCode(Email email) {
        return Optional.ofNullable(redis.opsForValue().get(key("code", email))).map(VerificationCode::new);
    }

    @Override
    public long recordAttempt(Email email) {
        String attemptsKey = key("attempts", email);
        long attempts = Objects.requireNonNull(redis.opsForValue().increment(attemptsKey));
        if (attempts == 1) {
            redis.expire(attemptsKey, CODE_TTL);
        }
        return attempts;
    }

    @Override
    public void deleteCode(Email email) {
        redis.delete(List.of(key("code", email), key("attempts", email)));
    }

    @Override
    public void markVerified(Email email) {
        redis.opsForValue().set(key("verified", email), "1", VERIFIED_TTL);
    }

    @Override
    public boolean isVerified(Email email) {
        return Boolean.TRUE.equals(redis.hasKey(key("verified", email)));
    }

    @Override
    public void clearVerified(Email email) {
        redis.delete(key("verified", email));
    }

    private String key(String kind, Email email) {
        return "email-verify:" + kind + ":" + email.value();
    }
}
