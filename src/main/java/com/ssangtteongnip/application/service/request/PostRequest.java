package com.ssangtteongnip.application.service.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// multipart 요청을 컨트롤러가 이 모양으로 풀어 넘긴다. 파일은 application이 web을 참조하지 않도록 바이트로 받는다.
// 제목·내용 규칙은 Post가 원본이라 검증 어노테이션을 두지 않는다. 글을 새로 쓸 때는 keepImageIds를 비워서 넘긴다.
public record PostRequest(String title, String content, Set<UUID> keepImageIds, List<byte[]> images) {

    // 생략하면 남길 이미지도 새 이미지도 없는 것으로 본다.
    public PostRequest {
        keepImageIds = keepImageIds == null ? Set.of() : keepImageIds;
        images = images == null ? List.of() : images;
    }
}
