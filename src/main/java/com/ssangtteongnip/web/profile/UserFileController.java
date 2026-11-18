package com.ssangtteongnip.web.profile;

import com.ssangtteongnip.application.service.UserFileService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.UserFileResponse;
import com.ssangtteongnip.domain.profile.FileType;
import com.ssangtteongnip.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/me/files")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileService userFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserFileResponse> upload(@AuthenticationPrincipal Long userId,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestPart("file") MultipartFile file) throws IOException {
        FileType fileType = FileType.parse(type);
        return ApiResponse.ok(userFileService.upload(userId, fileType, file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping
    public ApiResponse<List<UserFileResponse>> findAll(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userFileService.findAll(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileDownload> findDownload(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(userFileService.findDownload(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        userFileService.delete(userId, id);
        return ApiResponse.ok();
    }
}
