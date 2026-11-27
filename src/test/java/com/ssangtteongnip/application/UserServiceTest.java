package com.ssangtteongnip.application;

import com.ssangtteongnip.application.service.UserService;
import com.ssangtteongnip.application.service.dto.NicknameResponse;
import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.Nickname;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock UserRepository userRepository;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        user = User.register(new Email("hong@example.com"), "홍길동", new Nickname("길동이"), "password1",
                new FakePasswordHasher());
    }

    @Test
    void 닉네임을_바꾸면_새_닉네임이_저장된다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        userService.changeNickname(USER_ID, " 길순이 ");

        assertThat(user.getNickname()).isEqualTo(new Nickname("길순이"));
        verify(userRepository).flush();
    }

    @Test
    void 닉네임_형식이_틀리면_사용자를_찾지_않고_거절한다() {
        assertErrorCode(() -> userService.changeNickname(USER_ID, "길"), ErrorCode.INVALID_NICKNAME);
    }

    @Test
    void 다른_요청이_먼저_같은_닉네임을_가져갔으면_중복으로_거절한다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        willThrow(new DataIntegrityViolationException("uk_users_nickname")).given(userRepository).flush();

        assertErrorCode(() -> userService.changeNickname(USER_ID, "길순이"), ErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    @Test
    void 없는_사용자의_닉네임은_바꿀_수_없다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userService.changeNickname(USER_ID, "길순이"), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 내_닉네임을_조회한다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertThat(userService.findNickname(USER_ID).nickname()).isEqualTo("길동이");
    }

    @Test
    void 닉네임이_없는_기존_사용자는_null을_돌려준다() {
        ReflectionTestUtils.setField(user, "nickname", null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        NicknameResponse response = userService.findNickname(USER_ID);

        assertThat(response.nickname()).isNull();
    }
}
