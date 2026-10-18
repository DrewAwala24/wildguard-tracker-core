package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.model.IncidentLog;
import com.kws.backend.backend.model.PatrolDispatch;
import com.kws.backend.backend.model.PatrolUnit;
import com.kws.backend.backend.repository.IncidentRepository;
import com.kws.backend.backend.repository.PatrolDispatchRepository;
import com.kws.backend.backend.repository.PatrolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatrolService {

    private final PatrolRepository patrolRepository;
    private final PatrolDispatchRepository patrolDispatchRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public List<PatrolUnitDto> getAllPatrols() {
        return patrolRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PatrolUnit> getAllPatrolEntities() {
        return patrolRepository.findAll();
    }

    public List<PatrolDispatch> getAllDispatches() {
        return patrolDispatchRepository.findTop20ByOrderByDispatchTimeDesc();
    }

    @Transactional
    public PatrolDispatch createDispatch(Long unitId, Long incidentId, Double targetLat, Double targetLng, String park, String notes, String commander) {
        PatrolUnit unit = patrolRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Patrol unit not found: " + unitId));

        if ("DISPATCHED".equalsIgnoreCase(unit.getStatus()) || "MAINTENANCE".equalsIgnoreCase(unit.getStatus())) {
            throw new IllegalStateException("Patrol unit " + unit.getCallSign() + " is currently " + unit.getStatus() + " and unavailable for new dispatch.");
        }

        unit.setStatus("DISPATCHED");
        unit.setLastStatusUpdate(LocalDateTime.now());
        patrolRepository.save(unit);

        String dispatchCode = "DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PatrolDispatch dispatch = new PatrolDispatch();
        dispatch.setDispatchCode(dispatchCode);
        dispatch.setPatrolUnit(unit);
        dispatch.setCallSign(unit.getCallSign());
        dispatch.setTargetPark(park != null ? park : unit.getSector());
        dispatch.setTargetLatitude(targetLat != null ? targetLat : unit.getLatitude());
        dispatch.setTargetLongitude(targetLng != null ? targetLng : unit.getLongitude());
        dispatch.setStatus("DISPATCHED");
        dispatch.setCommanderUsername(commander != null ? commander : "COMMANDER");
        dispatch.setDispatchTime(LocalDateTime.now());
        dispatch.setNotes(notes);

        if (incidentId != null) {
            incidentRepository.findById(incidentId).ifPresent(incident -> {
                dispatch.setIncident(incident);
                incident.setStatus("DISPATCHED");
                incident.setDispatchedUnit(unit.getCallSign());
                incidentRepository.save(incident);
            });
        }

        PatrolDispatch savedDispatch = patrolDispatchRepository.save(dispatch);

        auditService.logAction(commander, "PATROL_DISPATCHED", "PATROL_UNIT", unit.getCallSign(),
                "Dispatched " + unit.getName() + " to " + dispatch.getTargetPark() + " [code: " + dispatchCode + "]");

        return savedDispatch;
    }

    @Transactional
    public PatrolUnitDto dispatchPatrol(Long id, Double targetLat, Double targetLng) {
        PatrolUnit unit = patrolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrol unit not found: " + id));

        createDispatch(id, null, targetLat, targetLng, unit.getSector(), "Rapid response dispatch", "COMMANDER");
        return toDto(unit);
    }

    @Transactional
    public PatrolUnitDto updateStatus(Long id, String status) {
        PatrolUnit unit = patrolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrol unit not found: " + id));
        String oldStatus = unit.getStatus();
        unit.setStatus(status);
        unit.setLastStatusUpdate(LocalDateTime.now());
        PatrolUnit updated = patrolRepository.save(unit);

        auditService.logAction("SYSTEM", "PATROL_STATUS_UPDATED", "PATROL_UNIT", unit.getCallSign(),
                "Status changed from " + oldStatus + " to " + status);
        return toDto(updated);
    }

    @Transactional
    public PatrolUnit saveOrUpdatePatrolUnit(PatrolUnit unit, String user) {
        boolean isNew = (unit.getId() == null);
        PatrolUnit saved = patrolRepository.save(unit);
        auditService.logAction(user, isNew ? "PATROL_CREATED" : "PATROL_UPDATED", "PATROL_UNIT", saved.getCallSign(),
                "Patrol unit " + saved.getName() + " saved with status " + saved.getStatus());
        return saved;
    }

    public PatrolUnitDto toDto(PatrolUnit unit) {
        PatrolUnitDto dto = new PatrolUnitDto();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setCallSign(unit.getCallSign());
        dto.setUnitType(unit.getUnitType());
        dto.setLatitude(unit.getLatitude());
        dto.setLongitude(unit.getLongitude());
        dto.setStatus(unit.getStatus());
        dto.setSector(unit.getSector());
        return dto;
    }
}
