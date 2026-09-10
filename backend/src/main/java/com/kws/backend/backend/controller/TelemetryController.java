package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.TelemetryRequestDto;
import com.kws.backend.backend.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryController {
    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<String> ingestTelemetry(@RequestBody TelemetryRequestDto dto) {
        telemetryService.processTelemetry(dto);
        return ResponseEntity.ok("Telemetry logged successfully");
    }
}