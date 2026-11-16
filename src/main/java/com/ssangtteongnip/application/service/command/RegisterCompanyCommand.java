package com.ssangtteongnip.application.service.command;

public record RegisterCompanyCommand(
        String name,
        String industry,
        String website
) {}
