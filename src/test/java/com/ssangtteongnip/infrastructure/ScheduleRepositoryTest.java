package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.domain.company.CompanyRepository;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationRepository;
import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleRepository;
import com.ssangtteongnip.domain.schedule.ScheduleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ScheduleRepositoryTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDateTime FIRST = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime SECOND = LocalDateTime.of(2026, 11, 20, 10, 0);
    private static final LocalDateTime THIRD = LocalDateTime.of(2026, 11, 30, 10, 0);

    @Autowired CompanyRepository companyRepository;
    @Autowired JobApplicationRepository jobApplicationRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired EntityManager em;

    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.create("카카오", "IT", "https://kakao.com"));
        saveSchedule(USER_ID, "첫째", FIRST);
        saveSchedule(USER_ID, "둘째", SECOND);
        saveSchedule(USER_ID, "셋째", THIRD);
        saveSchedule(OTHER_USER_ID, "다른 사용자의 일정", SECOND);
        em.flush();
        em.clear();
    }

    private void saveSchedule(Long userId, String title, LocalDateTime scheduledAt) {
        JobApplication application = jobApplicationRepository.save(
                JobApplication.create(userId, company, "백엔드 개발자", LocalDate.of(2026, 10, 1)));
        scheduleRepository.save(Schedule.create(application, title, scheduledAt, ScheduleType.INTERVIEW, "판교", ""));
    }

    @Test
    void 기간을_주지_않으면_본인의_일정을_모두_조회한다() {
        List<Schedule> found = scheduleRepository.findInPeriod(USER_ID, null, null);

        assertThat(found).extracting(Schedule::getTitle).containsExactlyInAnyOrder("첫째", "둘째", "셋째");
    }

    @Test
    void 시작과_끝을_모두_주면_양_끝을_포함한_기간의_일정만_조회한다() {
        List<Schedule> found = scheduleRepository.findInPeriod(USER_ID, FIRST, SECOND);

        assertThat(found).extracting(Schedule::getTitle).containsExactlyInAnyOrder("첫째", "둘째");
    }

    @Test
    void 시작만_주면_그_시각_이후의_일정을_조회한다() {
        List<Schedule> found = scheduleRepository.findInPeriod(USER_ID, SECOND, null);

        assertThat(found).extracting(Schedule::getTitle).containsExactlyInAnyOrder("둘째", "셋째");
    }

    @Test
    void 끝만_주면_그_시각_이전의_일정을_조회한다() {
        List<Schedule> found = scheduleRepository.findInPeriod(USER_ID, null, SECOND);

        assertThat(found).extracting(Schedule::getTitle).containsExactlyInAnyOrder("첫째", "둘째");
    }
}
