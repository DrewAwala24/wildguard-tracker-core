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
}
