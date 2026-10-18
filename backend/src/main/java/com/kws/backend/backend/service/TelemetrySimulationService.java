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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Continuous Background Animal Movement Simulation Service.
 * Implements realistic movement vector drifts, species-specific speeds and habits,
 * waypoint circuit navigation, and robust boundary containment within Kenyan park geofences.
 */
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

    private volatile boolean simulationActive = true;
    private volatile double simulationSpeedMultiplier = 1.0;

    // In-memory runtime telemetry telemetry states per animal
    private final Map<Long, Double> animalBatteries = new ConcurrentHashMap<>();
    private final Map<String, Double> animalHeadings = new ConcurrentHashMap<>();
    private final Map<String, Integer> animalWaypointIndices = new ConcurrentHashMap<>();
    private final Map<String, Integer> animalRestCounters = new ConcurrentHashMap<>();
    private final Map<String, Double> animalSpeeds = new ConcurrentHashMap<>();
    private final Map<String, String> animalBehaviors = new ConcurrentHashMap<>();
    private final Map<String, String> animalFenceDistances = new ConcurrentHashMap<>();
    private final Map<String, String> animalCorridors = new ConcurrentHashMap<>();

    public record Waypoint(double lat, double lng, String name) {}

    // ── 13 Kenyan Parks Waypoint Circuits ─────────────────────────────────────
    private static final Map<String, List<Waypoint>> PARK_WAYPOINTS = new HashMap<>();

    static {
        PARK_WAYPOINTS.put("Amboseli NP", List.of(
                new Waypoint(-2.6650, 37.2650, "Enkongo Narok Swamp"),
                new Waypoint(-2.6500, 37.3100, "Ol Tukai Springs"),
                new Waypoint(-2.6780, 37.2400, "Observation Hill"),
                new Waypoint(-2.6300, 37.2200, "Amboseli Basin")
        ));

        PARK_WAYPOINTS.put("Tsavo East NP", List.of(
                new Waypoint(-2.8124, 38.6210, "Galana River Rapids"),
                new Waypoint(-3.1150, 38.8250, "Aruba Dam"),
                new Waypoint(-3.1800, 38.5200, "Mudanda Rock"),
                new Waypoint(-3.1250, 38.5410, "Satao Waterhole")
        ));

        PARK_WAYPOINTS.put("Tsavo West NP", List.of(
                new Waypoint(-3.3200, 38.0000, "Ngulia Rhino Sanctuary"),
                new Waypoint(-2.9850, 37.9250, "Mzima Springs Oasis"),
                new Waypoint(-3.0500, 37.9800, "Shetani Lava Flow"),
                new Waypoint(-3.5800, 37.7500, "Lake Jipe Wetlands")
        ));

        PARK_WAYPOINTS.put("Maasai Mara", List.of(
                new Waypoint(-1.4420, 35.2150, "Talek River Plains"),
                new Waypoint(-1.5100, 35.0350, "Mara River Crossing"),
                new Waypoint(-1.3900, 35.1100, "Musiara Marsh"),
                new Waypoint(-1.4850, 35.1280, "Rhino Ridge")
        ));

        PARK_WAYPOINTS.put("Nairobi NP", List.of(
                new Waypoint(-1.3850, 36.8720, "Hippo Pools & Mbagathi River"),
                new Waypoint(-1.3820, 36.8250, "Nagolomon Dam"),
                new Waypoint(-1.3650, 36.8400, "Hyena Dam Plains"),
                new Waypoint(-1.4050, 36.8900, "Athi River Basin"),
                new Waypoint(-1.3700, 36.8000, "Central Rhino Sanctuary")
        ));

        PARK_WAYPOINTS.put("Ol Pejeta", List.of(
                new Waypoint(0.0420, 36.9450, "Sweetwaters Plains"),
                new Waypoint(0.0250, 36.9200, "Ewaso Nyiro Riverbed"),
                new Waypoint(0.0600, 36.9700, "Pelican Dam"),
                new Waypoint(0.0150, 36.9600, "Morani Rhino Sanctuary")
        ));

        PARK_WAYPOINTS.put("Lake Nakuru", List.of(
                new Waypoint(-0.3550, 36.0850, "Baboon Cliff Lookout"),
                new Waypoint(-0.3800, 36.0750, "Lake Nakuru Shoreline"),
                new Waypoint(-0.4150, 36.0950, "Makalia Falls Basin"),
                new Waypoint(-0.3600, 36.1100, "Sarova Lion Hill Ridge")
        ));

        PARK_WAYPOINTS.put("Samburu", List.of(
                new Waypoint(0.6250, 37.5350, "Ewaso Nyiro Riverbed"),
                new Waypoint(0.6500, 37.5100, "Elephant Bedroom Camp"),
                new Waypoint(0.5950, 37.5600, "Buffalo Springs Crossing"),
                new Waypoint(0.6400, 37.5750, "Koitogor Hill")
        ));

        PARK_WAYPOINTS.put("Aberdare", List.of(
                new Waypoint(-0.4200, 36.7050, "Karuru Falls Moorland"),
                new Waypoint(-0.3800, 36.7350, "Chania River Gorge"),
                new Waypoint(-0.4850, 36.6800, "Kinangop Peak Foothills"),
                new Waypoint(-0.3500, 36.8500, "Treetops Salient Corridor")
        ));

        PARK_WAYPOINTS.put("Mount Kenya", List.of(
                new Waypoint(-0.1500, 37.3100, "Sirimon Track Valley"),
                new Waypoint(-0.1800, 37.2800, "Mackinder's Valley Marsh"),
                new Waypoint(-0.1650, 37.3600, "Chogoria Gorges Ridge"),
                new Waypoint(-0.2200, 37.2500, "Ragati Montane Forest")
        ));

        PARK_WAYPOINTS.put("Lewa", List.of(
                new Waypoint(0.2100, 37.4550, "Lewa Swamp Sanctuary"),
                new Waypoint(0.1850, 37.4350, "Sirikoi Riverbank"),
                new Waypoint(0.2350, 37.4700, "Lewa Northern Savannah"),
                new Waypoint(0.1900, 37.4800, "Ngare Ndare Forest Fringe")
        ));

        PARK_WAYPOINTS.put("Saiwa Swamp", List.of(
                new Waypoint(1.0920, 35.1220, "Kipsaina Wetland Boardwalk"),
                new Waypoint(1.0960, 35.1240, "Central Sitatunga Reeds"),
                new Waypoint(1.0880, 35.1200, "Treehouse Viewing Marsh")
        ));

        PARK_WAYPOINTS.put("Mount Elgon", List.of(
                new Waypoint(1.1350, 34.5850, "Kitum Cave Elephant Path"),
                new Waypoint(1.1200, 34.5600, "Makingeny Salt Cave"),
                new Waypoint(1.1550, 34.6100, "Chepnyalil Moorland"),
                new Waypoint(1.1100, 34.6200, "Endebess Bluff")
        ));
    }

    // ── Park Bounding Boxes for Fallback Clamping [minLat, maxLat, minLng, maxLng] ───
    private static final Map<String, double[]> PARK_BOUNDS = new HashMap<>();

    static {
        PARK_BOUNDS.put("Amboseli NP", new double[]{-2.72, -2.58, 37.18, 37.36});
        PARK_BOUNDS.put("Tsavo East NP", new double[]{-3.30, -2.40, 38.30, 39.00});
        PARK_BOUNDS.put("Tsavo West NP", new double[]{-3.60, -2.95, 37.70, 38.25});
        PARK_BOUNDS.put("Maasai Mara", new double[]{-1.62, -1.36, 34.98, 35.34});
        PARK_BOUNDS.put("Nairobi NP", new double[]{-1.44, -1.3430, 36.76, 36.95}); // Northern city fence at -1.3430
        PARK_BOUNDS.put("Ol Pejeta", new double[]{0.00, 0.10, 36.88, 37.05});
        PARK_BOUNDS.put("Lake Nakuru", new double[]{-0.44, -0.32, 36.05, 36.14});
        PARK_BOUNDS.put("Samburu", new double[]{0.56, 0.68, 37.48, 37.60});
        PARK_BOUNDS.put("Aberdare", new double[]{-0.52, -0.32, 36.65, 36.90});
        PARK_BOUNDS.put("Mount Kenya", new double[]{-0.26, -0.10, 37.20, 37.42});
        PARK_BOUNDS.put("Lewa", new double[]{0.15, 0.26, 37.40, 37.52});
        PARK_BOUNDS.put("Saiwa Swamp", new double[]{1.07, 1.11, 35.10, 35.14});
        PARK_BOUNDS.put("Mount Elgon", new double[]{1.08, 1.22, 34.50, 34.68});
    }

    public boolean isSimulationActive() {
        return simulationActive;
    }

    public void setSimulationActive(boolean active) {
        this.simulationActive = active;
        log.info("Continuous background telemetry simulation set to: {}", active);
    }

    public double getSimulationSpeedMultiplier() {
        return simulationSpeedMultiplier;
    }

    public void setSimulationSpeedMultiplier(double mult) {
        this.simulationSpeedMultiplier = Math.max(0.5, Math.min(5.0, mult));
    }

    /**
     * Automated scheduled background simulation loop.
     * Executes every 12 seconds with realistic movement vector drift.
     */
    @Scheduled(fixedRate = 12000, initialDelay = 5000)
    public void runScheduledSimulation() {
        if (!simulationActive) {
            return;
        }
        simulateStep();
    }

    /**
     * Performs one simulation tick across all collared animals.
     * Calculates realistic drift, species behavior, and park geofence containment.
     */
    public synchronized int simulateStep() {
        List<Animal> animals = animalRepository.findAll();
        if (animals.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Animal animal : animals) {
            applyMovementStep(animal);
            updatedCount++;
        }

        log.debug("Simulated telemetry vector update completed for {} animals across Kenya.", updatedCount);
        return updatedCount;
    }

    private void applyMovementStep(Animal animal) {
        String park = normalizeParkName(animal.getParkName(), animal.getCollarId());
        String collarId = animal.getCollarId();
        String species = animal.getSpecies() != null ? animal.getSpecies() : "Unknown";

        // Current coordinates
        double curLat;
        double curLng;
        var latestOpt = telemetryRepository.findTop1ByAnimal_IdOrderByTimestampDesc(animal.getId());
        if (latestOpt.isPresent() && latestOpt.get().getLocation() != null) {
            curLat = latestOpt.get().getLocation().getY();
            curLng = latestOpt.get().getLocation().getX();
        } else {
            double[] baseline = getBaselineCoordinate(park, collarId);
            curLat = baseline[0];
            curLng = baseline[1];
        }

        // Species resting behavior (Lions rest periodically)
        boolean isLion = species.toLowerCase().contains("lion");
        if (isLion) {
            int rest = animalRestCounters.getOrDefault(collarId, 0);
            if (rest > 0) {
                animalRestCounters.put(collarId, rest - 1);
                animalSpeeds.put(collarId, 0.0);
                animalBehaviors.put(collarId, "💤 Resting in Savannah Shade");
                animalFenceDistances.put(collarId, calculateFenceStatus(curLat, curLng, park));
                recordTelemetry(animal, curLat, curLng);
                return;
            }
            if (random.nextDouble() < 0.15) {
                animalRestCounters.put(collarId, random.nextInt(3) + 3);
                animalSpeeds.put(collarId, 0.0);
                animalBehaviors.put(collarId, "💤 Resting in Acacia Canopy");
                animalFenceDistances.put(collarId, calculateFenceStatus(curLat, curLng, park));
                recordTelemetry(animal, curLat, curLng);
                return;
            }
        }

        // Select park waypoint circuit
        List<Waypoint> waypoints = PARK_WAYPOINTS.getOrDefault(park, PARK_WAYPOINTS.get("Amboseli NP"));
        int currentWpIdx = animalWaypointIndices.getOrDefault(collarId, 0);
        if (currentWpIdx >= waypoints.size()) {
            currentWpIdx = 0;
        }

        Waypoint targetWp = waypoints.get(currentWpIdx);
        double distToWp = distanceKm(curLat, curLng, targetWp.lat(), targetWp.lng());

        // Arrived within 400m of waypoint -> advance circuit
        if (distToWp < 0.40) {
            currentWpIdx = (currentWpIdx + 1) % waypoints.size();
            animalWaypointIndices.put(collarId, currentWpIdx);
            targetWp = waypoints.get(currentWpIdx);
        }

        // Heading calculation towards target waypoint
        double dLat = targetWp.lat() - curLat;
        double dLng = (targetWp.lng() - curLng) * Math.cos(Math.toRadians(curLat));
        double desiredHeading = Math.atan2(dLng, dLat);

        double currentHeading = animalHeadings.getOrDefault(collarId, desiredHeading);
        double angleDiff = Math.atan2(Math.sin(desiredHeading - currentHeading), Math.cos(desiredHeading - currentHeading));
        currentHeading += angleDiff * 0.28; // Smooth 28% turn toward target
        currentHeading += (random.nextDouble() - 0.5) * 0.16; // Natural animal wander jitter

        // Nairobi NP Electric Fence Avoidance: Repel south if nearing city northern border
        if ("Nairobi NP".equalsIgnoreCase(park) && curLat > -1.3520) {
            double southHeading = Math.PI * 0.65; // Repel southeast towards Mbagathi River
            double fenceDiff = Math.atan2(Math.sin(southHeading - currentHeading), Math.cos(southHeading - currentHeading));
            currentHeading += fenceDiff * 0.65;
        }

        animalHeadings.put(collarId, currentHeading);

        // Species-specific speed & step drift
        double baseSpeed = resolveRealisticSpeed(species);
        double currentSpeed = baseSpeed * (0.85 + random.nextDouble() * 0.30);
        animalSpeeds.put(collarId, currentSpeed);

        double stepLength = resolveStepLength(species) * 0.45 * simulationSpeedMultiplier;
        double nextLat = curLat + Math.cos(currentHeading) * stepLength;
        double nextLng = curLng + Math.sin(currentHeading) * stepLength / Math.max(0.1, Math.cos(Math.toRadians(curLat)));

        // Boundary containment validation
        double[] bounds = PARK_BOUNDS.getOrDefault(park, new double[]{-4.0, 4.0, 34.0, 41.0});
        boolean inside = (nextLat >= bounds[0] && nextLat <= bounds[1] && nextLng >= bounds[2] && nextLng <= bounds[3]);

        // Hard fence limit for Nairobi NP: never cross north of -1.3430
        if ("Nairobi NP".equalsIgnoreCase(park) && nextLat > -1.3430) {
            inside = false;
        }

        if (inside) {
            curLat = nextLat;
            curLng = nextLng;
        } else {
            // Rebound heading towards waypoint/center
            currentHeading = currentHeading + Math.PI;
            animalHeadings.put(collarId, currentHeading);
            curLat = curLat + (targetWp.lat() - curLat) * 0.10;
            curLng = curLng + (targetWp.lng() - curLng) * 0.10;
        }

        // Telemetry telemetry attributes
        animalBehaviors.put(collarId, resolveBehaviorState(species, targetWp.name(), distToWp));
        animalFenceDistances.put(collarId, calculateFenceStatus(curLat, curLng, park));
        int etaMin = Math.max(5, (int)(distToWp / (Math.max(0.5, currentSpeed) / 60.0)));
        animalCorridors.put(collarId, String.format("Corridor toward %s (ETA ~%d min)", targetWp.name(), etaMin));

        // Battery degradation & solar charging
        double battery = animalBatteries.getOrDefault(animal.getId(), 94.0);
        battery = Math.max(15.0, battery - 0.02);
        animalBatteries.put(animal.getId(), battery);

        recordTelemetry(animal, curLat, curLng);

        // Geofence buffer breach check
        evaluateBreaches(animal, curLat, curLng);
    }

    private void recordTelemetry(Animal animal, double lat, double lng) {
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);

        TelemetryLocation loc = new TelemetryLocation();
        loc.setAnimal(animal);
        loc.setLocation(point);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setSpeedKmh(animalSpeeds.getOrDefault(animal.getCollarId(), 0.0));
        loc.setHeading(animalHeadings.getOrDefault(animal.getCollarId(), 0.0));
        loc.setBatteryLevel(animalBatteries.getOrDefault(animal.getId(), 100.0));
        loc.setActivityState(animalBehaviors.getOrDefault(animal.getCollarId(), "ACTIVE"));
        loc.setDataSource("SIMULATED");
        loc.setTimestamp(LocalDateTime.now());
        loc.setCreatedAt(LocalDateTime.now());
        telemetryRepository.save(loc);
    }

    private void evaluateBreaches(Animal animal, double lat, double lng) {
        try {
            List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(lng, lat);
            for (GeofenceZone zone : zones) {
                if ("COMMUNITY_BUFFER".equalsIgnoreCase(zone.getZoneType()) || "HIGH_CONFLICT".equalsIgnoreCase(zone.getZoneType())) {
                    log.warn("⚠️ PERIMETER BREACH: {} ({}) in buffer zone: {}", animal.getName(), animal.getCollarId(), zone.getZoneName());
                    incidentService.recordIncident(
                            animal.getName(),
                            animal.getCollarId(),
                            zone.getZoneName(),
                            "CRITICAL",
                            String.format("%s '%s' (%s) entered %s at [%.4f, %.4f]. Rapid patrol response active.",
                                    animal.getSpecies(), animal.getName(), animal.getCollarId(), zone.getZoneName(), lat, lng),
                            animal.getParkName(),
                            lat,
                            lng
                    );
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Species & Speed Resolvers ──────────────────────────────────────────

    private double resolveRealisticSpeed(String species) {
        String s = species.toLowerCase();
        if (s.contains("eagle") || s.contains("raptor")) return 18.5; // Soaring raptor
        if (s.contains("secretary")) return 4.2; // Terrestrial bird
        if (s.contains("cheetah")) return 8.2;
        if (s.contains("lion")) return 4.2;
        if (s.contains("elephant")) return 3.2;
        if (s.contains("zebra")) return 4.0;
        if (s.contains("rhino")) return 2.4;
        if (s.contains("bongo")) return 2.0;
        if (s.contains("sitatunga")) return 1.6;
        if (s.contains("giraffe")) return 3.6;
        return 3.0;
    }

    private double resolveStepLength(String species) {
        String s = species.toLowerCase();
        if (s.contains("eagle")) return 0.0055;
        if (s.contains("cheetah")) return 0.0030;
        if (s.contains("lion")) return 0.0022;
        if (s.contains("elephant")) return 0.0018;
        if (s.contains("rhino") || s.contains("bongo")) return 0.0013;
        if (s.contains("sitatunga")) return 0.0010;
        return 0.0016;
    }

    private String resolveBehaviorState(String species, String targetWpName, double distKm) {
        String s = species.toLowerCase();
        if (distKm < 0.5) return "💧 Watering near " + targetWpName;
        if (s.contains("eagle")) return "🦅 High Thermal Soaring over " + targetWpName;
        if (s.contains("secretary")) return "🐍 Terrestrial Stalking near " + targetWpName;
        if (s.contains("rhino")) return "🌿 Browsing Acacia Bushland";
        if (s.contains("bongo")) return "🍃 Montane Bamboo Browsing";
        if (s.contains("sitatunga")) return "🌾 Wetland Reed Navigation";
        if (s.contains("elephant")) return "🐘 Family Corridor Foraging";
        if (s.contains("lion")) return "🐾 Pride Patrol towards " + targetWpName;
        if (s.contains("cheetah")) return "🐆 Open Savannah Scan";
        if (s.contains("zebra")) return "🦓 Herd Grazing Formation";
        if (s.contains("giraffe")) return "🦒 Acacia Canopy Browsing";
        return "🌿 Grazing in Protected Sanctuary";
    }

    private String calculateFenceStatus(double lat, double lng, String park) {
        if ("Nairobi NP".equalsIgnoreCase(park)) {
            double distKm = Math.max(0.05, (-1.3430 - lat) * 111.0);
            if (distKm < 0.6) {
                return String.format("⚡ Near Northern City Fence (%.0fm)", distKm * 1000.0);
            }
            return String.format("🛡️ Safe in Sanctuary (%.1f km from City Fence)", distKm);
        }
        return "🛡️ Safe within Park Perimeter";
    }

    public String normalizeParkName(String parkName, String collarId) {
        if (parkName != null && !parkName.isBlank()) {
            return parkName;
        }
        if (collarId == null) return "Amboseli NP";
        String id = collarId.toUpperCase();
        if (id.contains("AMB")) return "Amboseli NP";
        if (id.contains("TSV") || id.contains("TSE")) return "Tsavo East NP";
        if (id.contains("TSW")) return "Tsavo West NP";
        if (id.contains("MAR")) return "Maasai Mara";
        if (id.contains("NBI")) return "Nairobi NP";
        if (id.contains("OLP")) return "Ol Pejeta";
        if (id.contains("NAK")) return "Lake Nakuru";
        if (id.contains("SAM")) return "Samburu";
        if (id.contains("ABR")) return "Aberdare";
        if (id.contains("MTK")) return "Mount Kenya";
        if (id.contains("LEW")) return "Lewa";
        if (id.contains("SAI")) return "Saiwa Swamp";
        if (id.contains("ELG")) return "Mount Elgon";
        return "Amboseli NP";
    }

    public double[] getBaselineCoordinate(String park, String collarId) {
        List<Waypoint> wps = PARK_WAYPOINTS.get(park);
        if (wps != null && !wps.isEmpty()) {
            return new double[]{wps.get(0).lat(), wps.get(0).lng()};
        }
        return new double[]{-2.6531, 37.2625};
    }

    public double getBattery(Long animalId) {
        return animalBatteries.getOrDefault(animalId, 94.0);
    }

    public double getSpeed(String collarId) {
        return animalSpeeds.getOrDefault(collarId, 2.8);
    }

    public String getHeadingCompass(String collarId) {
        double heading = animalHeadings.getOrDefault(collarId, 0.0);
        double deg = (Math.toDegrees(heading) % 360.0 + 360.0) % 360.0;
        String[] cardinals = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int idx = (int) Math.round(deg / 22.5) % 16;
        return String.format("%s %.0f°", cardinals[idx], deg);
    }

    public String getBehavior(String collarId) {
        return animalBehaviors.getOrDefault(collarId, "🌿 Foraging in Sanctuary");
    }

    public String getFenceDistance(String collarId) {
        return animalFenceDistances.getOrDefault(collarId, "🛡️ Safe in Sanctuary");
    }

    public String getCorridor(String collarId) {
        return animalCorridors.getOrDefault(collarId, "Protected Park Corridor");
    }

    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}
