package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.profile.PersonalInfo;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    private final User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());

    @Test
    void create_빈_인적_사항으로_프로필을_생성한다() {
        Profile profile = Profile.create(user);

        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getInfo().getPhone()).isNull();
        assertThat(profile.getInfo().getIntroduction()).isNull();
        assertThat(profile.getInfo().getJobSearchStartDate()).isNull();
    }

    @Test
    void 이름과_인적_사항을_함께_수정한다() {
        Profile profile = Profile.create(user);
        PersonalInfo info = PersonalInfo.of("010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", null, null, "OO대 컴퓨터공학", "인턴 6개월", "백엔드 개발자", "안녕하세요",
                LocalDate.of(2025, 3, 1));

        profile.reviseNameAndInfo("홍길순", info, TODAY);

        assertThat(user.getName()).isEqualTo("홍길순");
        assertThat(profile.getInfo().getPhone()).isEqualTo("010-1234-5678");
        assertThat(profile.getInfo().getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 27));
        assertThat(profile.getInfo().getDesiredJob()).isEqualTo("백엔드 개발자");
        assertThat(profile.getInfo().getJobSearchStartDate()).isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void 취업_준비_시작일을_비우면_null이_된다() {
        Profile profile = Profile.create(user);
        profile.reviseNameAndInfo("홍길동", PersonalInfo.of(null, null, null, null, null, null, null, null, null, null,
                LocalDate.of(2025, 3, 1)), TODAY);

        profile.reviseNameAndInfo("홍길동", PersonalInfo.empty(), TODAY);

        assertThat(profile.getInfo().getJobSearchStartDate()).isNull();
    }

    @Test
    void 취업_준비_시작일이_오늘이면_저장된다() {
        Profile profile = Profile.create(user);

        profile.reviseNameAndInfo("홍길동", infoStartedOn(TODAY), TODAY);

        assertThat(profile.getInfo().getJobSearchStartDate()).isEqualTo(TODAY);
    }

    @Test
    void 취업_준비_시작일이_미래이면_이름과_인적_사항을_바꾸지_않는다() {
        Profile profile = Profile.create(user);

        assertErrorCode(() -> profile.reviseNameAndInfo("홍길순", infoStartedOn(TODAY.plusDays(1)), TODAY),
                ErrorCode.FUTURE_JOB_SEARCH_START_DATE);
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(profile.getInfo().getJobSearchStartDate()).isNull();
    }

    private PersonalInfo infoStartedOn(LocalDate jobSearchStartDate) {
        return PersonalInfo.of(null, null, null, null, null, null, null, null, null, null, jobSearchStartDate);
    }
}
