package com.kws.backend.backend.controller;

import com.kws.backend.backend.model.ParkProfile;
import com.kws.backend.backend.service.ParkProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parks")
@RequiredArgsConstructor
public class ParkProfileController {
    private final ParkProfileService parkProfileService;

    @GetMapping
    public ResponseEntity<List<ParkProfile>> getAllParkProfiles() {
        return ResponseEntity.ok(parkProfileService.getAllProfiles());
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<ParkProfile> getParkProfile(@PathVariable String identifier) {
        return parkProfileService.getProfileByCodeOrName(identifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ParkProfile> createParkProfile(@RequestBody ParkProfile profile) {
        return ResponseEntity.ok(parkProfileService.saveProfile(profile));
    }
}
