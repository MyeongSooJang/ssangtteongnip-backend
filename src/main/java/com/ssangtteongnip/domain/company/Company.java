package com.ssangtteongnip.domain.company;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String industry;

    private String website;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static Company create(String name, String industry, String website) {
        Company company = new Company();
        company.name = name;
        company.industry = industry;
        company.website = website;
        return company;
    }

    public void reviseInfo(String name, String industry, String website) {
        this.name = name;
        this.industry = industry;
        this.website = website;
    }

    @PrePersist
    private void recordCreation() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void recordModification() {
        this.updatedAt = LocalDateTime.now();
    }
}
