package com.ssangtteongnip.domain.schedule;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    private String address;

    private String note;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void validateOwner(Long userId) {
        if (!application.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }

    public boolean isInterview() {
        return type == ScheduleType.INTERVIEW;
    }

    public void changeAddress(String address) {
        this.address = address;
    }

    public static Schedule create(JobApplication application, String title,
                                  LocalDateTime scheduledAt, ScheduleType type, String address, String note) {
        Schedule schedule = new Schedule();
        schedule.application = application;
        schedule.title = title;
        schedule.scheduledAt = scheduledAt;
        schedule.type = type;
        schedule.address = address;
        schedule.note = note;
        return schedule;
    }

    @PrePersist
    private void recordCreation() {
        this.createdAt = LocalDateTime.now();
    }
}
