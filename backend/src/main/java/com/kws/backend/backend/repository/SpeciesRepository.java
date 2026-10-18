package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Long> {
    Optional<Species> findByCommonNameIgnoreCase(String commonName);
    List<Species> findByIucnStatus(String iucnStatus);
}
