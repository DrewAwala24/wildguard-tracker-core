package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.PatrolDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatrolDispatchRepository extends JpaRepository<PatrolDispatch, Long> {
    List<PatrolDispatch> findByStatus(String status);
    List<PatrolDispatch> findByTargetPark(String targetPark);
    List<PatrolDispatch> findByCallSign(String callSign);
    List<PatrolDispatch> findTop20ByOrderByDispatchTimeDesc();
    Optional<PatrolDispatch> findByDispatchCode(String dispatchCode);
}
