package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "species")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "common_name", unique = true, nullable = false)
    private String commonName;

    @Column(name = "scientific_name", nullable = false)
    private String scientificName;

    @Column(name = "iucn_status", nullable = false)
    private String iucnStatus; // CR, EN, VU, NT, LC

    @Column(name = "typical_speed_kmh", nullable = false)
    private Double typicalSpeedKmh = 4.0;

    private String habitat;

    @Column(name = "diet_type")
    private String dietType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
