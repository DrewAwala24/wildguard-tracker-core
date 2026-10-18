package com.kws.backend.backend;

import com.kws.backend.backend.repository.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FlywayMigrationVerificationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private EcologicalLandmarkRepository landmarkRepository;

    @Autowired
    private ParkProfileRepository parkProfileRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Test
    @DisplayName("Flyway migrations executed and applied to database schema")
    void testFlywayMigrationsApplied() {
        assertNotNull(flyway);
        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 1, "At least one Flyway migration must be applied");

        for (MigrationInfo info : applied) {
            assertTrue(info.getState().isApplied(), "Migration " + info.getScript() + " must be successfully applied");
        }
    }

    @Test
    @DisplayName("Reference data seeded by Flyway migrations exists in PostgreSQL")
    void testReferenceDataExists() {
        assertTrue(userRepository.count() >= 5, "Pre-seeded user accounts must exist");
        assertTrue(speciesRepository.count() >= 10, "Pre-seeded conservation species must exist");
        assertTrue(landmarkRepository.count() >= 10, "Pre-seeded ecological landmarks must exist");
        assertTrue(parkProfileRepository.count() >= 10, "Pre-seeded park profiles must exist");
        assertTrue(geofenceRepository.count() >= 10, "Pre-seeded PostGIS geofences must exist");
    }
}
