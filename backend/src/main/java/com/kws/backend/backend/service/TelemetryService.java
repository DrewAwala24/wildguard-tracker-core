package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.TelemetryRequestDto;
import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.GeofenceRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;
    private final AnimalRepository animalRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public void processTelemetry(TelemetryRequestDto dto) {
        Animal animal = animalRepository.findByCollarId(dto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Animal collar not found: " + dto.getCollarId()));

        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        point.setSRID(4326);

        TelemetryLocation telemetry = new TelemetryLocation();
        telemetry.setAnimal(animal);
        telemetry.setLocation(point);
        telemetry.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : java.time.LocalDateTime.now());

        telemetryRepository.save(telemetry);

        // Check spatial boundaries for geofence breaches
        var breaches = geofenceRepository.findZonesContainingPoint(dto.getLongitude(), dto.getLatitude());
        if (!breaches.isEmpty()) {
            // Trigger early-warning alert notifications here
        }
    }
}