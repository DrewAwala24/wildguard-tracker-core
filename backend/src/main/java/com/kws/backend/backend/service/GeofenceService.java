package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.GeofenceDto;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.repository.GeofenceRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeofenceService {
    private final GeofenceRepository geofenceRepository;

    public List<GeofenceZone> getAllZones() {
        return geofenceRepository.findAll();
    }

    public List<GeofenceDto> getAllZoneDtos() {
        return geofenceRepository.findAll().stream().map(zone -> {
            GeofenceDto dto = new GeofenceDto();
            dto.setId(zone.getId());
            dto.setZoneName(zone.getZoneName());
            dto.setZoneType(zone.getZoneType());
            if (zone.getBoundary() != null) {
                List<double[]> coords = new ArrayList<>();
                for (Coordinate c : zone.getBoundary().getCoordinates()) {
                    // Leaflet expects [lat, lng] format
                    coords.add(new double[]{c.getY(), c.getX()});
                }
                dto.setCoordinates(coords);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public GeofenceZone saveZone(GeofenceZone zone) {
        return geofenceRepository.save(zone);
    }
}