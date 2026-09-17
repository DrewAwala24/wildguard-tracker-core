package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.IncidentDto;
import com.kws.backend.backend.model.IncidentLog;
import com.kws.backend.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public List<IncidentDto> getActiveIncidents() {
        return incidentRepository.findByStatusOrderByTimestampDesc("ACTIVE").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<IncidentDto> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public IncidentDto recordIncident(String animalName, String collarId, String zoneName, String severity, String message) {
        IncidentLog incident = new IncidentLog();
        incident.setAnimalName(animalName);
        incident.setCollarId(collarId);
        incident.setZoneName(zoneName);
        incident.setSeverity(severity);
        incident.setMessage(message);
        incident.setStatus("ACTIVE");
        incident.setTimestamp(LocalDateTime.now());
        return toDto(incidentRepository.save(incident));
    }

    public IncidentDto resolveIncident(Long id, String notes) {
        IncidentLog incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));

        incident.setStatus("RESOLVED");
        incident.setResolutionNotes(notes != null && !notes.isBlank() 
                ? notes 
                : "Incident successfully resolved by KWS field team; animals guided back to protected zone.");
        log.info("Incident #{} marked RESOLVED with notes: {}", id, incident.getResolutionNotes());
        return toDto(incidentRepository.save(incident));
    }

    public Map<String, Object> broadcastCommunitySms(String corridor, String message) {
        // Simulates automated KWS emergency SMS dispatch to pastoralists & farmers along buffer zones
        int recipientCount = corridor != null && corridor.contains("Kimana") ? 54 : 38;
        String corridorName = corridor != null ? corridor : "Community Dispersal Area";

        log.warn("[KWS EMERGENCY SMS BROADCAST] To {} farmers in {}: {}", recipientCount, corridorName, message);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "DELIVERED");
        response.put("recipients", recipientCount);
        response.put("corridor", corridorName);
        response.put("dispatchedAt", LocalDateTime.now().toString());
        response.put("message", message);
        response.put("provider", "KWS Africa's Talking Gateway");
        return response;
    }

    private IncidentDto toDto(IncidentLog log) {
        IncidentDto dto = new IncidentDto();
        dto.setId(log.getId());
        dto.setAnimalName(log.getAnimalName());
        dto.setCollarId(log.getCollarId());
        dto.setZoneName(log.getZoneName());
        dto.setSeverity(log.getSeverity());
        dto.setMessage(log.getMessage());
        dto.setStatus(log.getStatus());
        dto.setResolutionNotes(log.getResolutionNotes());
        dto.setDispatchedUnit(log.getDispatchedUnit());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }
}
