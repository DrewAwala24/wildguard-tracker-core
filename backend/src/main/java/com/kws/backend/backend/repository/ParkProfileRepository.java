package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.ParkProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkProfileRepository extends JpaRepository<ParkProfile, Long> {
    Optional<ParkProfile> findByParkNameIgnoreCase(String parkName);
    Optional<ParkProfile> findByCodeIgnoreCase(String code);
}
