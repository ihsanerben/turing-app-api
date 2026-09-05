package com.turing.app.api.common.error;

import com.turing.app.api.application.exception.ApplicationException;
import com.turing.app.api.audience.exception.AudienceListException;
import com.turing.app.api.audit.exception.AuditException;
import com.turing.app.api.auth.exception.AuthException;
import com.turing.app.api.content.exception.ContentException;
import com.turing.app.api.interview.exception.InterviewException;
import com.turing.app.api.notification.exception.NotificationException;
import com.turing.app.api.participation.exception.ParticipationException;
import com.turing.app.api.profile.exception.ProfileException;
import com.turing.app.api.scholarship.exception.ScholarshipException;
import com.turing.app.api.user.exception.UserManagementException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ParticipationException.class)
  public ResponseEntity<ApiErrorResponse> handleParticipation(
      ParticipationException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiErrorResponse> handleAuth(
      AuthException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(AuditException.class)
  public ResponseEntity<ApiErrorResponse> handleAudit(
      AuditException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(AudienceListException.class)
  public ResponseEntity<ApiErrorResponse> handleAudienceList(
      AudienceListException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ProfileException.class)
  public ResponseEntity<ApiErrorResponse> handleProfile(
      ProfileException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ScholarshipException.class)
  public ResponseEntity<ApiErrorResponse> handleScholarship(
      ScholarshipException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiErrorResponse> handleApplication(
      ApplicationException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(InterviewException.class)
  public ResponseEntity<ApiErrorResponse> handleInterview(
      InterviewException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(NotificationException.class)
  public ResponseEntity<ApiErrorResponse> handleNotification(
      NotificationException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ContentException.class)
  public ResponseEntity<ApiErrorResponse> handleContent(
      ContentException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(UserManagementException.class)
  public ResponseEntity<ApiErrorResponse> handleUserManagement(
      UserManagementException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
      Exception exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.CONFLICT,
        "VERSION_CONFLICT",
        "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.",
        request,
        List.of());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
      MaxUploadSizeExceededException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "FILE_TOO_LARGE",
        "Dosya izin verilen boyutu aşıyor.",
        request,
        List.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(
      NoResourceFoundException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.NOT_FOUND,
        "ENDPOINT_NOT_FOUND",
        "İstenen sayfa veya servis bulunamadı.",
        request,
        List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream().map(this::toFieldError).toList();

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "Gönderilen alanları kontrol edin.",
        request,
        fieldErrors);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
      Exception exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "Gönderilen bilgi eksik veya geçersiz. Alanları kontrol edin.",
        request,
        List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    log.warn("Database rule rejected request path={}", request.getRequestURI());
    return buildResponse(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "Bu işlem mevcut kayıtlarla çakışıyor. Bilgileri kontrol edip yeniden deneyin.",
        request,
        List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error(
        "Unexpected request failure method={} path={}",
        request.getMethod(),
        request.getRequestURI(),
        exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Beklenmeyen bir hata oluştu.",
        request,
        List.of());
  }

  private FieldErrorResponse toFieldError(FieldError error) {
    return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<FieldErrorResponse> fieldErrors) {
    ApiErrorResponse response =
        new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            request.getRequestURI(),
            MDC.get("requestId"),
            fieldErrors);
    return ResponseEntity.status(status).body(response);
  }
}
