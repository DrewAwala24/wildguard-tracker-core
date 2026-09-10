package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "geofence_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String zoneName;

    @Column(nullable = false)
    private String zoneType; // e.g., PARK, PRIVATE_FARM

    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon boundary;
}