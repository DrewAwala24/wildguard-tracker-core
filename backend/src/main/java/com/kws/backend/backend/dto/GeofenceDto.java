package com.kws.backend.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class GeofenceDto {
    private Long id;
    private String zoneName;
    private String zoneType;
    private List<double[]> coordinates; // Simplified polygon boundary format
}