package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<GeofenceZone, Long> {

    @Query(value = "SELECT * FROM geofence_zones WHERE ST_Intersects(boundary, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) = true", nativeQuery = true)
    List<GeofenceZone> findZonesContainingPoint(@Param("longitude") double longitude, @Param("latitude") double latitude);

    @Query(value = "SELECT * FROM geofence_zones WHERE ST_DWithin(boundary::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :distanceMeters) = true", nativeQuery = true)
    List<GeofenceZone> findZonesNearPoint(@Param("longitude") double longitude, @Param("latitude") double latitude, @Param("distanceMeters") double distanceMeters);

    @Query(value = "SELECT MIN(ST_Distance(boundary::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)) FROM geofence_zones WHERE LOWER(zone_name) = LOWER(:zoneName)", nativeQuery = true)
    Double findDistanceToZone(@Param("zoneName") String zoneName, @Param("longitude") double longitude, @Param("latitude") double latitude);

    List<GeofenceZone> findByZoneType(String zoneType);

    Optional<GeofenceZone> findByZoneNameIgnoreCase(String zoneName);
}