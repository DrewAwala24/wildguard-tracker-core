package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.IncidentDto;
import com.kws.backend.backend.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<IncidentDto>> getActiveIncidents() {
        return ResponseEntity.ok(incidentService.getActiveIncidents());
    }

    @GetMapping("/all")
    public ResponseEntity<List<IncidentDto>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<IncidentDto> resolveIncident(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(incidentService.resolveIncident(id, notes));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastCommunitySms(
            @RequestBody Map<String, String> payload) {
        String corridor = payload.getOrDefault("corridor", "Kimana Agricultural Buffer");
        String message = payload.getOrDefault("message", "KWS Alert: Collared wildlife breach detected in buffer corridor. Please stay alert.");
        return ResponseEntity.ok(incidentService.broadcastCommunitySms(corridor, message));
    }
}
