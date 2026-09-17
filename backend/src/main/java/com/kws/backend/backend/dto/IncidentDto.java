package com.kws.backend.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IncidentDto {
    private Long id;
    private String animalName;
    private String collarId;
    private String zoneName;
    private String severity;
    private String message;
    private String status;
    private String resolutionNotes;
    private String dispatchedUnit;
    private LocalDateTime timestamp;
}
