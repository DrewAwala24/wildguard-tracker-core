package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "park_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String parkName;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private Double areaSqKm;

    @Column(nullable = false)
    private String ecosystemType;

    @Column(nullable = false)
    private Integer establishedYear;

    @Column(nullable = false)
    private String rangerHq;

    @Column(nullable = false)
    private String fenceType;

    @Column(nullable = false)
    private String threatLevel;

    @Column(columnDefinition = "TEXT")
    private String keyWaterholes;

    @Column(columnDefinition = "TEXT")
    private String keySpecies;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;

    @Column(nullable = false)
    private Integer defaultZoom;
}
