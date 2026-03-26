package com.playtab.cloudgateservice.dto;

import java.time.LocalDateTime;

public record StageResponse(
        Long id,
        String name,
        Integer maxCapacity,
        LocalDateTime createdAt
) {}
