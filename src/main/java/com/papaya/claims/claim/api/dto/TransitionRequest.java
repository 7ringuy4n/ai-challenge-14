package com.papaya.claims.claim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record TransitionRequest(@NotBlank @Size(max = 80) String targetState,
                                @NotNull @PositiveOrZero Long expectedVersion,
                                @Size(max = 2000) String reason,
                                @NotNull @Size(max = 100) Map<String, Object> facts) { }
