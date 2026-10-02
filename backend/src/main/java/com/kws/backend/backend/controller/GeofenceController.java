package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.GeofenceDto;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GeofenceController {
    private final GeofenceService geofenceService;

    @GetMapping
    public ResponseEntity<List<GeofenceDto>> getAllGeofences() {
        return ResponseEntity.ok(geofenceService.getAllZoneDtos());
    }

    @PostMapping
    public ResponseEntity<GeofenceZone> createGeofence(@RequestBody GeofenceZone zone) {
        return ResponseEntity.ok(geofenceService.saveZone(zone));
    }
}