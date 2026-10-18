package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.IncidentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentLog, Long> {
    List<IncidentLog> findByStatusOrderByTimestampDesc(String status);
    List<IncidentLog> findByParkNameIgnoreCaseOrderByTimestampDesc(String parkName);
    List<IncidentLog> findBySeverityOrderByTimestampDesc(String severity);
    List<IncidentLog> findTop25ByOrderByTimestampDesc();

    @Query("SELECT i FROM IncidentLog i WHERE i.status IN ('ACTIVE', 'DISPATCHED', 'ACKNOWLEDGED') ORDER BY i.timestamp DESC")
    List<IncidentLog> findOpenIncidents();

    long countByStatus(String status);
}
