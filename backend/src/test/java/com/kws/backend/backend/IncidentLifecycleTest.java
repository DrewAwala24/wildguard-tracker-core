package com.kws.backend.backend;

import com.kws.backend.backend.dto.IncidentDto;
import com.kws.backend.backend.model.IncidentLog;
import com.kws.backend.backend.repository.IncidentRepository;
import com.kws.backend.backend.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class IncidentLifecycleTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("Incident full lifecycle: creation, acknowledgement, resolution with notes")
    void testIncidentLifecycle() {
        // 1. Create
        IncidentDto inc = incidentService.recordIncident(
                "Echo Matriarch", "KWS-AMB-ELE01", "Kimana Corridor", "WARNING", "Herd grazing along buffer boundary"
        );
        assertNotNull(inc);
        assertEquals("ACTIVE", inc.getStatus());

        // 2. Acknowledge
        IncidentDto ack = incidentService.acknowledgeIncident(inc.getId(), "commander");
        assertEquals("ACKNOWLEDGED", ack.getStatus());

        IncidentLog reloaded = incidentRepository.findById(inc.getId()).orElseThrow();
        assertNotNull(reloaded.getAcknowledgedAt());

        // 3. Resolve
        IncidentDto resolved = incidentService.resolveIncident(inc.getId(), "Rangers guided herd back into Amboseli National Park.", "commander");
        assertEquals("RESOLVED", resolved.getStatus());
        assertTrue(resolved.getResolutionNotes().contains("guided herd back"));

        IncidentLog resolvedEntity = incidentRepository.findById(inc.getId()).orElseThrow();
        assertNotNull(resolvedEntity.getResolvedAt());
    }

    @Test
    @DisplayName("Duplicate alert suppression prevents spamming database during active breach")
    void testDuplicateAlertSuppression() {
        // First alert
        IncidentDto inc1 = incidentService.recordIncident(
                "Mutula Bull", "KWS-AMB-ELE04", "Farmland Edge", "CRITICAL", "First warning breach"
        );
        assertNotNull(inc1, "First breach event must be recorded");

        // Immediate second alert for same collar and zone within cooldown window
        IncidentDto inc2 = incidentService.recordIncident(
                "Mutula Bull", "KWS-AMB-ELE04", "Farmland Edge", "CRITICAL", "Immediate second ping in same zone"
        );
        assertNull(inc2, "Duplicate incident within cooldown window must be suppressed (return null)");
    }
}
