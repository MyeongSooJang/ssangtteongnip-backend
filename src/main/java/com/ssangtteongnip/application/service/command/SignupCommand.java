package com.ssangtteongnip.application.service.command;

public record SignupCommand(
        String email,
        String name,
        String password
) {}
