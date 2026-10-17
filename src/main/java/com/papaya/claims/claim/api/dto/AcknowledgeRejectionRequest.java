package com.papaya.claims.claim.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AcknowledgeRejectionRequest(@NotNull @PositiveOrZero Long expectedVersion) { }
