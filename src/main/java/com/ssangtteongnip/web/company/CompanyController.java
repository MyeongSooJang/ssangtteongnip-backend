package com.ssangtteongnip.web.company;

import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.application.service.request.CompanyRequest;
import com.ssangtteongnip.domain.company.Company;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Company> register(@RequestBody @Valid CompanyRequest request) {
        return ApiResponse.ok(companyService.register(request));
    }

    @GetMapping
    public ApiResponse<List<CompanyResponse>> findAll() {
        return ApiResponse.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(companyService.findDetail(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<CompanyReviewResponse>> findReviews(@PathVariable Long id) {
        return ApiResponse.ok(companyService.findReviews(id));
    }
}
