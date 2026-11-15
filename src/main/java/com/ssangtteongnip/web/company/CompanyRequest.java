package com.ssangtteongnip.web.company;

import com.ssangtteongnip.application.service.command.RegisterCompanyCommand;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String industry,
        String website
) {

    public RegisterCompanyCommand toCommand() {
        return new RegisterCompanyCommand(name, industry, website);
    }
}
