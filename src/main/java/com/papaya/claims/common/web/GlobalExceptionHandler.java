package com.papaya.claims.common.web;

import com.papaya.claims.claim.api.dto.ApiError;
import com.papaya.claims.claim.service.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException error, HttpServletRequest request) {
        return response(error.status(), error.code(), error.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException error, HttpServletRequest request) {
        List<String> details = error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage()).toList();
        return response(400, "VALIDATION_ERROR", "Request validation failed", details, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> malformed(Exception error, HttpServletRequest request) {
        return response(400, "INVALID_REQUEST", "Request is malformed or contains an invalid value", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception error, HttpServletRequest request) {
        log.error("request_failed correlationId={} exceptionType={}", CorrelationIdFilter.currentId(request), error.getClass().getSimpleName());
        return response(500, "INTERNAL_ERROR", "The request could not be completed", List.of(), request);
    }

    private static ResponseEntity<ApiError> response(int status, String code, String message,
            List<String> details, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code, message, details, CorrelationIdFilter.currentId(request)));
    }
}
