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

    @Column(name = "animal_name")
    private String animalName;

    @Column(name = "collar_id")
    private String collarId;

    @Column(name = "zone_name")
    private String zoneName;

    private String severity; // CRITICAL, WARNING, INFO

    @Column(columnDefinition = "TEXT")
    private String message;

    private String status; // ACTIVE, DISPATCHED, RESOLVED

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "dispatched_unit")
    private String dispatchedUnit;

    private LocalDateTime timestamp;
}
