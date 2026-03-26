package com.playtab.cloudgateservice.dto;

import java.time.LocalDateTime;

public record TagResponse(
        Long id,
        String chipSerial,
        String readerSerial,
        String eventType,
        String stageName,
        LocalDateTime taggedAt
) {}
