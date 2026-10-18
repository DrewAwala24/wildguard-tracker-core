package com.kws.backend.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "speed_kmh")
    private Double speedKmh = 0.0;

    @Column(name = "heading")
    private Double heading = 0.0;

    @Column(name = "battery_level")
    private Double batteryLevel = 100.0;

    @Column(name = "activity_state")
    private String activityState = "ACTIVE";

    @Column(name = "data_source")
    private String dataSource = "SIMULATED";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Double getLatitude() {
        if (latitude != null) return latitude;
        return location != null ? location.getY() : null;
    }

    public Double getLongitude() {
        if (longitude != null) return longitude;
        return location != null ? location.getX() : null;
    }

    @Transient
    public String getCollarTag() {
        return animal != null ? animal.getCollarId() : null;
    }
}