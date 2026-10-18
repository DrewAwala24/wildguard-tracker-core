package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.TelemetryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryLocation, Long> {
    List<TelemetryLocation> findByAnimal_IdOrderByTimestampDesc(Long animalId);
    List<TelemetryLocation> findTop15ByAnimal_IdOrderByTimestampDesc(Long animalId);
    List<TelemetryLocation> findTop25ByAnimal_IdOrderByTimestampDesc(Long animalId);
    List<TelemetryLocation> findTop100ByOrderByTimestampDesc();
    Optional<TelemetryLocation> findTop1ByAnimal_IdOrderByTimestampDesc(Long animalId);

    @Query("SELECT t FROM TelemetryLocation t WHERE t.timestamp >= :since ORDER BY t.timestamp DESC")
    List<TelemetryLocation> findRecentTelemetry(@Param("since") LocalDateTime since);

    @Query(value = "SELECT DISTINCT ON (animal_id) * FROM telemetry_locations ORDER BY animal_id, timestamp DESC", nativeQuery = true)
    List<TelemetryLocation> findLatestFixPerAnimal();

    long countByTimestampAfter(LocalDateTime timestamp);
}