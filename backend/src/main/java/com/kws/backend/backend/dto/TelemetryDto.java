package com.kws.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryDto {
    private Long id;
    private String animalId;
    private String collarId;
    private AnimalDto animal;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}
