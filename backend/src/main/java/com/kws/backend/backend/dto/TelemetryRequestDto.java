package com.kws.backend.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TelemetryRequestDto {
    private String collarId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
}