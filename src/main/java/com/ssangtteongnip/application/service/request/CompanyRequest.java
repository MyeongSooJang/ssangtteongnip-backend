package com.ssangtteongnip.application.service.request;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String industry,
        String website
) {}
