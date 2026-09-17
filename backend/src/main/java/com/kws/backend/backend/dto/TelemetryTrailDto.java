package com.kws.backend.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryTrailDto {
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}
