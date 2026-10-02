package com.kws.backend.backend.config;

import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.model.ParkProfile;
import com.kws.backend.backend.model.PatrolUnit;
import com.kws.backend.backend.repository.AnimalRepository;
import com.kws.backend.backend.repository.GeofenceRepository;
import com.kws.backend.backend.repository.ParkProfileRepository;
import com.kws.backend.backend.repository.PatrolRepository;
import com.kws.backend.backend.repository.TelemetryRepository;
import com.kws.backend.backend.service.TelemetrySimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final GeofenceRepository geofenceRepository;
    private final ParkProfileRepository parkProfileRepository;
    private final AnimalRepository animalRepository;
    private final PatrolRepository patrolRepository;
    private final TelemetryRepository telemetryRepository;
    private final TelemetrySimulationService telemetrySimulationService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public void run(String... args) {
        seedGeofences();
        seedParkProfiles();
        seedAnimals();
        seedPatrolUnits();
        seedInitialTelemetry();
    }

    private void seedInitialTelemetry() {
        if (telemetryRepository.count() == 0) {
            log.info("Generating initial GPS telemetry fixes for collared wildlife...");
            telemetrySimulationService.simulateStep();
        }
    }

    private void seedGeofences() {
        if (geofenceRepository.count() > 0) {
            return;
        }

        log.info("Initializing Kenya National Parks and Conservancies geofence boundaries...");

        // Amboseli National Park
        Polygon amboseliPoly = createPolygon(new Coordinate[]{
                new Coordinate(37.18, -2.58),
                new Coordinate(37.35, -2.58),
                new Coordinate(37.35, -2.72),
                new Coordinate(37.18, -2.72),
                new Coordinate(37.18, -2.58)
        });
        GeofenceZone amboseli = new GeofenceZone(null, "Amboseli National Park", "PARK", amboseliPoly);

        // Kimana Community Dispersal Area (Buffer)
        Polygon kimanaPoly = createPolygon(new Coordinate[]{
                new Coordinate(37.35, -2.68),
                new Coordinate(37.45, -2.68),
                new Coordinate(37.45, -2.75),
                new Coordinate(37.35, -2.75),
                new Coordinate(37.35, -2.68)
        });
        GeofenceZone kimana = new GeofenceZone(null, "Kimana Community Wildlife Corridor", "COMMUNITY_BUFFER", kimanaPoly);

        // Nairobi National Park
        Polygon nairobiPoly = createPolygon(new Coordinate[]{
                new Coordinate(36.80, -1.32),
                new Coordinate(36.92, -1.32),
                new Coordinate(36.92, -1.42),
                new Coordinate(36.80, -1.42),
                new Coordinate(36.80, -1.32)
        });
        GeofenceZone nairobi = new GeofenceZone(null, "Nairobi National Park", "PARK", nairobiPoly);

        // Maasai Mara National Reserve
        Polygon maraPoly = createPolygon(new Coordinate[]{
                new Coordinate(34.98, -1.38),
                new Coordinate(35.32, -1.38),
                new Coordinate(35.32, -1.62),
                new Coordinate(34.98, -1.62),
                new Coordinate(34.98, -1.38)
        });
        GeofenceZone mara = new GeofenceZone(null, "Maasai Mara National Reserve", "PARK", maraPoly);

        // Tsavo East National Park
        Polygon tsavoPoly = createPolygon(new Coordinate[]{
                new Coordinate(38.30, -2.40),
                new Coordinate(39.00, -2.40),
                new Coordinate(39.00, -3.30),
                new Coordinate(38.30, -3.30),
                new Coordinate(38.30, -2.40)
        });
        GeofenceZone tsavo = new GeofenceZone(null, "Tsavo East National Park", "PARK", tsavoPoly);

        geofenceRepository.saveAll(List.of(amboseli, kimana, nairobi, mara, tsavo));
        log.info("Successfully seeded Kenyan park boundaries.");
    }

    private void seedParkProfiles() {
        if (parkProfileRepository.count() > 0) {
            return;
        }

        log.info("Initializing Kenya National Parks and Conservancies operational profiles...");

        ParkProfile amboseli = new ParkProfile(
                null,
                "Amboseli National Park",
                "AMBOSELI",
                "Kajiado County",
                392.0,
                "Savannah Plains, Acacia Woodland & Freshwater Swamps",
                1974,
                "Ol Tukai Ranger Command Post",
                "Partially Fenced (Southern Kimana Corridor Buffer)",
                "MODERATE - Human-Wildlife Conflict Buffer",
                "Enkongo Narok Swamp, Ol Okenya Marsh, Lake Amboseli Basin, Observation Hill Waterhole",
                "African Bush Elephant, Lion, Cheetah, Maasai Giraffe, Cape Buffalo, Spotted Hyena",
                "Famous for free-ranging elephants against Mount Kilimanjaro. Critical focus is the southern Kimana community corridor to prevent crop-raiding.",
                -2.65,
                37.26,
                11
        );

        ParkProfile tsavoEast = new ParkProfile(
                null,
                "Tsavo East National Park",
                "TSAVO_EAST",
                "Taita-Taveta, Kitui & Tana River Counties",
                13747.0,
                "Semi-Arid Bushland, Savannah & Riverine Galana Basin",
                1948,
                "Voi Gate Headquarters & Lugard Falls Patrol Post",
                "Open Dispersal (SGR Wildlife Underpasses & Highway Crossings)",
                "HIGH - Vast Area Poaching & SGR Dispersal Risk",
                "Aruba Dam, Galana River Rapids, Mudanda Rock Water catchment, Lugard Falls Pool",
                "Red Dust Elephants, Tsavo Maneless Lions, Black Rhino, Hirola, Lesser Kudu",
                "One of the largest national parks in Kenya. Renowned for dust-red elephants and wildlife movement across the Standard Gauge Railway corridor.",
                -2.77,
                38.77,
                10
        );

        ParkProfile tsavoWest = new ParkProfile(
                null,
                "Tsavo West National Park",
                "TSAVO_WEST",
                "Taita-Taveta County",
                9065.0,
                "Rugged Volcanic Ridges, Mzima Springs & Acacia Savannah",
                1948,
                "Ngulia Rhino Sanctuary Command Post",
                "Specialized Rhino Sanctuary Electric Grid + Open Park",
                "CRITICAL - Intensive Rhino Anti-Poaching Sanctuary",
                "Mzima Springs Natural Oasis, Lake Jipe Wetlands, Ngulia Water Pan, Shetani Lava Pools",
                "Eastern Black Rhino, African Leopard, Hippopotamus, Crocodile, Wild Dog",
                "Characterized by rugged mountainous landscapes, Shetani lava flow, and Mzima Springs with an intensive rhino sanctuary.",
                -3.32,
                38.00,
                10
        );

        ParkProfile mara = new ParkProfile(
                null,
                "Maasai Mara National Reserve",
                "MAASAI_MARA",
                "Narok County",
                1510.0,
                "Rolling Grassland Savannah & Riverine Woodland",
                1961,
                "Sekenani Gate & Mara Triangle HQ",
                "Unfenced Greater Mara Ecosystem & Community Conservancies",
                "HIGH - Livestock Grazing Encroachment & Predator Conflict",
                "Mara River Crossing Points, Talek River Junction, Sand River Basin, Musiara Marsh",
                "Lions (Mara Predator Project), Cheetahs, Leopards, Elephants, Great Migration Herds",
                "World-renowned for apex predator densities and the Great Migration across the Mara River.",
                -1.50,
                35.15,
                11
        );

        ParkProfile nairobi = new ParkProfile(
                null,
                "Nairobi National Park",
                "NAIROBI_NP",
                "Nairobi City / Kajiado County",
                117.0,
                "Open Grass Plains, Acacia Bush & Riverine Gorge",
                1946,
                "KWS Central Command Headquarters & Ivory Burning Site Post",
                "Electrified Northern/Eastern City Perimeter (Unfenced South Kitengela Corridor)",
                "HIGH - Urban Encroachment & Southern Kitengela Buffer Pressures",
                "Nagolomon Dam, Hippo Pools (Mbagathi River), Athi Basin Waterholes, Hyena Dam",
                "Eastern Black Rhino Sanctuary, Lion Pride, Leopard, Cheetah, Maasai Giraffe, Cape Buffalo",
                "The only protected wildlife national park bordering a capital metropolis. High-security sanctuary for Black Rhinos.",
                -1.37,
                36.86,
                12
        );

        ParkProfile olPejeta = new ParkProfile(
                null,
                "Ol Pejeta Conservancy",
                "OL_PEJETA",
                "Laikipia County",
                364.0,
                "High-Plateau Savannah, Ewaso Nyiro River Basin & Acacia Plains",
                1988,
                "Morani Command Post & Sweetwaters Research Base",
                "Smart Solar-Powered High-Security Perimeter Fence",
                "CRITICAL - Last Northern White Rhinos Sanctuary Protection",
                "Ewaso Nyiro River Pools, Sweetwaters Dam, Morani Waterhole, Pelican Dam",
                "Northern White Rhinos (Najin & Fatu), Black Rhinos, Grevy's Zebra, African Wild Dog",
                "East Africa's largest Black Rhino sanctuary and home to the world's last two surviving Northern White Rhinos.",
                0.03,
                36.95,
                12
        );

        parkProfileRepository.saveAll(List.of(amboseli, tsavoEast, tsavoWest, mara, nairobi, olPejeta));
        log.info("Successfully seeded Kenyan park operational profiles.");
    }

    private void seedAnimals() {
        if (animalRepository.count() > 0) {
            return;
        }

        log.info("Seeding initial Kenya collared wildlife registry...");
        List<Animal> defaultAnimals = List.of(
                new Animal(null, "Echo's Matriarch", "African Elephant", "KWS-AMB-ELE01"),
                new Animal(null, "Mutula Bull", "African Elephant", "KWS-AMB-ELE04"),
                new Animal(null, "Galana Red Bull", "African Elephant", "KWS-TSV-ELE12"),
                new Animal(null, "Satao Pride Lioness", "Lion", "KWS-TSV-LIO03"),
                new Animal(null, "Kipsing Male", "Lion", "KWS-MAR-LIO07"),
                new Animal(null, "Talek River Female", "Cheetah", "KWS-MAR-CHT02"),
                new Animal(null, "Mukurwe Black Rhino", "Eastern Black Rhino", "KWS-NBI-RHN01"),
                new Animal(null, "Simba Nairobi", "Lion", "KWS-NBI-LIO05"),
                new Animal(null, "Baraka Rhino", "Black Rhino", "KWS-OLP-RHN03"),
                new Animal(null, "Kibo Giraffe", "Maasai Giraffe", "KWS-AMB-GIR02"),
                new Animal(null, "Zuri Plains Zebra", "Plains Zebra", "KWS-NBI-ZEB08")
        );
        animalRepository.saveAll(defaultAnimals);
        log.info("Successfully seeded {} collared animals.", defaultAnimals.size());
    }

    private void seedPatrolUnits() {
        if (patrolRepository.count() > 0) {
            return;
        }

        log.info("Seeding active KWS patrol units and airwing aircraft...");
        List<PatrolUnit> defaultPatrols = List.of(
                new PatrolUnit(null, "Amboseli Ranger Cruiser Alpha", "KWS-CRU-01", "LAND_CRUISER", -2.665, 37.250, "AVAILABLE", "Amboseli NP"),
                new PatrolUnit(null, "Tsavo Galana Airwing 1", "KWS-AIR-04", "AIRWING", -2.850, 38.650, "ON_PATROL", "Tsavo East NP"),
                new PatrolUnit(null, "Mara Anti-Poaching Interceptor", "KWS-CRU-09", "LAND_CRUISER", -1.490, 35.140, "AVAILABLE", "Maasai Mara"),
                new PatrolUnit(null, "Nairobi Sanctuary Quick Response", "KWS-CRU-03", "LAND_CRUISER", -1.370, 36.850, "AVAILABLE", "Nairobi NP"),
                new PatrolUnit(null, "Ol Pejeta Rhino Security Cruiser", "KWS-CRU-07", "LAND_CRUISER", 0.035, 36.960, "AVAILABLE", "Ol Pejeta")
        );
        patrolRepository.saveAll(defaultPatrols);
        log.info("Successfully seeded {} KWS patrol units.", defaultPatrols.size());
    }

    private Polygon createPolygon(Coordinate[] coords) {
        Polygon polygon = geometryFactory.createPolygon(coords);
        polygon.setSRID(4326);
        return polygon;
    }
}
