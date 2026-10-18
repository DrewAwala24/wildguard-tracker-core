package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_code")
    private String incidentCode;

    @Column(name = "animal_name")
    private String animalName;

    @Column(name = "collar_id")
    private String collarId;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "park_name")
    private String parkName;

    private String severity; // CRITICAL, WARNING, INFO

    @Column(columnDefinition = "TEXT")
    private String message;

    private String status; // ACTIVE, ACKNOWLEDGED, DISPATCHED, RESOLVED

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "dispatched_unit")
    private String dispatchedUnit;

    private Double latitude;
    private Double longitude;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)")
    private org.locationtech.jts.geom.Point location;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
