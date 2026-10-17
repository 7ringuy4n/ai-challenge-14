package com.papaya.claims.web;

import com.papaya.claims.contract.BusinessException;
import com.papaya.claims.observability.TraceContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public org.springframework.http.ResponseEntity<ErrorEnvelope> business(BusinessException error, HttpServletRequest request) {
        return response(error.status(), error.code(), error.getMessage(), request);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class})
    public org.springframework.http.ResponseEntity<ErrorEnvelope> badRequest(Exception error, HttpServletRequest request) {
        String message = "Request body or path parameter is invalid";
        if (error instanceof MethodArgumentNotValidException invalid) {
            FieldError field = invalid.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
            if (field != null) message = "Invalid value for " + field.getField();
        }
        return response(400, "INVALID_REQUEST", message, request);
    }
    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorEnvelope> unexpected(Exception error, HttpServletRequest request) {
        return response(500, "INTERNAL_ERROR", "The request could not be completed", request);
    }
    public static org.springframework.http.ResponseEntity<ErrorEnvelope> response(int status, String code, String message, HttpServletRequest request) {
        Object trace = request.getAttribute(TraceContextFilter.TRACE_ID_ATTRIBUTE);
        return org.springframework.http.ResponseEntity.status(status).body(new ErrorEnvelope(code, message,
                trace instanceof String value ? value : "unavailable", Map.of()));
    }
}
