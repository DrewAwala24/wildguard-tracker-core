package com.kws.backend.backend.controller;

import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
public class GeofenceController {
    private final GeofenceService geofenceService;

    @GetMapping
    public ResponseEntity<List<GeofenceZone>> getAllGeofences() {
        return ResponseEntity.ok(geofenceService.getAllZones());
    }

    @PostMapping
    public ResponseEntity<GeofenceZone> createGeofence(@RequestBody GeofenceZone zone) {
        return ResponseEntity.ok(geofenceService.saveZone(zone));
    }
}