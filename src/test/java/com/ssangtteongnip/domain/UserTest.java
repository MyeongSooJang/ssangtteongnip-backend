package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserTest {

    private static final Email EMAIL = new Email("hong@example.com");

    private final FakePasswordHasher hasher = new FakePasswordHasher();

    @Test
    void 가입하면_비밀번호는_해시로_바뀌어_저장된다() {
        User user = User.register(EMAIL, "홍길동", "password1", hasher);

        assertThatCode(() -> user.authenticate("password1", hasher)).doesNotThrowAnyException();
        assertErrorCode(() -> user.authenticate("hashed:password1", hasher), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 비밀번호가_8자_미만이면_가입할_수_없다() {
        assertErrorCode(() -> User.register(EMAIL, "홍길동", "short12", hasher), ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void 비밀번호가_정확히_8자면_가입할_수_있다() {
        User user = User.register(EMAIL, "홍길동", "exact8ch", hasher);

        assertThat(user.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void 비밀번호가_없으면_가입할_수_없다() {
        assertErrorCode(() -> User.register(EMAIL, "홍길동", null, hasher), ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void 올바른_비밀번호면_인증을_통과한다() {
        User user = User.register(EMAIL, "홍길동", "password1", hasher);

        assertThatCode(() -> user.authenticate("password1", hasher)).doesNotThrowAnyException();
    }

    @Test
    void 비밀번호가_틀리면_인증에_실패한다() {
        User user = User.register(EMAIL, "홍길동", "password1", hasher);

        assertErrorCode(() -> user.authenticate("wrong-password", hasher), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 비밀번호가_없으면_인증에_실패한다() {
        User user = User.register(EMAIL, "홍길동", "password1", hasher);

        assertErrorCode(() -> user.authenticate(null, hasher), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 이름을_변경하면_새_이름으로_바뀐다() {
        User user = User.register(EMAIL, "홍길동", "password1", hasher);

        user.changeName("홍길순");

        assertThat(user.getName()).isEqualTo("홍길순");
    }
}
