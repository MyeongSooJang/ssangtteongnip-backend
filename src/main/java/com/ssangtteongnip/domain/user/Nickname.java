package com.ssangtteongnip.domain.user;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

// 게시판에서 작성자를 보여주는 이름. 앞뒤 공백을 지우고 한글·영문·숫자 2~10자인지 검사한다.
// 영문 대소문자는 구분한다(Kim과 kim은 다른 닉네임이다).
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nickname {

    private static final Pattern PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$");

    @Column(name = "nickname")
    private String value;

    public Nickname(String rawNickname) {
        String trimmed = rawNickname == null ? "" : rawNickname.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }
        this.value = trimmed;
    }

    // 응답 본문이 문자열을 필요로 해서 노출한다.
    public String value() {
        return value;
    }
}
