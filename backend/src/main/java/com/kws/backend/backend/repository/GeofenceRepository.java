package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeofenceRepository extends JpaRepository<GeofenceZone, Long> {

    @Query(value = "SELECT * FROM geofence_zones WHERE ST_Intersects(boundary, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) = true", nativeQuery = true)
    List<GeofenceZone> findZonesContainingPoint(@Param("longitude") double longitude, @Param("latitude") double latitude);
}