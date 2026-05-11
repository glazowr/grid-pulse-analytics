package com.glazowr.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.Instant;

@Builder
public record EnergyUsageDto (
        @NotNull(message = "Device ID cannot be null")
        @Positive(message = "Device ID must be positive")
        Long deviceId,

        @Positive(message = "Energy consumed must be greater than 0")
        double energyConsumed,

        @NotNull(message = "Timestamp cannot be null")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {}