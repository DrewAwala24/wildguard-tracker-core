package com.kws.backend.backend;

import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.dto.TelemetryRequestDto;
import com.kws.backend.backend.dto.TelemetryTrailDto;
import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import com.kws.backend.backend.service.AnimalService;
import com.kws.backend.backend.service.TelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TelemetryAndPersistenceTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private AnimalService animalService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    @DisplayName("Process telemetry rejects unregistered collar tags")
    void testRejectUnregisteredCollar() {
        TelemetryRequestDto dto = new TelemetryRequestDto();
        dto.setCollarId("KWS-NON-EXISTENT-999");
        dto.setLatitude(-1.35);
        dto.setLongitude(36.85);

        assertThrows(IllegalArgumentException.class, () -> {
            telemetryService.processTelemetry(dto);
        });
    }

    @Test
    @DisplayName("Process telemetry rejects invalid Kenya coordinates")
    void testRejectInvalidCoordinates() {
        TelemetryRequestDto dto = new TelemetryRequestDto();
        dto.setCollarId("KWS-AMB-ELE01");
        dto.setLatitude(75.0); // Arctic latitude
        dto.setLongitude(36.85);

        assertThrows(IllegalArgumentException.class, () -> {
            telemetryService.processTelemetry(dto);
        });
    }

    @Test
    @DisplayName("Process valid telemetry persists coordinates, timestamp, and dataSource")
    void testProcessValidTelemetry() {
        TelemetryRequestDto dto = new TelemetryRequestDto();
        dto.setCollarId("KWS-AMB-ELE01");
        dto.setLatitude(-2.665);
        dto.setLongitude(37.265);
        dto.setTimestamp(LocalDateTime.now());

        long beforeCount = telemetryRepository.count();
        telemetryService.processTelemetry(dto);

        assertEquals(beforeCount + 1, telemetryRepository.count());

        Animal animal = animalRepository.findByCollarId("KWS-AMB-ELE01").orElseThrow();
        var latest = telemetryRepository.findTop1ByAnimal_IdOrderByTimestampDesc(animal.getId()).orElseThrow();

        assertEquals(-2.665, latest.getLatitude(), 0.001);
        assertEquals(37.265, latest.getLongitude(), 0.001);
        assertEquals("LIVE", latest.getDataSource());
        assertNotNull(latest.getLocation());
        assertEquals(4326, latest.getLocation().getSRID());
    }

    @Test
    @DisplayName("Animal trail query returns chronological breadcrumb fixes")
    void testAnimalTrailRetrieval() {
        List<TelemetryTrailDto> trail = telemetryService.getAnimalTrail("KWS-AMB-ELE01");
        assertNotNull(trail);
        // Trail should be non-empty after seed/telemetry steps
        if (trail.size() >= 2) {
            // Verify chronologically non-decreasing
            for (int i = 0; i < trail.size() - 1; i++) {
                assertFalse(trail.get(i).getTimestamp().isAfter(trail.get(i + 1).getTimestamp()),
                        "Trail points must be in chronological ascending order");
            }
        }
    }

    @Test
    @DisplayName("Animal CRUD persists changes in PostgreSQL and retrieves after reload")
    void testAnimalCrudPersistence() {
        AnimalDto newAnimal = new AnimalDto();
        newAnimal.setName("Serengeti Roamer");
        newAnimal.setSpecies("African Lion");
        newAnimal.setCollarId("KWS-TEST-LIO99");
        newAnimal.setParkName("Maasai Mara");
        newAnimal.setSex("Male");

        AnimalDto saved = animalService.registerAnimal(newAnimal, "commander");
        assertNotNull(saved.getId());

        // Verify retrieval
        Animal reloaded = animalRepository.findByCollarId("KWS-TEST-LIO99").orElseThrow();
        assertEquals("Serengeti Roamer", reloaded.getName());
        assertEquals("VU", reloaded.getIucnStatus());

        // Update animal
        saved.setName("Serengeti Pride King");
        AnimalDto updated = animalService.updateAnimal(saved.getId(), saved, "commander");
        assertEquals("Serengeti Pride King", updated.getName());

        // Retire collar
        animalService.retireCollar("KWS-TEST-LIO99", "commander");
        Animal retired = animalRepository.findByCollarId("KWS-TEST-LIO99").orElseThrow();
        assertFalse(retired.getActive(), "Retired animal must have active=false");
    }
}
