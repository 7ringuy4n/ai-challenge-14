package com.papaya.claims.claim.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateFactsRequest(@NotNull @PositiveOrZero Long expectedVersion,
                                 @NotNull @Size(max = 100) Map<String, Object> facts) { }
