package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.service.PatrolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patrols")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolService patrolService;

    @GetMapping
    public ResponseEntity<List<PatrolUnitDto>> getAllPatrols() {
        return ResponseEntity.ok(patrolService.getAllPatrols());
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<PatrolUnitDto> dispatchPatrol(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Double> targetCoord) {
        Double targetLat = targetCoord != null ? targetCoord.get("latitude") : null;
        Double targetLng = targetCoord != null ? targetCoord.get("longitude") : null;
        return ResponseEntity.ok(patrolService.dispatchPatrol(id, targetLat, targetLng));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<PatrolUnitDto> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(patrolService.updateStatus(id, status));
    }
}
