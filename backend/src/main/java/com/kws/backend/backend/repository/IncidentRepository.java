package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.IncidentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentLog, Long> {
    List<IncidentLog> findByStatusOrderByTimestampDesc(String status);
}
