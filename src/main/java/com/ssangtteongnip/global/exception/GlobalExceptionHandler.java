package com.ssangtteongnip.global.exception;

import com.ssangtteongnip.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(e.getErrorCode()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(ErrorCode.FILE_TOO_LARGE.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        return ResponseEntity
                .status(ErrorCode.EMPTY_FILE.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.EMPTY_FILE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("VALIDATION_ERROR", message));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            return handleErrorResponse(errorResponse);
        }
        log.error("처리되지 않은 예외", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    // 프레임워크가 자체 HTTP 상태를 정해 둔 예외(415, 406, 503 등)는 상태를 유지하고 본문만 봉투로 바꾼다.
    private ResponseEntity<ApiResponse<Void>> handleErrorResponse(ErrorResponse errorResponse) {
        HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = status.is4xxClientError()
                ? "요청을 처리할 수 없습니다."
                : "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.";
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON) // Accept 불일치(406)여도 본문을 쓸 수 있게 고정
                .body(ApiResponse.fail(status.name(), message));
    }
}
