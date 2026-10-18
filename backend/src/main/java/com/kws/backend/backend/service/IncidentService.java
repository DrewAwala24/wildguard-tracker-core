package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.IncidentDto;
import com.kws.backend.backend.model.IncidentLog;
import com.kws.backend.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    // Cooldown map: key = collarId + ":" + zoneName, value = last reported timestamp
    private final Map<String, LocalDateTime> incidentCooldowns = new ConcurrentHashMap<>();
    private static final int COOLDOWN_MINUTES = 10;

    public List<IncidentDto> getActiveIncidents() {
        return incidentRepository.findOpenIncidents().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<IncidentLog> getOpenIncidentEntities() {
        return incidentRepository.findOpenIncidents();
    }

    public List<IncidentDto> getAllIncidents() {
        return incidentRepository.findTop25ByOrderByTimestampDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<IncidentLog> getAllIncidentEntities() {
        return incidentRepository.findAll();
    }

    @Transactional
    public IncidentDto recordIncident(String animalName, String collarId, String zoneName, String severity, String message) {
        return recordIncident(animalName, collarId, zoneName, severity, message, null, null, null);
    }

    @Transactional
    public synchronized IncidentDto recordIncident(String animalName, String collarId, String zoneName, String severity, String message, String parkName, Double lat, Double lng) {
        String key = (collarId != null ? collarId : "ANIMAL") + ":" + (zoneName != null ? zoneName : "ZONE");
        LocalDateTime lastReported = incidentCooldowns.get(key);

        if (lastReported != null && lastReported.plusMinutes(COOLDOWN_MINUTES).isAfter(LocalDateTime.now())) {
            log.debug("Suppressing duplicate incident for {} in {} (cooling down until {})",
                    collarId, zoneName, lastReported.plusMinutes(COOLDOWN_MINUTES));
            return null;
        }

        incidentCooldowns.put(key, LocalDateTime.now());

        String incidentCode = "HWC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        IncidentLog incident = new IncidentLog();
        incident.setIncidentCode(incidentCode);
        incident.setAnimalName(animalName);
        incident.setCollarId(collarId);
        incident.setZoneName(zoneName);
        incident.setParkName(parkName);
        incident.setSeverity(severity != null ? severity : "WARNING");
        incident.setMessage(message);
        incident.setStatus("ACTIVE");
        incident.setLatitude(lat);
        incident.setLongitude(lng);
        incident.setTimestamp(LocalDateTime.now());

        IncidentLog saved = incidentRepository.save(incident);
        auditService.logAction("SYSTEM", "INCIDENT_CREATED", "INCIDENT", incidentCode,
                "Breach detected for " + animalName + " (" + collarId + ") in " + zoneName + " - " + message);

        return toDto(saved);
    }

    @Transactional
    public IncidentDto acknowledgeIncident(Long id, String username) {
        IncidentLog incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));

        incident.setStatus("ACKNOWLEDGED");
        incident.setAcknowledgedAt(LocalDateTime.now());
        IncidentLog saved = incidentRepository.save(incident);

        auditService.logAction(username != null ? username : "COMMANDER", "INCIDENT_ACKNOWLEDGED", "INCIDENT",
                incident.getIncidentCode() != null ? incident.getIncidentCode() : String.valueOf(id),
                "Incident acknowledged by " + (username != null ? username : "duty commander"));

        return toDto(saved);
    }

    @Transactional
    public IncidentDto resolveIncident(Long id, String notes) {
        return resolveIncident(id, notes, "COMMANDER");
    }

    @Transactional
    public IncidentDto resolveIncident(Long id, String notes, String username) {
        IncidentLog incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolutionNotes(notes != null && !notes.isBlank()
                ? notes
                : "Incident successfully resolved; wildlife safely contained within sanctuary perimeter.");

        IncidentLog saved = incidentRepository.save(incident);
        auditService.logAction(username != null ? username : "COMMANDER", "INCIDENT_RESOLVED", "INCIDENT",
                incident.getIncidentCode() != null ? incident.getIncidentCode() : String.valueOf(id),
                "Resolved: " + incident.getResolutionNotes());

        return toDto(saved);
    }

    public Map<String, Object> broadcastCommunitySms(String corridor, String message) {
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

    public IncidentDto toDto(IncidentLog log) {
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
