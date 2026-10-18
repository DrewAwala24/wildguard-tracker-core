package com.kws.backend.backend;

import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.model.IncidentLog;
import com.kws.backend.backend.model.PatrolDispatch;
import com.kws.backend.backend.model.PatrolUnit;
import com.kws.backend.backend.repository.IncidentRepository;
import com.kws.backend.backend.repository.PatrolRepository;
import com.kws.backend.backend.service.IncidentService;
import com.kws.backend.backend.service.PatrolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PatrolDispatchIntegrationTest {

    @Autowired
    private PatrolService patrolService;

    @Autowired
    private PatrolRepository patrolRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("Dispatch patrol order updates unit status, links incident, and records dispatch")
    void testDispatchPatrolOrder() {
        // Find an available patrol unit
        List<PatrolUnit> patrols = patrolRepository.findByStatus("AVAILABLE");
        assertFalse(patrols.isEmpty(), "Available patrol units must be present");
        PatrolUnit unit = patrols.get(0);

        // Create an incident to dispatch against
        var incDto = incidentService.recordIncident("Test Elephant", "KWS-TEST-01", "Kimana Corridor", "CRITICAL", "Crop raiding warning");
        assertNotNull(incDto);

        // Dispatch unit
        PatrolDispatch dispatch = patrolService.createDispatch(
                unit.getId(), incDto.getId(), -2.68, 37.38, "Amboseli NP", "Intercept and escort back to park", "commander"
        );

        assertNotNull(dispatch);
        assertNotNull(dispatch.getDispatchCode());
        assertEquals("DISPATCHED", dispatch.getStatus());

        // Verify Unit status is now DISPATCHED
        PatrolUnit reloadedUnit = patrolRepository.findById(unit.getId()).orElseThrow();
        assertEquals("DISPATCHED", reloadedUnit.getStatus());

        // Verify Incident status is updated to DISPATCHED
        IncidentLog reloadedInc = incidentRepository.findById(incDto.getId()).orElseThrow();
        assertEquals("DISPATCHED", reloadedInc.getStatus());
        assertEquals(unit.getCallSign(), reloadedInc.getDispatchedUnit());

        // Attempting to dispatch the same unit again must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            patrolService.createDispatch(unit.getId(), null, -2.68, 37.38, "Amboseli NP", "Duplicate dispatch", "commander");
        });

        // Reset status back to AVAILABLE
        PatrolUnitDto updated = patrolService.updateStatus(unit.getId(), "AVAILABLE");
        assertEquals("AVAILABLE", updated.getStatus());
    }
}
