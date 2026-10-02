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

    @Transient
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    @Transient
    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    @Transient
    public String getCollarTag() {
        return animal != null ? animal.getCollarId() : null;
    }
}