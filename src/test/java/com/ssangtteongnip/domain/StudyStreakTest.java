package com.ssangtteongnip.domain;

import com.ssangtteongnip.domain.studylog.StudyStreak;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudyStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private StudyStreak streakOf(int... daysAgo) {
        return new StudyStreak(java.util.Arrays.stream(daysAgo).mapToObj(TODAY::minusDays).toList());
    }

    @Test
    void 기록이_없으면_연속_작성일은_0이다() {
        assertThat(new StudyStreak(List.of()).calculateCurrent(TODAY)).isZero();
    }

    @Test
    void 오늘까지_이어서_썼으면_오늘을_포함해_센다() {
        assertThat(streakOf(0, 1, 2).calculateCurrent(TODAY)).isEqualTo(3);
    }

    @Test
    void 오늘은_아직_안_썼어도_어제까지_이어졌으면_끊기지_않는다() {
        assertThat(streakOf(1, 2, 3, 4).calculateCurrent(TODAY)).isEqualTo(4);
    }

    @Test
    void 어제도_오늘도_안_썼으면_0이다() {
        assertThat(streakOf(2, 3, 4).calculateCurrent(TODAY)).isZero();
    }

    @Test
    void 중간에_하루라도_비면_거기서_끊긴다() {
        assertThat(streakOf(0, 1, 3, 4).calculateCurrent(TODAY)).isEqualTo(2);
    }

    @Test
    void 월을_넘어서도_이어서_센다() {
        StudyStreak streak = new StudyStreak(List.of(
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 30)));

        assertThat(streak.calculateCurrent(TODAY)).isEqualTo(4);
    }

    @Test
    void 기록이_없으면_최장_연속_작성일은_0이다() {
        assertThat(new StudyStreak(List.of()).calculateLongest()).isZero();
    }

    @Test
    void 가장_긴_연속_구간의_길이를_돌려준다() {
        // 0,1 (2일) / 5,6,7,8 (4일) / 12 (1일)
        assertThat(streakOf(0, 1, 5, 6, 7, 8, 12).calculateLongest()).isEqualTo(4);
    }

    @Test
    void 최장_연속은_현재_연속과_무관하게_과거_구간도_센다() {
        assertThat(streakOf(10, 11, 12, 13, 14).calculateLongest()).isEqualTo(5);
    }

    @Test
    void 같은_날짜가_중복되어도_최장_연속_작성일은_하루로_센다() {
        assertThat(streakOf(0, 0, 1).calculateLongest()).isEqualTo(2);
    }

    @Test
    void 월을_넘어서도_최장_연속을_이어서_센다() {
        StudyStreak streak = new StudyStreak(List.of(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)));

        assertThat(streak.calculateLongest()).isEqualTo(3);
    }
}
