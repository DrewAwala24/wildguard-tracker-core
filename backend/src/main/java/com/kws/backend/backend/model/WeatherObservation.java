package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_observations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "park_name", nullable = false)
    private String parkName;

    @Column(name = "temperature_c", nullable = false)
    private Double temperatureC;

    @Column(nullable = false)
    private String conditions;

    @Column(name = "humidity_percent", nullable = false)
    private Integer humidityPercent;

    @Column(name = "wind_speed_kmh", nullable = false)
    private Double windSpeedKmh;

    @Column(name = "observation_source", nullable = false)
    private String observationSource; // SENSOR_METEO, CACHED_METEO, FALLBACK

    @Column(name = "observation_time", nullable = false)
    private LocalDateTime observationTime = LocalDateTime.now();

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();
}
