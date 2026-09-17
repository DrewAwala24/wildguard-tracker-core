package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.model.PatrolUnit;
import com.kws.backend.backend.repository.PatrolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatrolService {

    private final PatrolRepository patrolRepository;

    public List<PatrolUnitDto> getAllPatrols() {
        return patrolRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PatrolUnitDto dispatchPatrol(Long id, Double targetLat, Double targetLng) {
        PatrolUnit unit = patrolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrol unit not found: " + id));

        unit.setStatus("DISPATCHED");
        // Optionally update vehicle heading / intermediate position towards target
        if (targetLat != null && targetLng != null) {
            log.info("Dispatching patrol {} ({}) towards target [{}, {}]", unit.getName(), unit.getCallSign(), targetLat, targetLng);
        }
        PatrolUnit updated = patrolRepository.save(unit);
        return toDto(updated);
    }

    public PatrolUnitDto updateStatus(Long id, String status) {
        PatrolUnit unit = patrolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patrol unit not found: " + id));
        unit.setStatus(status);
        return toDto(patrolRepository.save(unit));
    }

    private PatrolUnitDto toDto(PatrolUnit unit) {
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
