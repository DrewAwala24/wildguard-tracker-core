package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.EcologicalLandmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EcologicalLandmarkRepository extends JpaRepository<EcologicalLandmark, Long> {
    List<EcologicalLandmark> findByParkNameIgnoreCase(String parkName);

    @Query(value = "SELECT * FROM ecological_landmarks WHERE ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters) = true", nativeQuery = true)
    List<EcologicalLandmark> findLandmarksNearPoint(@Param("longitude") double longitude, @Param("latitude") double latitude, @Param("radiusMeters") double radiusMeters);
}
