package com.kws.backend.backend.config;

import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.repository.GeofenceRepository;
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
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public void run(String... args) {
        seedGeofences();
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

    private Polygon createPolygon(Coordinate[] coords) {
        Polygon polygon = geometryFactory.createPolygon(coords);
        polygon.setSRID(4326);
        return polygon;
    }
}
