package com.ssangtteongnip.web.plan;

import jakarta.validation.constraints.Size;

public record PlanLinkRequest(@Size(max = 500, message = "링크는 500자 이하여야 합니다.") String url) {
}
