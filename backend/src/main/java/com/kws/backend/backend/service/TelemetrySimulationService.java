package com.kws.backend.backend.service;

import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.GeofenceRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetrySimulationService {

    private final AnimalRepository animalRepository;
    private final TelemetryRepository telemetryRepository;
    private final GeofenceRepository geofenceRepository;
    private final IncidentService incidentService;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final Random random = new Random();

    private boolean simulationActive = true;
    private final Map<Long, Integer> stationaryPingsCount = new ConcurrentHashMap<>();
    private final Map<Long, Double> animalBatteries = new ConcurrentHashMap<>();

    public boolean isSimulationActive() {
        return simulationActive;
    }

    public void setSimulationActive(boolean active) {
        this.simulationActive = active;
        log.info("Telemetry simulation active status set to: {}", active);
    }

    @Scheduled(fixedRate = 25000, initialDelay = 10000)
    public void runScheduledSimulation() {
        if (!simulationActive) {
            return;
        }
        simulateStep();
    }

    public int simulateStep() {
        List<Animal> animals = animalRepository.findAll();
        if (animals.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Animal animal : animals) {
            var latestOpt = telemetryRepository.findTop1ByAnimalIdOrderByTimestampDesc(animal.getId());

            double curLat;
            double curLng;

            if (latestOpt.isPresent() && latestOpt.get().getLocation() != null) {
                curLat = latestOpt.get().getLocation().getY();
                curLng = latestOpt.get().getLocation().getX();
            } else {
                // Initial baseline fallback coordinates based on collar prefix or id
                double[] baseline = getBaselineCoordinate(animal.getCollarId());
                curLat = baseline[0];
                curLng = baseline[1];
            }

            // Calculate realistic subtle GPS displacement (approx 50m - 150m walk)
            double latShift = (random.nextDouble() - 0.48) * 0.0020;
            double lngShift = (random.nextDouble() - 0.48) * 0.0020;

            // Scenario calibration: Keep Mutula Bull hovering right at the Amboseli/Kimana corridor
            if ("KWS-AMB-ELE04".equalsIgnoreCase(animal.getCollarId())) {
                latShift = (random.nextDouble() - 0.5) * 0.0015;
                lngShift = (random.nextDouble() - 0.5) * 0.0015;
            }

            double nextLat = curLat + latShift;
            double nextLng = curLng + lngShift;

            Point newPoint = geometryFactory.createPoint(new Coordinate(nextLng, nextLat));
            newPoint.setSRID(4326);

            TelemetryLocation newLoc = new TelemetryLocation();
            newLoc.setAnimal(animal);
            newLoc.setLocation(newPoint);
            newLoc.setTimestamp(LocalDateTime.now());
            telemetryRepository.save(newLoc);

            // Battery drain simulation (98% down gradually)
            double currentBattery = animalBatteries.getOrDefault(animal.getId(), 94.0);
            currentBattery = Math.max(12.0, currentBattery - 0.05);
            animalBatteries.put(animal.getId(), currentBattery);

            // Spatial PostGIS Geofence Breach check
            evaluateBreaches(animal, nextLat, nextLng);
            updatedCount++;
        }

        log.info("Simulated telemetry update completed for {} animals across Kenya.", updatedCount);
        return updatedCount;
    }

    private void evaluateBreaches(Animal animal, double lat, double lng) {
        List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(lng, lat);
        boolean inCommunityBuffer = false;
        String bufferZoneName = "";

        for (GeofenceZone zone : zones) {
            if ("COMMUNITY_BUFFER".equalsIgnoreCase(zone.getZoneType())) {
                inCommunityBuffer = true;
                bufferZoneName = zone.getZoneName();
                break;
            }
        }

        if (inCommunityBuffer) {
            log.warn("HUMAN-WILDLIFE CONFLICT: {} ({}) detected in community buffer: {} [{}, {}]",
                    animal.getName(), animal.getCollarId(), bufferZoneName, lat, lng);
            incidentService.recordIncident(
                    animal.getName(),
                    animal.getCollarId(),
                    bufferZoneName,
                    "CRITICAL",
                    String.format("%s '%s' (%s) entered community dispersal buffer %s at [%.4f, %.4f]. Rapid patrol response recommended.",
                            animal.getSpecies(), animal.getName(), animal.getCollarId(), bufferZoneName, lat, lng)
            );
        }
    }

    public double getBattery(Long animalId) {
        return animalBatteries.getOrDefault(animalId, 92.0);
    }

    private double[] getBaselineCoordinate(String collarId) {
        if (collarId == null) return new double[]{-2.6531, 37.2625};
        String id = collarId.toUpperCase();
        if (id.contains("AMB")) return new double[]{-2.6531, 37.2625}; // Amboseli
        if (id.contains("TSV")) return new double[]{-2.8124, 38.6210}; // Tsavo East
        if (id.contains("MAR")) return new double[]{-1.4850, 35.1280}; // Maasai Mara
        if (id.contains("NBI")) return new double[]{-1.3780, 36.8420}; // Nairobi
        if (id.contains("OLP")) return new double[]{0.0420, 36.9450};  // Ol Pejeta
        return new double[]{-2.6531, 37.2625};
    }
}
