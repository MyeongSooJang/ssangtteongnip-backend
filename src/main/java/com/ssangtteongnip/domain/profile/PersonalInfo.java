package com.ssangtteongnip.domain.profile;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalInfo {

    private String phone;
    private LocalDate birthDate;
    private String address;

    @Column(length = 300)
    private String githubUrl;
    @Column(length = 300)
    private String blogUrl;
    @Column(length = 300)
    private String portfolioUrl;

    @Column(length = 2000)
    private String education;
    @Column(length = 2000)
    private String career;

    private LocalDate jobSearchStartDate;

    private String desiredJob;

    @Column(length = 2000)
    private String introduction;

    public static PersonalInfo empty() {
        return new PersonalInfo(null, null, null, null, null, null, null, null, null, null, null);
    }

    public PersonalInfo(String phone, LocalDate birthDate, String address,
                        String githubUrl, String blogUrl, String portfolioUrl,
                        String education, String career,
                        String desiredJob, String introduction,
                        LocalDate jobSearchStartDate) {
        this.phone = phone;
        this.birthDate = birthDate;
        this.address = address;
        this.githubUrl = githubUrl;
        this.blogUrl = blogUrl;
        this.portfolioUrl = portfolioUrl;
        this.education = education;
        this.career = career;
        this.desiredJob = desiredJob;
        this.introduction = introduction;
        this.jobSearchStartDate = jobSearchStartDate;
    }

    // 취업 준비 시작일은 비워 둘 수 있지만, 적었다면 오늘보다 뒤일 수 없다.
    void validateJobSearchStartDate(LocalDate today) {
        if (jobSearchStartDate != null && jobSearchStartDate.isAfter(today)) {
            throw new BusinessException(ErrorCode.FUTURE_JOB_SEARCH_START_DATE);
        }
    }
}
