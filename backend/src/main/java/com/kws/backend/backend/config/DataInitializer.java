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
import java.util.Optional;

/**
 * Initializes and synchronizes database registries for all 13 Kenyan national parks,
 * conservancies, PostGIS geofences, and endangered wildlife populations.
 */
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

    // ── 1. Geofences for All 13 National Parks & Buffer Corridors ─────────────
    private void seedGeofences() {
        log.info("Checking & synchronizing Kenya National Parks and Conservancies geofence boundaries...");

        upsertGeofence("Amboseli National Park", "PARK", new Coordinate[]{
                new Coordinate(37.18, -2.58), new Coordinate(37.35, -2.58),
                new Coordinate(37.35, -2.72), new Coordinate(37.18, -2.72), new Coordinate(37.18, -2.58)
        });

        upsertGeofence("Kimana Community Wildlife Corridor", "COMMUNITY_BUFFER", new Coordinate[]{
                new Coordinate(37.35, -2.68), new Coordinate(37.45, -2.68),
                new Coordinate(37.45, -2.75), new Coordinate(37.35, -2.75), new Coordinate(37.35, -2.68)
        });

        upsertGeofence("Nairobi National Park", "PARK", new Coordinate[]{
                new Coordinate(36.76, -1.343), new Coordinate(36.95, -1.343),
                new Coordinate(36.95, -1.440), new Coordinate(36.76, -1.440), new Coordinate(36.76, -1.343)
        });

        upsertGeofence("Maasai Mara National Reserve", "PARK", new Coordinate[]{
                new Coordinate(34.98, -1.36), new Coordinate(35.34, -1.36),
                new Coordinate(35.34, -1.62), new Coordinate(34.98, -1.62), new Coordinate(34.98, -1.36)
        });

        upsertGeofence("Tsavo East National Park", "PARK", new Coordinate[]{
                new Coordinate(38.30, -2.40), new Coordinate(39.00, -2.40),
                new Coordinate(39.00, -3.30), new Coordinate(38.30, -3.30), new Coordinate(38.30, -2.40)
        });

        upsertGeofence("Tsavo West National Park", "PARK", new Coordinate[]{
                new Coordinate(37.70, -2.95), new Coordinate(38.25, -2.95),
                new Coordinate(38.25, -3.60), new Coordinate(37.70, -3.60), new Coordinate(37.70, -2.95)
        });

        upsertGeofence("Ol Pejeta Conservancy", "PARK", new Coordinate[]{
                new Coordinate(36.88, 0.10), new Coordinate(37.05, 0.10),
                new Coordinate(37.05, 0.00), new Coordinate(36.88, 0.00), new Coordinate(36.88, 0.10)
        });

        upsertGeofence("Lake Nakuru National Park", "PARK", new Coordinate[]{
                new Coordinate(36.05, -0.32), new Coordinate(36.14, -0.32),
                new Coordinate(36.14, -0.44), new Coordinate(36.05, -0.44), new Coordinate(36.05, -0.32)
        });

        upsertGeofence("Samburu National Reserve", "PARK", new Coordinate[]{
                new Coordinate(37.48, 0.68), new Coordinate(37.60, 0.68),
                new Coordinate(37.60, 0.56), new Coordinate(37.48, 0.56), new Coordinate(37.48, 0.68)
        });

        upsertGeofence("Aberdare National Park", "PARK", new Coordinate[]{
                new Coordinate(36.65, -0.32), new Coordinate(36.90, -0.32),
                new Coordinate(36.90, -0.52), new Coordinate(36.65, -0.52), new Coordinate(36.65, -0.32)
        });

        upsertGeofence("Mount Kenya National Park", "PARK", new Coordinate[]{
                new Coordinate(37.20, -0.10), new Coordinate(37.42, -0.10),
                new Coordinate(37.42, -0.26), new Coordinate(37.20, -0.26), new Coordinate(37.20, -0.10)
        });

        upsertGeofence("Lewa Wildlife Conservancy", "PARK", new Coordinate[]{
                new Coordinate(37.40, 0.26), new Coordinate(37.52, 0.26),
                new Coordinate(37.52, 0.15), new Coordinate(37.40, 0.15), new Coordinate(37.40, 0.26)
        });

        upsertGeofence("Saiwa Swamp National Park", "PARK", new Coordinate[]{
                new Coordinate(35.10, 1.11), new Coordinate(35.14, 1.11),
                new Coordinate(35.14, 1.07), new Coordinate(35.10, 1.07), new Coordinate(35.10, 1.11)
        });

        upsertGeofence("Mount Elgon National Park", "PARK", new Coordinate[]{
                new Coordinate(34.50, 1.22), new Coordinate(34.68, 1.22),
                new Coordinate(34.68, 1.08), new Coordinate(34.50, 1.08), new Coordinate(34.50, 1.22)
        });
    }

    private void upsertGeofence(String name, String type, Coordinate[] coords) {
        boolean exists = geofenceRepository.findAll().stream().anyMatch(z -> z.getZoneName().equalsIgnoreCase(name));
        if (!exists) {
            Polygon poly = createPolygon(coords);
            geofenceRepository.save(new GeofenceZone(null, name, type, poly));
            log.info("Seeded geofence: {}", name);
        }
    }

    // ── 2. Operational Profiles for All 13 National Parks & Conservancies ──────
    private void seedParkProfiles() {
        log.info("Checking & synchronizing Kenya National Parks and Conservancies operational profiles...");

        upsertParkProfile(new ParkProfile(
                null, "Amboseli National Park", "AMBOSELI", "Kajiado County",
                392.0, "Savannah Plains, Acacia Woodland & Freshwater Swamps", 1974,
                "Ol Tukai Ranger Command Post", "Partially Fenced (Southern Kimana Corridor Buffer)",
                "MODERATE - Human-Wildlife Conflict Buffer",
                "Enkongo Narok Swamp, Ol Okenya Marsh, Lake Amboseli Basin, Observation Hill Waterhole",
                "African Bush Elephant, Lion, Cheetah, Maasai Giraffe, Cape Buffalo, Spotted Hyena",
                "Famous for free-ranging elephants against Mount Kilimanjaro. Critical focus is the southern Kimana community corridor.",
                -2.65, 37.26, 11
        ));

        upsertParkProfile(new ParkProfile(
                null, "Tsavo East National Park", "TSAVO_EAST", "Taita-Taveta, Kitui & Tana River Counties",
                13747.0, "Semi-Arid Bushland, Savannah & Riverine Galana Basin", 1948,
                "Voi Gate Headquarters & Lugard Falls Patrol Post", "Open Dispersal (SGR Wildlife Underpasses & Highway Crossings)",
                "HIGH - Vast Area Poaching & SGR Dispersal Risk",
                "Aruba Dam, Galana River Rapids, Mudanda Rock Water catchment, Lugard Falls Pool",
                "Red Dust Elephants, Tsavo Maneless Lions, Black Rhino, Hirola, Lesser Kudu",
                "One of the largest national parks in Kenya. Renowned for dust-red elephants and wildlife movement across the SGR corridor.",
                -2.77, 38.77, 10
        ));

        upsertParkProfile(new ParkProfile(
                null, "Tsavo West National Park", "TSAVO_WEST", "Taita-Taveta County",
                9065.0, "Rugged Volcanic Ridges, Mzima Springs & Acacia Savannah", 1948,
                "Ngulia Rhino Sanctuary Command Post", "Specialized Rhino Sanctuary Electric Grid + Open Park",
                "CRITICAL - Intensive Rhino Anti-Poaching Sanctuary",
                "Mzima Springs Natural Oasis, Lake Jipe Wetlands, Ngulia Water Pan, Shetani Lava Pools",
                "Eastern Black Rhino, African Leopard, Hippopotamus, Crocodile, Wild Dog",
                "Rugged mountainous volcanic landscape with Shetani lava flows and the heavily fortified Ngulia Rhino Sanctuary.",
                -3.32, 38.00, 10
        ));

        upsertParkProfile(new ParkProfile(
                null, "Maasai Mara National Reserve", "MAASAI_MARA", "Narok County",
                1510.0, "Rolling Grassland Savannah & Riverine Woodland", 1961,
                "Sekenani Gate & Mara Triangle HQ", "Unfenced Greater Mara Ecosystem & Community Conservancies",
                "HIGH - Livestock Grazing Encroachment & Predator Conflict",
                "Mara River Crossing Points, Talek River Junction, Sand River Basin, Musiara Marsh",
                "Lions (Mara Predator Project), Cheetahs, Leopards, Elephants, Great Migration Herds",
                "World-renowned for apex predator densities and the Great Migration across the Mara River.",
                -1.50, 35.15, 11
        ));

        upsertParkProfile(new ParkProfile(
                null, "Nairobi National Park", "NAIROBI_NP", "Nairobi City / Kajiado County",
                117.0, "Open Grass Plains, Acacia Bush & Riverine Gorge", 1946,
                "KWS Central Command Headquarters & Ivory Burning Site Post", "Electrified Northern/Eastern City Perimeter (Unfenced South Kitengela Corridor)",
                "HIGH - Urban Encroachment & Southern Kitengela Buffer Pressures",
                "Nagolomon Dam, Hippo Pools (Mbagathi River), Athi Basin Waterholes, Hyena Dam",
                "Eastern Black Rhino Sanctuary, Lion Pride, Leopard, Cheetah, Maasai Giraffe, Cape Buffalo",
                "The only protected national park bordering a capital metropolis. High-security sanctuary for Black Rhinos.",
                -1.37, 36.86, 12
        ));

        upsertParkProfile(new ParkProfile(
                null, "Ol Pejeta Conservancy", "OL_PEJETA", "Laikipia County",
                364.0, "High-Plateau Savannah, Ewaso Nyiro River Basin & Acacia Plains", 1988,
                "Morani Command Post & Sweetwaters Research Base", "Smart Solar-Powered High-Security Perimeter Fence",
                "CRITICAL - Last Northern White Rhinos Sanctuary Protection",
                "Ewaso Nyiro River Pools, Sweetwaters Dam, Morani Waterhole, Pelican Dam",
                "Northern White Rhinos (Najin & Fatu), Black Rhinos, Grevy's Zebra, African Wild Dog",
                "East Africa's largest Black Rhino sanctuary and home to the world's last two surviving Northern White Rhinos.",
                0.03, 36.95, 12
        ));

        upsertParkProfile(new ParkProfile(
                null, "Lake Nakuru National Park", "LAKE_NAKURU", "Nakuru County",
                188.0, "Alkaline Rift Valley Lake, Acacia Woodland & Euphorbia Forest", 1961,
                "Main Gate Headquarters & Baboon Cliff Patrol Base", "Fully Fenced Perimeter (100% Electrified)",
                "MODERATE - Water Level Fluctuation & Fenced Rhino Security",
                "Makalia Falls, Baboon Cliff Shoreline, Lion Hill Springs, Pelican Point",
                "Eastern Black Rhino, Southern White Rhino, Rothschild's Giraffe, Greater & Lesser Flamingo",
                "Renowned sanctuary for both Black and White Rhinos and Rothschild's Giraffes around an alkaline Great Rift Valley lake.",
                -0.37, 36.08, 12
        ));

        upsertParkProfile(new ParkProfile(
                null, "Samburu National Reserve", "SAMBURU", "Samburu County",
                165.0, "Semi-Arid Scrubland, Riverine Doum Palm Woodland", 1962,
                "Archer's Post Ranger Station & Ewaso Command", "Unfenced Open Northern Pastoral Dispersal",
                "HIGH - Drought Pressures & Livestock Conflict",
                "Ewaso Nyiro River Pools, Buffalo Springs Confluence, West Gate Water Pans",
                "Grevy's Zebra, Reticulated Giraffe, Beisa Oryx, Gerenuk, Somali Ostrich, Lion",
                "Home to the Samburu 'Special Five' including the endangered Grevy's Zebra along the life-giving Ewaso Nyiro River.",
                0.62, 37.53, 11
        ));

        upsertParkProfile(new ParkProfile(
                null, "Aberdare National Park", "ABERDARE", "Nyeri & Nyandarua Counties",
                766.0, "Montane Rainforest, Bamboo Thickett & Moorland Plateau", 1950,
                "Mweiga Headquarters & Ruhuruini Gate Station", "Rhino Ark Solar Electrified Perimeter Fence (400 km)",
                "HIGH - Dense Cloud Cover Tracking & Forest Poaching",
                "Karuru Waterfalls Basin, Chania River Pools, Queen's Cave Stream, Treetops Marsh",
                "Critically Endangered Mountain Bongo, Eastern Black Rhino, Giant Forest Hog, Leopard",
                "Steep afro-alpine ravines and mist-covered bamboo forests sheltering the critically endangered Mountain Bongo antelope.",
                -0.42, 36.70, 11
        ));

        upsertParkProfile(new ParkProfile(
                null, "Mount Kenya National Park", "MOUNT_KENYA", "Meru, Embu & Nyeri Counties",
                715.0, "Afro-Alpine Moorlands, Glacial Tarns & Montane Forest", 1949,
                "Naro Moru Ranger Post & Sirimon Gate Station", "Electric Wildlife Corridor Fences (Mount Kenya Elephant Underpass)",
                "MODERATE - Illegal Logging Buffer & Human-Elephant Conflict",
                "Lake Michaelson, Lake Alice, Nithi River Cascades, Sirimon Glacial Stream",
                "Mountain Bongo, African Elephant, Giant Forest Hog, Black-fronted Duiker, Mount Kenya Hyrax",
                "UNESCO World Heritage Site with rugged glacier-clad summits. Shelters unique high-altitude elephants and Mountain Bongos.",
                -0.15, 37.30, 11
        ));

        upsertParkProfile(new ParkProfile(
                null, "Lewa Wildlife Conservancy", "LEWA", "Meru & Laikipia Counties",
                250.0, "Rolling Grassland Savannah, Acacia Woodlands & Papyrus Swamp", 1995,
                "Lewa Operations Operations HQ & K9 Anti-Poaching Base", "Smart Predator-Permeable & High-Security Rhino Fence",
                "CRITICAL - High-Value Rhino & Grevy's Zebra Breeding Core",
                "Lewa Swamp Wetlands, Sirikoi Springs, Ngare Ndare River Weir",
                "Eastern Black Rhino, White Rhino, Grevy's Zebra, Lion, Cheetah, African Wild Dog",
                "Global model for community-centric conservation with an intensive anti-poaching and rhino breeding program.",
                0.20, 37.45, 12
        ));

        upsertParkProfile(new ParkProfile(
                null, "Saiwa Swamp National Park", "SAIWA_SWAMP", "Trans-Nzoia County",
                2.9, "Wetland Bulrush Swamp & Riverine Canopy Forest", 1974,
                "Saiwa Swamp KWS Headquarters Post", "Fully Fenced Habitat Protection",
                "LOW - Strict Botanical & Sitatunga Wetland Sanctuary",
                "Saiwa River Marsh Basin, Sitatunga Boardwalk Pools, Nature Trail Streams",
                "Semi-aquatic Sitatunga Antelope, De Brazza's Monkey, Giant Kingfisher, Otters",
                "Kenya's smallest national park, created specifically to protect the rare, semi-aquatic Sitatunga antelope and its wetland habitat.",
                1.09, 35.12, 13
        ));

        upsertParkProfile(new ParkProfile(
                null, "Mount Elgon National Park", "MOUNT_ELGON", "Bungoma County",
                169.0, "Volcanic Slopes, Afro-Montane Forest & Subterranean Salt Caves", 1968,
                "Chorlim Gate Command Post & Rongai Forest Station", "Partially Fenced Forest Reserve Buffer",
                "HIGH - Transboundary Uganda Border & Elephant Cave Salt Mining",
                "Kitum Cave Underground Pool, Makingeny Falls, Chepnyalil River Pool",
                "Cave-Mining African Elephants, Colobus Monkey, Blue Monkey, Leopard, Bushbuck",
                "Famous for vast subterranean caves where generations of elephants venture deep in the dark to mine calcium-rich salt with their tusks.",
                1.13, 34.58, 11
        ));
    }

    private void upsertParkProfile(ParkProfile profile) {
        Optional<ParkProfile> existing = parkProfileRepository.findByParkNameIgnoreCase(profile.getParkName());
        if (existing.isEmpty()) {
            parkProfileRepository.save(profile);
            log.info("Seeded park operational profile: {}", profile.getParkName());
        }
    }

    // ── 3. Comprehensive Wildlife Registry Across All Species & Parks ────────
    private void seedAnimals() {
        log.info("Checking & synchronizing Kenya collared wildlife registry across all species...");

        List<Animal> roster = List.of(
                // Amboseli NP
                new Animal(null, "Echo's Matriarch", "African Bush Elephant", "KWS-AMB-ELE01", "Amboseli NP", "Female", "VU"),
                new Animal(null, "Mutula Bull", "African Bush Elephant", "KWS-AMB-ELE04", "Amboseli NP", "Male", "VU"),
                new Animal(null, "Kibo Giraffe", "Maasai Giraffe", "KWS-AMB-GIR02", "Amboseli NP", "Male", "VU"),
                new Animal(null, "Amboseli Stalker", "Secretary Bird", "KWS-AMB-SEC01", "Amboseli NP", "Male", "EN"),

                // Tsavo East NP
                new Animal(null, "Galana Red Bull", "African Bush Elephant", "KWS-TSV-ELE12", "Tsavo East NP", "Male", "VU"),
                new Animal(null, "Satao Pride Lioness", "African Lion", "KWS-TSV-LIO03", "Tsavo East NP", "Female", "VU"),
                new Animal(null, "Tsavo Ridge Raptor", "Martial Eagle", "KWS-TSV-EAG02", "Tsavo East NP", "Female", "EN"),

                // Tsavo West NP
                new Animal(null, "Ngulia Bull", "Eastern Black Rhino", "KWS-TSW-RHN05", "Tsavo West NP", "Male", "CR"),

                // Maasai Mara
                new Animal(null, "Kipsing Male", "African Lion", "KWS-MAR-LIO07", "Maasai Mara", "Male", "VU"),
                new Animal(null, "Talek River Female", "Cheetah", "KWS-MAR-CHT02", "Maasai Mara", "Female", "VU"),
                new Animal(null, "Mara Apex Soarer", "Martial Eagle", "KWS-MAR-EAG01", "Maasai Mara", "Male", "EN"),

                // Nairobi NP
                new Animal(null, "Mukurwe Black Rhino", "Eastern Black Rhino", "KWS-NBI-RHN01", "Nairobi NP", "Male", "CR"),
                new Animal(null, "Simba Nairobi", "African Lion", "KWS-NBI-LIO05", "Nairobi NP", "Male", "VU"),
                new Animal(null, "Zuri Plains Zebra", "Plains Zebra", "KWS-NBI-ZEB08", "Nairobi NP", "Female", "LC"),
                new Animal(null, "Athi Plains Sprinter", "Cheetah", "KWS-NBI-CHT04", "Nairobi NP", "Female", "VU"),
                new Animal(null, "Nairobi Plains Hunter", "Secretary Bird", "KWS-NBI-SEC02", "Nairobi NP", "Male", "EN"),

                // Ol Pejeta Conservancy
                new Animal(null, "Najin", "Northern White Rhino", "KWS-OLP-WRH01", "Ol Pejeta", "Female", "CR"),
                new Animal(null, "Fatu", "Northern White Rhino", "KWS-OLP-WRH02", "Ol Pejeta", "Female", "CR"),
                new Animal(null, "Baraka Rhino", "Eastern Black Rhino", "KWS-OLP-RHN03", "Ol Pejeta", "Male", "CR"),

                // Lake Nakuru
                new Animal(null, "Baboon Cliff Rhino", "Eastern Black Rhino", "KWS-NAK-RHN02", "Lake Nakuru", "Male", "CR"),
                new Animal(null, "Makalia White Rhino", "Southern White Rhino", "KWS-NAK-WRH03", "Lake Nakuru", "Female", "NT"),
                new Animal(null, "Rothschild Nakuru Tall", "Rothschild's Giraffe", "KWS-NAK-GIR01", "Lake Nakuru", "Female", "VU"),

                // Samburu
                new Animal(null, "Samburu Stripe", "Grevy's Zebra", "KWS-SAM-GZB01", "Samburu", "Male", "EN"),
                new Animal(null, "Wamba Stallion", "Grevy's Zebra", "KWS-SAM-GZB02", "Samburu", "Male", "EN"),
                new Animal(null, "Samburu Warrior", "African Lion", "KWS-SAM-LIO02", "Samburu", "Male", "VU"),

                // Aberdare
                new Animal(null, "Kinangop Male", "Mountain Bongo", "KWS-ABR-BGO01", "Aberdare", "Male", "CR"),
                new Animal(null, "Chania Female", "Mountain Bongo", "KWS-ABR-BGO02", "Aberdare", "Female", "CR"),

                // Mount Kenya
                new Animal(null, "Ragati Bull", "Mountain Bongo", "KWS-MTK-BGO03", "Mount Kenya", "Male", "CR"),
                new Animal(null, "Sirimon Tusker", "African Bush Elephant", "KWS-MTK-ELE02", "Mount Kenya", "Male", "VU"),

                // Lewa
                new Animal(null, "Lewa Ridge Female", "Eastern Black Rhino", "KWS-LEW-RHN04", "Lewa", "Female", "CR"),
                new Animal(null, "Lewa Grevy Mare", "Grevy's Zebra", "KWS-LEW-GZB03", "Lewa", "Female", "EN"),

                // Saiwa Swamp
                new Animal(null, "Saiwa Marsh Buck", "Sitatunga", "KWS-SAI-SIT01", "Saiwa Swamp", "Male", "LC"),
                new Animal(null, "Kipsaina Doe", "Sitatunga", "KWS-SAI-SIT02", "Saiwa Swamp", "Female", "LC"),

                // Mount Elgon
                new Animal(null, "Kitum Cave Bull", "African Bush Elephant", "KWS-ELG-ELE01", "Mount Elgon", "Male", "VU")
        );

        for (Animal animal : roster) {
            Optional<Animal> existing = animalRepository.findByCollarId(animal.getCollarId());
            if (existing.isEmpty()) {
                animalRepository.save(animal);
                log.info("Seeded animal: {} ({}) in {}", animal.getName(), animal.getSpecies(), animal.getParkName());
            } else {
                // Synchronize parkName, sex, iucnStatus if previously null
                Animal curr = existing.get();
                boolean changed = false;
                if (curr.getParkName() == null) { curr.setParkName(animal.getParkName()); changed = true; }
                if (curr.getSex() == null) { curr.setSex(animal.getSex()); changed = true; }
                if (curr.getIucnStatus() == null) { curr.setIucnStatus(animal.getIucnStatus()); changed = true; }
                if (changed) {
                    animalRepository.save(curr);
                }
            }
        }
    }

    // ── 4. Stationed Patrol Units ─────────────────────────────────────────────
    private void seedPatrolUnits() {
        if (patrolRepository.count() >= 5) {
            return;
        }

        List<PatrolUnit> patrols = List.of(
                new PatrolUnit(null, "Amboseli Ranger Cruiser Alpha", "KWS-CRU-01", "LAND_CRUISER", -2.665, 37.250, "AVAILABLE", "Amboseli NP"),
                new PatrolUnit(null, "Tsavo Galana Airwing 1", "KWS-AIR-04", "AIRWING", -2.850, 38.650, "ON_PATROL", "Tsavo East NP"),
                new PatrolUnit(null, "Mara Anti-Poaching Interceptor", "KWS-CRU-09", "LAND_CRUISER", -1.490, 35.140, "AVAILABLE", "Maasai Mara"),
                new PatrolUnit(null, "Nairobi Sanctuary Quick Response", "KWS-CRU-03", "LAND_CRUISER", -1.370, 36.850, "AVAILABLE", "Nairobi NP"),
                new PatrolUnit(null, "Ol Pejeta Rhino Security Cruiser", "KWS-CRU-07", "LAND_CRUISER", 0.035, 36.960, "AVAILABLE", "Ol Pejeta"),
                new PatrolUnit(null, "Samburu Desert Patrol 1", "KWS-CRU-11", "LAND_CRUISER", 0.620, 37.530, "AVAILABLE", "Samburu"),
                new PatrolUnit(null, "Aberdare Forest Anti-Poaching Unit", "KWS-CRU-14", "LAND_CRUISER", -0.410, 36.710, "AVAILABLE", "Aberdare")
        );

        for (PatrolUnit p : patrols) {
            boolean exists = patrolRepository.findAll().stream().anyMatch(u -> u.getCallSign().equalsIgnoreCase(p.getCallSign()));
            if (!exists) {
                patrolRepository.save(p);
            }
        }
    }

    private Polygon createPolygon(Coordinate[] coords) {
        Polygon polygon = geometryFactory.createPolygon(coords);
        polygon.setSRID(4326);
        return polygon;
    }
}
