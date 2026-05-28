package eu.px.genba.common;

import eu.px.genba.common.exception.GenbaException;
import eu.px.genba.common.exception.InvalidCredentialsException;
import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.common.exception.TokenInvalidException;
import eu.px.genba.common.exception.UserInactiveException;
import eu.px.genba.i18n.MessageProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain and framework exceptions to the {@link ApiError} envelope with
 * the request-locale message resolved via {@link MessageProvider}.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageProvider messageProvider;

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return build(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<ApiError> handleUserInactive(
            UserInactiveException ex, HttpServletRequest request) {
        return build(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ApiError> handleTokenInvalid(
            TokenInvalidException ex, HttpServletRequest request) {
        return build(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException ex, HttpServletRequest request) {
        return build(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        String key = "common.error.forbidden";
        ApiError body = ApiError.builder()
                .messageKey(key)
                .message(messageProvider.getForRequest(request, key))
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode("FORBIDDEN")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fe.getField(), k -> new ArrayList<>()).add(fe.getDefaultMessage());
        }
        String key = "common.error.validation";
        ApiError body = ApiError.builder()
                .messageKey(key)
                .message(messageProvider.getForRequest(request, key))
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .errorCode("VALIDATION_FAILED")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(GenbaException.class)
    public ResponseEntity<ApiError> handleGenba(GenbaException ex, HttpServletRequest request) {
        return build(ex, HttpStatus.valueOf(ex.getHttpStatus()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        String key = "common.error.unexpected";
        ApiError body = ApiError.builder()
                .messageKey(key)
                .message(messageProvider.getForRequest(request, key))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("UNEXPECTED")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ApiError> build(
            GenbaException ex, HttpStatus status, HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .messageKey(ex.getMessageKey())
                .message(messageProvider.getForRequest(request, ex.getMessageKey()))
                .status(status.value())
                .errorCode(ex.getErrorCode())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
