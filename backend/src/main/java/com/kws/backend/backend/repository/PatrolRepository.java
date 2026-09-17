package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.PatrolUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatrolRepository extends JpaRepository<PatrolUnit, Long> {
    Optional<PatrolUnit> findByCallSign(String callSign);
}
