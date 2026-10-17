package com.papaya.claims.claim.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateClaimRequestValidationTest {
    @Test
    void validatesPolicyLimitAsNonnegativeNumericEighteenTwoAtTheHttpBoundary() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valid = new CreateClaimRequest(new BigDecimal("9999999999999999.99"), "USD", Map.of());
            var excessScale = new CreateClaimRequest(new BigDecimal("1.001"), "USD", Map.of());
            var excessPrecision = new CreateClaimRequest(new BigDecimal("10000000000000000"), "USD", Map.of());
            var negative = new CreateClaimRequest(new BigDecimal("-0.01"), "USD", Map.of());

            assertThat(validator.validate(valid)).isEmpty();
            assertThat(validator.validate(excessScale)).extracting(violation -> violation.getPropertyPath().toString())
                    .contains("policyLimit");
            assertThat(validator.validate(excessPrecision)).extracting(violation -> violation.getPropertyPath().toString())
                    .contains("policyLimit");
            assertThat(validator.validate(negative)).extracting(violation -> violation.getPropertyPath().toString())
                    .contains("policyLimit");
        }
    }
}
