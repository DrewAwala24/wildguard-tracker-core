package com.kws.backend.backend.service;

import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.repository.GeofenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeofenceService {
    private final GeofenceRepository geofenceRepository;

    public List<GeofenceZone> getAllZones() {
        return geofenceRepository.findAll();
    }

    public GeofenceZone saveZone(GeofenceZone zone) {
        return geofenceRepository.save(zone);
    }
}