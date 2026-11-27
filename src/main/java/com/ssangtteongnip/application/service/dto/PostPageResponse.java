package com.ssangtteongnip.application.service.dto;

import java.util.List;

public record PostPageResponse(List<PostSummaryResponse> posts, int page, int pageSize, long totalCount) {}
