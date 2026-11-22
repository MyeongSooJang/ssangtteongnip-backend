package com.ssangtteongnip.web.company;

import com.ssangtteongnip.application.service.CompanyCatalogService;
import com.ssangtteongnip.application.service.CompanyService;
import com.ssangtteongnip.application.service.dto.CompanyCandidateResponse;
import com.ssangtteongnip.application.service.dto.CompanyResponse;
import com.ssangtteongnip.application.service.dto.CompanyReviewResponse;
import com.ssangtteongnip.application.service.request.CompanyRequest;
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
    private final CompanyCatalogService companyCatalogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> register(@RequestBody @Valid CompanyRequest request) {
        return ApiResponse.ok(companyService.register(request));
    }

    @GetMapping
    public ApiResponse<List<CompanyResponse>> findAll() {
        return ApiResponse.ok(companyService.findAll());
    }

    // 회사 이름 자동완성. 글자가 없으면 빈 목록이다.
    @GetMapping("/candidates")
    public ApiResponse<List<CompanyCandidateResponse>> findCandidates(
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(companyCatalogService.findCandidates(keyword));
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
