package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.Nickname;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class NicknameTest {

    @ParameterizedTest
    @ValueSource(strings = {"길동이", "gildong", "홍길동12", "ab", "가나다라마바사아자차"})
    void 한글_영문_숫자_2자에서_10자는_닉네임이_될_수_있다(String raw) {
        assertThat(new Nickname(raw).value()).isEqualTo(raw);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "a", "가나다라마바사아자차카", "길 동", "길동!", "ㅋㅋ", "닉네임_1"})
    void 비었거나_길이_문자_규칙을_어기면_닉네임이_될_수_없다(String raw) {
        assertErrorCode(() -> new Nickname(raw), ErrorCode.INVALID_NICKNAME);
    }

    @Test
    void 앞뒤_공백은_지운다() {
        assertThat(new Nickname(" 길동이 ").value()).isEqualTo("길동이");
    }

    @Test
    void 값이_같으면_같은_닉네임이다() {
        assertThat(new Nickname("길동이")).isEqualTo(new Nickname(" 길동이"));
    }
}
