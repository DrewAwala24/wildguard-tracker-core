package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.TelemetryRequestDto;
import com.kws.backend.backend.dto.TelemetryTrailDto;
import com.kws.backend.backend.model.TelemetryLocation;
import com.kws.backend.backend.service.TelemetryService;
import com.kws.backend.backend.service.TelemetrySimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final TelemetrySimulationService simulationService;

    @GetMapping
    public ResponseEntity<List<TelemetryLocation>> getAllTelemetry() {
        return ResponseEntity.ok(telemetryService.getAllTelemetry());
    }

    @GetMapping("/animal/{collarId}/trail")
    public ResponseEntity<List<TelemetryTrailDto>> getAnimalTrail(@PathVariable String collarId) {
        return ResponseEntity.ok(telemetryService.getAnimalTrail(collarId));
    }

    @PostMapping
    public ResponseEntity<String> ingestTelemetry(@RequestBody TelemetryRequestDto dto) {
        telemetryService.processTelemetry(dto);
        return ResponseEntity.ok("Telemetry logged successfully");
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> triggerSimulationStep() {
        int updated = simulationService.simulateStep();
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "updatedCollars", updated));
    }

    @PostMapping("/simulation/toggle")
    public ResponseEntity<Map<String, Object>> toggleSimulation(@RequestParam(required = false) Boolean active) {
        boolean nextState = active != null ? active : !simulationService.isSimulationActive();
        simulationService.setSimulationActive(nextState);
        return ResponseEntity.ok(Map.of("simulationActive", nextState));
    }
}