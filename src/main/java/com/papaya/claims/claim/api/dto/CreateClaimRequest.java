package com.papaya.claims.claim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Map;

public record CreateClaimRequest(@NotNull @PositiveOrZero @Digits(integer = 16, fraction = 2) BigDecimal policyLimit,
                                 @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
                                 @NotNull @Size(max = 100) Map<String, Object> facts) { }
