package com.ssangtteongnip.domain.user;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

// 이메일 주소. 앞뒤 공백을 지우고 소문자로 맞춘 뒤 형식을 검사한다.
// 그래서 같은 주소는 어디서 만들어도 같은 값이 되고, DB 조회 키와 인증 저장소 키가 어긋나지 않는다.
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Column(name = "email", nullable = false, unique = true)
    private String value;

    public Email(String rawEmail) {
        String normalized = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL);
        }
        this.value = normalized;
    }

    // 인증 저장소 키와 메일 수신 주소, 응답 본문이 문자열을 필요로 해서 노출한다.
    public String value() {
        return value;
    }
}
