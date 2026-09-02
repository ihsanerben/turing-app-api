package com.turing.app.api.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.turing.app.api.auth.exception.AuthException;
import com.turing.app.api.profile.exception.ProfileException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.turing.app.api.scholarship.exception.ScholarshipException;
import com.turing.app.api.application.exception.ApplicationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthException exception, HttpServletRequest request) {
        return buildResponse(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ProfileException.class)
    public ResponseEntity<ApiErrorResponse> handleProfile(ProfileException exception, HttpServletRequest request) {
        return buildResponse(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ScholarshipException.class)
    public ResponseEntity<ApiErrorResponse> handleScholarship(ScholarshipException exception, HttpServletRequest request) {
        return buildResponse(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(ApplicationException exception, HttpServletRequest request) {
        return buildResponse(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.", request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Dosya izin verilen boyutu aşıyor.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Gönderilen alanları kontrol edin.",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected request failure method={} path={}", request.getMethod(), request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Beklenmeyen bir hata oluştu.",
                request,
                List.of()
        );
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                MDC.get("requestId"),
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
