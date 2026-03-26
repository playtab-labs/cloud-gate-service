package com.playtab.cloudgateservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TagRequest(
        @NotBlank String readerSerial,
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{14}$", message = "chip serial must be 14 hex characters")
        String chipSerial
) {}
