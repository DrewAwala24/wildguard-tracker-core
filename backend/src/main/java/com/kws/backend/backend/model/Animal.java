package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "animals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column(name = "collar_id", unique = true, nullable = false)
    private String collarId;

    @Column(name = "park_name")
    private String parkName;

    @Column(name = "sex")
    private String sex;

    @Column(name = "iucn_status")
    private String iucnStatus;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "registered_at")
    private java.time.LocalDateTime registeredAt = java.time.LocalDateTime.now();

    public Animal(Long id, String name, String species, String collarId) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.collarId = collarId;
        this.active = true;
        this.registeredAt = java.time.LocalDateTime.now();
    }

    public Animal(Long id, String name, String species, String collarId, String parkName, String sex, String iucnStatus) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.collarId = collarId;
        this.parkName = parkName;
        this.sex = sex;
        this.iucnStatus = iucnStatus;
        this.active = true;
        this.registeredAt = java.time.LocalDateTime.now();
    }
}