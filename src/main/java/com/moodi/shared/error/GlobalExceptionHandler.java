package com.moodi.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e, HttpServletRequest request) {
        // 외부 시스템 실패를 감싼 경우(예: GCS 서명 실패 → 503) 원인이 없으면 운영에서 진단할 수 없다.
        if (e.getCause() != null) {
            log.warn("BusinessException: {} (cause: {})", e.getMessage(), e.getCause().toString(), e.getCause());
        } else {
            log.warn("BusinessException: {}", e.getMessage());
        }
        ErrorCode errorCode = e.getErrorCode();
        return problem(errorCode.getStatus(), e.getMessage(), errorCode.getCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("ValidationException: {}", message);
        return problem(HttpStatus.BAD_REQUEST, message, "INVALID_REQUEST", request);
    }

    /**
     * 잘못된 형식의 요청은 서버 오류가 아니라 400 이다.
     * <p>
     * 이 어드바이스는 {@code ResponseEntityExceptionHandler} 를 상속하지 않으므로, 아래 표준 MVC 예외를
     * 명시적으로 받지 않으면 마지막 {@code Exception} 핸들러가 삼켜 500 으로 나간다
     * (실측: {@code /api/spots/abc}, {@code ?sort=LATEST}, {@code ?size=-1} 등이 모두 500 이었다).
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("BadRequest: {} {} — {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.", "INVALID_REQUEST", request);
    }

    /** 없는 경로는 404 로 답한다. 500 이면 클라이언트가 서버 장애와 구분할 수 없다. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("NotFound: {} {}", request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다.", "NOT_FOUND", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        log.warn("MethodNotAllowed: {} {}", request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다.", "METHOD_NOT_ALLOWED", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String detail, String code,
                                                   HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", code);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
