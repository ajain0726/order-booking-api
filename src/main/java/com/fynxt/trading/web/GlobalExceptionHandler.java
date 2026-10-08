package com.fynxt.trading.web;

import com.fynxt.trading.exception.ErrorCode;
import com.fynxt.trading.exception.TradingException;
import com.fynxt.trading.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Translates exceptions into the uniform {@link ErrorResponse} body.
 * Expected business failures log at WARN; anything unexpected logs at ERROR
 * with a stack trace and returns a generic message (no internals leaked).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TradingException.class)
    ResponseEntity<ErrorResponse> handleTrading(TradingException ex, HttpServletRequest request) {
        log.warn("{} {} rejected: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformed(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST,
                "Malformed request: check JSON syntax, field types and enum values (side must be BUY or SELL)",
                request, List.of());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleLockTimeout(PessimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Lock contention on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "The trader is busy with another request; please retry", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Integrity constraint violated on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "The request conflicts with the current state; please retry", request, List.of());
    }

    /** Spring MVC's own 4xx errors (unknown path, wrong verb, wrong content type) keep their status. */
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ErrorResponse> handleFrameworkClientError(Exception ex, HttpServletRequest request) {
        HttpStatusCode status = ((org.springframework.web.ErrorResponse) ex).getStatusCode();
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(), "REQUEST_REJECTED", ex.getMessage(),
                request.getRequestURI(), List.of());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, List.of());
    }

    private static ResponseEntity<ErrorResponse> build(ErrorCode code, String message,
                                                       HttpServletRequest request, List<String> details) {
        ErrorResponse body = new ErrorResponse(Instant.now(), code.httpStatus().value(),
                code.httpStatus().getReasonPhrase(), code.name(), message, request.getRequestURI(), details);
        return ResponseEntity.status(code.httpStatus()).body(body);
    }
}
