package com.playtab.cloudgateservice.dto;

import java.time.LocalDateTime;

public record OccupancyResponse(
        Long stageId,
        String stageName,
        long currentCount,
        Integer maxCapacity,
        LocalDateTime updatedAt
) {}
