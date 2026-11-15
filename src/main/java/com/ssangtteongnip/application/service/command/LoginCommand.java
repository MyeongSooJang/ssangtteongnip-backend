package com.ssangtteongnip.application.service.command;

public record LoginCommand(
        String email,
        String password
) {}
