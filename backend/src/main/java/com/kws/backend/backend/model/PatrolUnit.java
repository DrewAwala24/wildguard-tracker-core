package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "patrol_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "call_sign", unique = true, nullable = false)
    private String callSign;

    @Column(name = "unit_type", nullable = false)
    private String unitType; // LAND_CRUISER, AIRWING, FOOT_PATROL

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String status; // AVAILABLE, DISPATCHED, ON_PATROL

    @Column(nullable = false)
    private String sector; // e.g. Amboseli NP, Tsavo East NP

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "current_location", columnDefinition = "geometry(Point,4326)")
    private org.locationtech.jts.geom.Point currentLocation;

    @Column(name = "last_status_update")
    private java.time.LocalDateTime lastStatusUpdate = java.time.LocalDateTime.now();

    public PatrolUnit(Long id, String name, String callSign, String unitType, Double latitude, Double longitude, String status, String sector) {
        this.id = id;
        this.name = name;
        this.callSign = callSign;
        this.unitType = unitType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.sector = sector;
        this.lastStatusUpdate = java.time.LocalDateTime.now();
    }
}
