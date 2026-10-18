package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_dispatches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispatch_code", unique = true, nullable = false)
    private String dispatchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentLog incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrol_unit_id", nullable = false)
    private PatrolUnit patrolUnit;

    @Column(name = "call_sign", nullable = false)
    private String callSign;

    @Column(name = "target_park", nullable = false)
    private String targetPark;

    @Column(name = "target_latitude", nullable = false)
    private Double targetLatitude;

    @Column(name = "target_longitude", nullable = false)
    private Double targetLongitude;

    @Column(nullable = false)
    private String status; // DISPATCHED, EN_ROUTE, ON_SCENE, COMPLETED, CANCELLED

    @Column(name = "commander_username", nullable = false)
    private String commanderUsername;

    @Column(name = "dispatch_time", nullable = false)
    private LocalDateTime dispatchTime = LocalDateTime.now();

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
