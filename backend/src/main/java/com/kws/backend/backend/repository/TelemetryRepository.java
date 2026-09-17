package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.TelemetryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryLocation, Long> {
    List<TelemetryLocation> findByAnimalIdOrderByTimestampDesc(Long animalId);
    List<TelemetryLocation> findTop15ByAnimalIdOrderByTimestampDesc(Long animalId);
    Optional<TelemetryLocation> findTop1ByAnimalIdOrderByTimestampDesc(Long animalId);
}