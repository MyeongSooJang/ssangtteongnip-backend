package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class EmailTest {

    @Test
    void 앞뒤_공백을_지우고_소문자로_맞춘다() {
        Email email = new Email("  Hong@Example.COM ");

        assertThat(email.value()).isEqualTo("hong@example.com");
    }

    @Test
    void 대소문자와_앞뒤_공백만_다른_주소는_같은_이메일이다() {
        assertThat(new Email(" HONG@example.com ")).isEqualTo(new Email("hong@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "hong@example", "hong@@example.com", "ho ng@example.com", "   "})
    void 형식이_올바르지_않으면_만들_수_없다(String rawEmail) {
        assertErrorCode(() -> new Email(rawEmail), ErrorCode.INVALID_EMAIL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void 주소가_없으면_만들_수_없다(String rawEmail) {
        assertErrorCode(() -> new Email(rawEmail), ErrorCode.INVALID_EMAIL);
    }
}
