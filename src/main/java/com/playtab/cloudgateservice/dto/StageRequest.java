package com.playtab.cloudgateservice.dto;

import jakarta.validation.constraints.NotBlank;

public record StageRequest(
        @NotBlank String name,
        Integer maxCapacity
) {}
