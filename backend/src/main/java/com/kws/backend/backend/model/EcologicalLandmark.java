package com.kws.backend.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Entity
@Table(name = "ecological_landmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcologicalLandmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "landmark_name", nullable = false)
    private String landmarkName;

    @Column(name = "landmark_type", nullable = false)
    private String landmarkType; // WATERHOLE, DAM, RIVER_CROSSING, RIDGE, SWAMP, OBS_POST

    @Column(name = "park_name", nullable = false)
    private String parkName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
