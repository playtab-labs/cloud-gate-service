package com.playtab.cloudgateservice.dto;

import java.time.LocalDateTime;

public record ReaderResponse(
        Long id,
        String serialNumber,
        Long stageId,
        String stageName,
        String direction,
        String status,
        LocalDateTime createdAt
) {}
