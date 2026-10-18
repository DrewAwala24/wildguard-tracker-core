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
public class AnimalDto {
    private Long id;
    private String name;
    private String species;
    private String collarId;
    private String parkName;
    private String sex;
    private Integer collarBattery;
    private Double latitude;
    private Double longitude;
    private String status;
    private Boolean isBreaching;
    private LocalDateTime lastSeen;

    // Tactical & Telemetry Analytics
    private Double speedKmh;
    private String headingCompass;
    private String behaviorState;
    private String fenceDistance;
    private String predictedCorridor;
    private String solarVoltageDisplay;
    private String collarTempDisplay;
    private String signalStrength;
    private String iucnStatus;
    private String iucnLabel;
    private String speciesIcon;
    private String imageFileName;
    private String imageUrl;

    public String getCoordinateDisplay() {
        if (latitude == null || longitude == null) return "Unknown";
        return String.format("%.4f°, %.4f°", latitude, longitude);
    }

    public String getSpeedDisplay() {
        if (speedKmh == null) return "0.0 km/h";
        return String.format("%.1f km/h", speedKmh);
    }
}