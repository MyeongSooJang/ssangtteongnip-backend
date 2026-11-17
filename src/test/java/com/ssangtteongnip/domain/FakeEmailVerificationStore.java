package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.EmailVerificationStore;
import com.ssangtteongnip.domain.user.VerificationCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeEmailVerificationStore implements EmailVerificationStore {

    private final Set<Email> cooldowns = new HashSet<>();
    private final Map<Email, VerificationCode> codes = new HashMap<>();
    private final Map<Email, Long> attempts = new HashMap<>();
    private final Set<Email> verified = new HashSet<>();

    @Override
    public boolean tryAcquireCooldown(Email email) {
        return cooldowns.add(email);
    }

    @Override
    public void releaseCooldown(Email email) {
        cooldowns.remove(email);
    }

    @Override
    public void saveCode(Email email, VerificationCode code) {
        codes.put(email, code);
        attempts.remove(email);
    }

    @Override
    public Optional<VerificationCode> findCode(Email email) {
        return Optional.ofNullable(codes.get(email));
    }

    @Override
    public long recordAttempt(Email email) {
        return attempts.merge(email, 1L, Long::sum);
    }

    @Override
    public void deleteCode(Email email) {
        codes.remove(email);
        attempts.remove(email);
    }

    @Override
    public void markVerified(Email email) {
        verified.add(email);
    }

    @Override
    public boolean isVerified(Email email) {
        return verified.contains(email);
    }

    @Override
    public void clearVerified(Email email) {
        verified.remove(email);
    }
}
