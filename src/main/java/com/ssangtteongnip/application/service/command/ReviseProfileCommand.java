package com.ssangtteongnip.application.service.command;

import com.ssangtteongnip.domain.profile.PersonalInfo;

public record ReviseProfileCommand(
        String name,
        PersonalInfo personalInfo
) {}
