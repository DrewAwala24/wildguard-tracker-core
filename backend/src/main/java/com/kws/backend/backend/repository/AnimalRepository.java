package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Optional<Animal> findByCollarId(String collarId);
    List<Animal> findByParkNameIgnoreCase(String parkName);
    List<Animal> findBySpeciesIgnoreCase(String species);
    List<Animal> findByIucnStatus(String iucnStatus);
    List<Animal> findByActiveTrue();
    long countByActiveTrue();
}