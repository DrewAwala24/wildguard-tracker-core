package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.GeofenceRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final TelemetryRepository telemetryRepository;
    private final GeofenceRepository geofenceRepository;
    private final TelemetrySimulationService simulationService;

    public List<AnimalDto> getAllAnimals() {
        return animalRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public AnimalDto registerAnimal(AnimalDto dto) {
        Animal animal = new Animal();
        animal.setName(dto.getName());
        animal.setSpecies(dto.getSpecies());
        animal.setCollarId(dto.getCollarId());
        Animal saved = animalRepository.save(animal);
        return toDto(saved);
    }

    private AnimalDto toDto(Animal animal) {
        AnimalDto dto = new AnimalDto();
        dto.setId(animal.getId());
        dto.setName(animal.getName());
        dto.setSpecies(animal.getSpecies());
        dto.setCollarId(animal.getCollarId());
        dto.setParkName(determineParkName(animal.getCollarId()));
        dto.setSex(determineSex(animal.getName()));
        dto.setCollarBattery((int) Math.round(simulationService.getBattery(animal.getId())));

        Optional<TelemetryLocation> latestLoc = telemetryRepository.findTop1ByAnimalIdOrderByTimestampDesc(animal.getId());
        if (latestLoc.isPresent() && latestLoc.get().getLocation() != null) {
            double lat = latestLoc.get().getLocation().getY();
            double lng = latestLoc.get().getLocation().getX();
            dto.setLatitude(lat);
            dto.setLongitude(lng);
            dto.setLastSeen(latestLoc.get().getTimestamp());

            // PostGIS Geofence Breach Check
            List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(lng, lat);
            boolean inBuffer = zones.stream().anyMatch(z -> "COMMUNITY_BUFFER".equalsIgnoreCase(z.getZoneType()));
            dto.setIsBreaching(inBuffer);

            if (inBuffer) {
                dto.setStatus("Alert");
            } else if (dto.getCollarBattery() != null && dto.getCollarBattery() < 20) {
                dto.setStatus("Low Battery");
            } else {
                dto.setStatus("Active");
            }
        } else {
            // Default baseline if no telemetry recorded yet
            double[] baseline = getFallbackCoord(animal.getCollarId());
            dto.setLatitude(baseline[0]);
            dto.setLongitude(baseline[1]);
            dto.setIsBreaching(false);
            dto.setStatus("Active");
        }

        return dto;
    }

    private String determineParkName(String collarId) {
        if (collarId == null) return "Tsavo East NP";
        String id = collarId.toUpperCase();
        if (id.contains("AMB")) return "Amboseli NP";
        if (id.contains("TSV")) return "Tsavo East NP";
        if (id.contains("MAR")) return "Maasai Mara";
        if (id.contains("NBI")) return "Nairobi NP";
        if (id.contains("OLP")) return "Ol Pejeta";
        return "Amboseli NP";
    }

    private String determineSex(String name) {
        if (name == null) return "Unknown";
        String lower = name.toLowerCase();
        if (lower.contains("bull") || lower.contains("male") || lower.contains("simba") || lower.contains("kipsing") || lower.contains("baraka")) {
            return "Male";
        }
        if (lower.contains("matriarch") || lower.contains("female") || lower.contains("pride") || lower.contains("talek") || lower.contains("zuri")) {
            return "Female";
        }
        return "Unknown";
    }

    private double[] getFallbackCoord(String collarId) {
        if (collarId == null) return new double[]{-2.6531, 37.2625};
        String id = collarId.toUpperCase();
        if (id.contains("AMB")) return new double[]{-2.6531, 37.2625};
        if (id.contains("TSV")) return new double[]{-2.8124, 38.6210};
        if (id.contains("MAR")) return new double[]{-1.4850, 35.1280};
        if (id.contains("NBI")) return new double[]{-1.3780, 36.8420};
        if (id.contains("OLP")) return new double[]{0.0420, 36.9450};
        return new double[]{-2.6531, 37.2625};
    }
}