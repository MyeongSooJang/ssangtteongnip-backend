package com.ssangtteongnip.domain.user;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final int MIN_PASSWORD_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 한 이메일로 한 번만 가입한다는 규칙은 users 테이블의 것이라 값 객체가 아니라 여기서 유니크를 건다.
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private Email email;

    @Column(nullable = false)
    private String name;

    // 닉네임은 기존 사용자에게 없을 수 있어(null) 비워 둔다. 유니크는 이메일과 같은 방식으로 컬럼에 건다.
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "nickname", unique = true))
    private Nickname nickname;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String passwordHash;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static User register(Email email, String name, Nickname nickname, String rawPassword, PasswordHasher hasher) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        User user = new User();
        user.email = Objects.requireNonNull(email);
        user.name = name;
        user.nickname = Objects.requireNonNull(nickname);
        user.passwordHash = hasher.hash(rawPassword);
        return user;
    }

    public void authenticate(String rawPassword, PasswordHasher hasher) {
        if (rawPassword == null || !hasher.matches(rawPassword, passwordHash)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeNickname(Nickname nickname) {
        this.nickname = Objects.requireNonNull(nickname);
    }

    // 닉네임이 없는 기존 사용자는 게시판에 글을 쓰기 전에 닉네임을 정해야 한다.
    public void validateHasNickname() {
        if (nickname == null) {
            throw new BusinessException(ErrorCode.NICKNAME_REQUIRED);
        }
    }

    @PrePersist
    private void recordCreation() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void recordModification() {
        this.updatedAt = LocalDateTime.now();
    }
}
