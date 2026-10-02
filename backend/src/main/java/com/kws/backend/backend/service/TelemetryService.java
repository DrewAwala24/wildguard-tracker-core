package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.dto.TelemetryDto;
import com.kws.backend.backend.dto.TelemetryRequestDto;
import com.kws.backend.backend.dto.TelemetryTrailDto;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<TelemetryLocation> getAllTelemetry() {
        return telemetryRepository.findAll();
    }

    public List<TelemetryDto> getAllTelemetryDtos() {
        return telemetryRepository.findAll().stream().map(t -> {
            Animal animal = t.getAnimal();
            AnimalDto animalDto = null;
            String collarId = null;
            if (animal != null) {
                collarId = animal.getCollarId();
                animalDto = new AnimalDto();
                animalDto.setId(animal.getId());
                animalDto.setName(animal.getName());
                animalDto.setSpecies(animal.getSpecies());
                animalDto.setCollarId(animal.getCollarId());
            }
            Double lat = t.getLocation() != null ? t.getLocation().getY() : null;
            Double lng = t.getLocation() != null ? t.getLocation().getX() : null;
            return new TelemetryDto(
                    t.getId(),
                    collarId,
                    collarId,
                    animalDto,
                    lat,
                    lng,
                    t.getTimestamp()
            );
        }).collect(Collectors.toList());
    }

    public List<TelemetryTrailDto> getAnimalTrail(String collarId) {
        var animalOpt = animalRepository.findByCollarId(collarId);
        if (animalOpt.isEmpty()) {
            return Collections.emptyList();
        }
        List<TelemetryLocation> recent = telemetryRepository.findTop15ByAnimal_IdOrderByTimestampDesc(animalOpt.get().getId());
        // Reverse so it's chronologically oldest to newest for map polyline
        Collections.reverse(recent);
        return recent.stream()
                .filter(t -> t.getLocation() != null)
                .map(t -> new TelemetryTrailDto(t.getLocation().getY(), t.getLocation().getX(), t.getTimestamp()))
                .collect(Collectors.toList());
    }
}