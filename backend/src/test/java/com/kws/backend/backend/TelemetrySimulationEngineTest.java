package com.kws.backend.backend;

import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import com.kws.backend.backend.service.TelemetrySimulationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TelemetrySimulationEngineTest {

    @Autowired
    private TelemetrySimulationService simulationService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Test
    @DisplayName("Simulation state and speed multipliers can be adjusted dynamically")
    void testSimulationControls() {
        simulationService.setSimulationActive(false);
        assertFalse(simulationService.isSimulationActive());

        simulationService.setSimulationActive(true);
        assertTrue(simulationService.isSimulationActive());

        simulationService.setSimulationSpeedMultiplier(2.0);
        assertEquals(2.0, simulationService.getSimulationSpeedMultiplier(), 0.01);

        simulationService.setSimulationSpeedMultiplier(4.0);
        assertEquals(4.0, simulationService.getSimulationSpeedMultiplier(), 0.01);

        // Clamping between 0.5 and 5.0
        simulationService.setSimulationSpeedMultiplier(10.0);
        assertEquals(5.0, simulationService.getSimulationSpeedMultiplier(), 0.01);
    }

    @Test
    @DisplayName("Nairobi NP Northern Electric Fence hard limit pushes animals south")
    void testNairobiNorthernFenceConstraint() {
        // Find a Nairobi NP animal (e.g. Mukurwe Black Rhino or Simba Nairobi)
        List<Animal> nairobiAnimals = animalRepository.findByParkNameIgnoreCase("Nairobi NP");
        assertFalse(nairobiAnimals.isEmpty(), "Nairobi NP animals must exist in registry");

        // Execute multiple simulation ticks
        for (int i = 0; i < 5; i++) {
            simulationService.simulateStep();
        }

        // Verify that all Nairobi NP telemetry records strictly remain south of the electric fence line (-1.3430°S)
        for (Animal a : nairobiAnimals) {
            var latestOpt = telemetryRepository.findTop1ByAnimal_IdOrderByTimestampDesc(a.getId());
            if (latestOpt.isPresent()) {
                TelemetryLocation loc = latestOpt.get();
                double lat = loc.getLatitude();
                assertTrue(lat <= -1.3430,
                        String.format("Animal %s breached northern electric fence! Latitude: %.4f (must be <= -1.3430)", a.getName(), lat));
            }
        }
    }

    @Test
    @DisplayName("Simulation step updates telemetry records and speeds correctly")
    void testSimulationStepUpdates() {
        long beforeCount = telemetryRepository.count();
        int updated = simulationService.simulateStep();

        assertTrue(updated > 0, "Simulation step must update collared animals");
        long afterCount = telemetryRepository.count();
        assertTrue(afterCount >= beforeCount + updated, "New telemetry fixes must be persisted in database");
    }
}
