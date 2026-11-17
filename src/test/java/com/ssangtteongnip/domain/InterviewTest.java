package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.domain.interview.InterviewQuestion;
import com.ssangtteongnip.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ssangtteongnip.support.ErrorCodeAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewTest {

    private static final LocalDateTime INTERVIEW_DATE = LocalDateTime.of(2026, 11, 10, 14, 0);

    private Interview interview() {
        return Interview.create(application(), 1, INTERVIEW_DATE, "판교 본사", "1차 면접");
    }

    private JobApplication application() {
        Company company = Company.create("카카오", "IT", "https://kakao.com");
        return JobApplication.create(1L, company, "백엔드 개발자", LocalDate.of(2026, 11, 1));
    }

    @Test
    void 면접을_만들면_입력한_정보가_담기고_점수와_질문은_비어있다() {
        Interview interview = interview();

        assertThat(interview.getRound()).isEqualTo(1);
        assertThat(interview.getInterviewDate()).isEqualTo(INTERVIEW_DATE);
        assertThat(interview.getLocation()).isEqualTo("판교 본사");
        assertThat(interview.getNote()).isEqualTo("1차 면접");
        assertThat(interview.getScore()).isNull();
        assertThat(interview.getQuestions()).isEmpty();
    }

    @Test
    void 면접을_평가하면_점수가_저장된다() {
        Interview interview = interview();

        interview.evaluate(4);

        assertThat(interview.getScore()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void 점수가_0점이나_100점이면_그대로_저장된다(int score) {
        Interview interview = interview();

        interview.evaluate(score);

        assertThat(interview.getScore()).isEqualTo(score);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void 점수가_0에서_100_밖이면_평가할_수_없다(int score) {
        Interview interview = interview();

        assertErrorCode(() -> interview.evaluate(score), ErrorCode.INVALID_INTERVIEW_SCORE);
        assertThat(interview.getScore()).isNull();
    }

    @Test
    void 점수를_비우면_평가_전으로_돌아간다() {
        Interview interview = interview();
        interview.evaluate(80);

        interview.evaluate(null);

        assertThat(interview.getScore()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 차수가_1보다_작으면_면접을_만들_수_없다(int round) {
        JobApplication application = application();

        assertErrorCode(() -> Interview.create(application, round, INTERVIEW_DATE, "판교 본사", "1차 면접"),
                ErrorCode.INVALID_INTERVIEW_ROUND);
    }

    @Test
    void 질문을_추가하면_질문과_답변과_메모가_함께_기록된다() {
        Interview interview = interview();

        interview.addQuestion("자기소개 해주세요", "저는...", "잘 대답했음");

        assertThat(interview.getQuestions()).hasSize(1);
        InterviewQuestion recorded = interview.getQuestions().get(0);
        assertThat(recorded.getQuestion()).isEqualTo("자기소개 해주세요");
        assertThat(recorded.getAnswer()).isEqualTo("저는...");
        assertThat(recorded.getMemo()).isEqualTo("잘 대답했음");
    }

    @Test
    void 조회한_질문_목록은_외부에서_수정할_수_없다() {
        Interview interview = interview();

        assertThatThrownBy(() -> interview.getQuestions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
