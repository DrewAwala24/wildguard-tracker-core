package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.PatrolUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatrolRepository extends JpaRepository<PatrolUnit, Long> {
    Optional<PatrolUnit> findByCallSign(String callSign);
    List<PatrolUnit> findByStatus(String status);
    List<PatrolUnit> findBySectorIgnoreCase(String sector);
    List<PatrolUnit> findByStatusAndSectorIgnoreCase(String status, String sector);
    long countByStatus(String status);
}
