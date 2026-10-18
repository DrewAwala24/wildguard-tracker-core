package com.kws.backend.backend;

import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.repository.GeofenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SpatialGeofenceIntegrationTest {

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Test
    @DisplayName("PostGIS ST_Intersects correctly identifies point inside Nairobi National Park")
    void testPointInsideNairobiNp() {
        // Nagolomon Dam in Nairobi NP: lat -1.3820, lng 36.8250
        List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(36.8250, -1.3820);
        assertFalse(zones.isEmpty(), "Point must be inside Nairobi National Park");
        assertTrue(zones.stream().anyMatch(z -> z.getZoneName().contains("Nairobi")),
                "Zone must match Nairobi National Park");
    }

    @Test
    @DisplayName("PostGIS ST_Intersects identifies point inside Amboseli National Park")
    void testPointInsideAmboseliNp() {
        // Enkongo Narok Swamp in Amboseli: lat -2.6650, lng 37.2650
        List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(37.2650, -2.6650);
        assertFalse(zones.isEmpty(), "Point must be inside Amboseli National Park");
        assertTrue(zones.stream().anyMatch(z -> z.getZoneName().contains("Amboseli")));
    }

    @Test
    @DisplayName("PostGIS ST_Intersects detects breach inside Kimana Community Corridor buffer")
    void testPointInsideKimanaBuffer() {
        // Coordinates in Kimana Corridor buffer: lat -2.7100, lng 37.4000
        List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(37.4000, -2.7100);
        assertFalse(zones.isEmpty(), "Point must intersect Kimana Community Corridor");
        assertTrue(zones.stream().anyMatch(z -> "COMMUNITY_BUFFER".equalsIgnoreCase(z.getZoneType())),
                "Zone must be classified as COMMUNITY_BUFFER");
    }

    @Test
    @DisplayName("PostGIS returns empty zones for coordinates outside any sanctuary")
    void testPointOutsideAllParks() {
        // Point in the Indian Ocean: lat -3.00, lng 42.50
        List<GeofenceZone> zones = geofenceRepository.findZonesContainingPoint(42.50, -3.00);
        assertTrue(zones.isEmpty(), "Point outside all parks must not intersect any geofence");
    }

    @Test
    @DisplayName("PostGIS ST_Distance accurately calculates meters to park boundary")
    void testDistanceToParkBoundary() {
        // Point just outside Amboseli: lat -2.85, lng 37.25
        Double distanceMeters = geofenceRepository.findDistanceToZone("Amboseli National Park", 37.25, -2.85);
        assertNotNull(distanceMeters);
        assertTrue(distanceMeters > 0, "Distance to boundary must be positive");
        assertTrue(distanceMeters < 50_000, "Distance should be under 50 km");
    }
}
