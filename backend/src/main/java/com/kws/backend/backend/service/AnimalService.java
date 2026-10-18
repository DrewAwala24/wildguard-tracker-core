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
    private final AuditService auditService;

    public List<AnimalDto> getAllAnimals() {
        return animalRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Animal> getAllAnimalEntities() {
        return animalRepository.findAll();
    }

    public Optional<Animal> getAnimalById(Long id) {
        return animalRepository.findById(id);
    }

    public Optional<Animal> getAnimalByCollarId(String collarId) {
        return animalRepository.findByCollarId(collarId);
    }

    @org.springframework.transaction.annotation.Transactional
    public AnimalDto registerAnimal(AnimalDto dto) {
        return registerAnimal(dto, "COMMANDER");
    }

    @org.springframework.transaction.annotation.Transactional
    public AnimalDto registerAnimal(AnimalDto dto, String username) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Animal name is required.");
        }
        if (dto.getCollarId() == null || dto.getCollarId().isBlank()) {
            throw new IllegalArgumentException("Collar ID is required.");
        }
        if (animalRepository.findByCollarId(dto.getCollarId()).isPresent()) {
            throw new IllegalArgumentException("Collar ID already registered: " + dto.getCollarId());
        }

        Animal animal = new Animal();
        animal.setName(dto.getName().trim());
        animal.setSpecies(dto.getSpecies() != null ? dto.getSpecies().trim() : "African Bush Elephant");
        animal.setCollarId(dto.getCollarId().trim().toUpperCase());
        animal.setParkName(dto.getParkName() != null ? dto.getParkName().trim() : "Amboseli NP");
        animal.setSex(dto.getSex() != null ? dto.getSex().trim() : "Female");
        animal.setIucnStatus(resolveIucnStatus(animal.getSpecies()));
        animal.setActive(true);
        animal.setRegisteredAt(java.time.LocalDateTime.now());

        Animal saved = animalRepository.save(animal);
        auditService.logAction(username, "ANIMAL_REGISTERED", "ANIMAL", saved.getCollarId(),
                "Registered new animal " + saved.getName() + " (" + saved.getSpecies() + ") in " + saved.getParkName());

        return toDto(saved);
    }

    @org.springframework.transaction.annotation.Transactional
    public AnimalDto updateAnimal(Long id, AnimalDto dto, String username) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found with ID: " + id));

        animal.setName(dto.getName());
        if (dto.getSpecies() != null) {
            animal.setSpecies(dto.getSpecies());
            animal.setIucnStatus(resolveIucnStatus(dto.getSpecies()));
        }
        if (dto.getParkName() != null) {
            animal.setParkName(dto.getParkName());
        }
        if (dto.getSex() != null) {
            animal.setSex(dto.getSex());
        }

        Animal updated = animalRepository.save(animal);
        auditService.logAction(username, "ANIMAL_UPDATED", "ANIMAL", updated.getCollarId(),
                "Updated details for animal " + updated.getName());

        return toDto(updated);
    }

    @org.springframework.transaction.annotation.Transactional
    public void retireCollar(String collarId, String username) {
        Animal animal = animalRepository.findByCollarId(collarId)
                .orElseThrow(() -> new IllegalArgumentException("Animal with collar " + collarId + " not found."));

        animal.setActive(false);
        animalRepository.save(animal);

        auditService.logAction(username, "COLLAR_RETIRED", "ANIMAL", collarId,
                "Retired collar for animal " + animal.getName() + ". Historical telemetry preserved.");
    }

    public AnimalDto toDto(Animal animal) {
        AnimalDto dto = new AnimalDto();
        dto.setId(animal.getId());
        dto.setName(animal.getName());
        dto.setSpecies(animal.getSpecies());
        dto.setCollarId(animal.getCollarId());

        String park = simulationService.normalizeParkName(animal.getParkName(), animal.getCollarId());
        dto.setParkName(park);
        dto.setSex(animal.getSex() != null ? animal.getSex() : determineSex(animal.getName()));

        String iucn = animal.getIucnStatus() != null ? animal.getIucnStatus() : resolveIucnStatus(animal.getSpecies());
        dto.setIucnStatus(iucn);
        dto.setIucnLabel(resolveIucnLabel(iucn));
        dto.setSpeciesIcon(resolveSpeciesIcon(animal.getSpecies()));

        String imgName = resolveImageFileName(animal.getName(), animal.getSpecies());
        dto.setImageFileName(imgName);
        dto.setImageUrl("/images/" + imgName);

        int battery = (int) Math.round(simulationService.getBattery(animal.getId()));
        dto.setCollarBattery(battery);

        dto.setSpeedKmh(simulationService.getSpeed(animal.getCollarId()));
        dto.setHeadingCompass(simulationService.getHeadingCompass(animal.getCollarId()));
        dto.setBehaviorState(simulationService.getBehavior(animal.getCollarId()));
        dto.setFenceDistance(simulationService.getFenceDistance(animal.getCollarId()));
        dto.setPredictedCorridor(simulationService.getCorridor(animal.getCollarId()));
        dto.setSolarVoltageDisplay(String.format("☀️ %.1fV (Solar Float)", 13.8 + (battery * 0.005)));
        dto.setCollarTempDisplay("🌡️ 27°C (Nominal)");
        dto.setSignalStrength("🛰️ -82 dBm (Iridium SAT)");

        Optional<TelemetryLocation> latestLoc = telemetryRepository.findTop1ByAnimal_IdOrderByTimestampDesc(animal.getId());
        if (latestLoc.isPresent() && latestLoc.get().getLocation() != null) {
            double lat = latestLoc.get().getLocation().getY();
            double lng = latestLoc.get().getLocation().getX();
            dto.setLatitude(lat);
            dto.setLongitude(lng);
            dto.setLastSeen(latestLoc.get().getTimestamp());

            // PostGIS Geofence Breach Check
            boolean inBuffer = false;
            try {
                List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(lng, lat);
                inBuffer = zones.stream().anyMatch(z -> "COMMUNITY_BUFFER".equalsIgnoreCase(z.getZoneType()));
            } catch (Exception ignored) {}
            dto.setIsBreaching(inBuffer);

            if (inBuffer) {
                dto.setStatus("Alert");
            } else if (battery < 20) {
                dto.setStatus("Low Battery");
            } else {
                dto.setStatus("Active");
            }
        } else {
            double[] baseline = simulationService.getBaselineCoordinate(park, animal.getCollarId());
            dto.setLatitude(baseline[0]);
            dto.setLongitude(baseline[1]);
            dto.setIsBreaching(false);
            dto.setStatus("Active");
        }

        return dto;
    }

    public static String resolveSpeciesIcon(String species) {
        if (species == null) return "🐾";
        String s = species.toLowerCase();
        if (s.contains("elephant")) return "🐘";
        if (s.contains("rhino")) return "🦏";
        if (s.contains("bongo")) return "🦌";
        if (s.contains("sitatunga")) return "🦌";
        if (s.contains("zebra")) return "🦓";
        if (s.contains("lion")) return "🦁";
        if (s.contains("cheetah")) return "🐆";
        if (s.contains("eagle")) return "🦅";
        if (s.contains("secretary")) return "🦅";
        if (s.contains("giraffe")) return "🦒";
        if (s.contains("wild dog") || s.contains("lycaon")) return "🐺";
        if (s.contains("hyena")) return "🐺";
        if (s.contains("buffalo")) return "🦬";
        if (s.contains("hippo")) return "🦛";
        return "🐾";
    }

    public static String resolveIucnStatus(String species) {
        if (species == null) return "LC";
        String s = species.toLowerCase();
        if (s.contains("eastern black") || s.contains("black rhino") || s.contains("northern white") || s.contains("bongo")) {
            return "CR"; // Critically Endangered
        }
        if (s.contains("grevy") || s.contains("wild dog") || s.contains("martial eagle") || s.contains("secretary bird")) {
            return "EN"; // Endangered
        }
        if (s.contains("elephant") || s.contains("lion") || s.contains("cheetah") || s.contains("giraffe")) {
            return "VU"; // Vulnerable
        }
        if (s.contains("white rhino") || s.contains("leopard")) {
            return "NT"; // Near Threatened
        }
        return "LC"; // Least Concern
    }

    public static String resolveIucnLabel(String iucn) {
        if (iucn == null) return "Least Concern";
        return switch (iucn) {
            case "CR" -> "Critically Endangered";
            case "EN" -> "Endangered";
            case "VU" -> "Vulnerable";
            case "NT" -> "Near Threatened";
            default -> "Least Concern";
        };
    }

    private String determineSex(String name) {
        if (name == null) return "Unknown";
        String lower = name.toLowerCase();
        if (lower.contains("bull") || lower.contains("male") || lower.contains("simba") || lower.contains("kipsing") || lower.contains("baraka") || lower.contains("tusker") || lower.contains("stallion") || lower.contains("buck") || lower.contains("hunter")) {
            return "Male";
        }
        if (lower.contains("matriarch") || lower.contains("female") || lower.contains("pride") || lower.contains("talek") || lower.contains("zuri") || lower.contains("mare") || lower.contains("doe") || lower.contains("najin") || lower.contains("fatu")) {
            return "Female";
        }
        return "Female";
    }

    public static String resolveImageFileName(String name, String species) {
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("mutula")) return "mutula_bull.jpg";
            if (lower.contains("kibo")) return "kibo.jpg";
            if (lower.contains("satao")) return "satao.jpg";
            if (lower.contains("kipsing")) return "kipsing.jpg";
            if (lower.contains("mukurwe")) return "mukurwe.jpg";
            if (lower.contains("simba")) return "simba.jpg";
            if (lower.contains("talek")) return "talek.jpg";
            if (lower.contains("zuri")) return "zuri.jpg";
            if (lower.contains("baraka")) return "baraka_rh.jpg";
        }
        if (species != null) {
            String s = species.toLowerCase();
            if (s.contains("rhino")) return "baraka.jpg";
            if (s.contains("elephant")) return "mutula_bull.jpg";
            if (s.contains("lion")) return "simba.jpg";
            if (s.contains("cheetah")) return "talek.jpg";
            if (s.contains("zebra")) return "zuri.jpg";
            if (s.contains("giraffe")) return "kibo.jpg";
        }
        return "kws_logo.jpg";
    }
}