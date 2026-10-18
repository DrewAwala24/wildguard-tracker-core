package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.WeatherObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {
    List<WeatherObservation> findByParkNameIgnoreCase(String parkName);

    @Query(value = "SELECT DISTINCT ON (park_name) * FROM weather_observations ORDER BY park_name, observation_time DESC", nativeQuery = true)
    List<WeatherObservation> findLatestObservationPerPark();

    Optional<WeatherObservation> findTop1ByParkNameIgnoreCaseOrderByObservationTimeDesc(String parkName);
}
