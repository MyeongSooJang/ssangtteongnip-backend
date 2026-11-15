package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobSite;
import com.ssangtteongnip.domain.jobapplication.JobSiteStatistics;

public record JobSiteStatisticsResponse(JobSite jobSite, long applicationCount, long documentPassedCount,
                                        double documentPassRate) {

    public static JobSiteStatisticsResponse from(JobSiteStatistics statistics) {
        return new JobSiteStatisticsResponse(statistics.jobSite(), statistics.applicationCount(),
                statistics.documentPassedCount(), statistics.documentPassRate());
    }
}
