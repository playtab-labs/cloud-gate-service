package com.playtab.cloudgateservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReaderRequest(
        @NotBlank String serialNumber,
        @NotNull Long stageId,
        @NotBlank String direction
) {}
