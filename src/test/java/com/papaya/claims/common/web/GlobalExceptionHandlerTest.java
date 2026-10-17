package com.papaya.claims.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.service.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsBusinessErrorAndCarriesEffectiveCorrelationId() {
        var request = request("correlation-17");
        var response = handler.business(new BusinessException("VERSION_CONFLICT", "Claim version is stale", 409), request);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.code()).isEqualTo("VERSION_CONFLICT");
            assertThat(error.message()).isEqualTo("Claim version is stale");
            assertThat(error.correlationId()).isEqualTo("correlation-17");
        });
    }

    @Test
    void validationErrorsContainFieldDetailsAndStablePublicMessage() {
        var binding = new BeanPropertyBindingResult(new MutablePropertyValues(), "request");
        binding.addError(new FieldError("request", "currency", "invalid"));
        binding.addError(new FieldError("request", "amount", "must be positive"));
        var exception = new MethodArgumentNotValidException(null, binding);
        var response = handler.invalid(exception, request(null));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).isEqualTo("Request validation failed");
            assertThat(error.details()).containsExactly("currency: invalid", "amount: must be positive");
            assertThat(error.correlationId()).isNotBlank();
        });
    }

    @Test
    void malformedAndUnexpectedErrorsDoNotExposeInternalExceptionMessages() {
        var malformed = handler.malformed(new IllegalArgumentException("private parser detail"), request("safe-id"));
        assertThat(malformed.getBody()).satisfies(error -> {
            assertThat(error.code()).isEqualTo("INVALID_REQUEST");
            assertThat(error.message()).isEqualTo("Request is malformed or contains an invalid value");
            assertThat(error.message()).doesNotContain("private parser detail");
        });

        var unexpected = handler.unexpected(new IllegalStateException("database password leaked"), request("safe-id"));
        assertThat(unexpected.getStatusCode().value()).isEqualTo(500);
        assertThat(unexpected.getBody()).satisfies(error -> {
            assertThat(error.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(error.message()).isEqualTo("The request could not be completed");
            assertThat(error.message()).doesNotContain("database password");
        });
    }

    private static MockHttpServletRequest request(String correlationId) {
        var request = new MockHttpServletRequest("GET", "/api/v1/claims");
        if (correlationId != null) request.setAttribute(CorrelationIdFilter.ATTRIBUTE, correlationId);
        return request;
    }
}
