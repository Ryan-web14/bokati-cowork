package com.sni.bokaticowork.features.visitor.repository;

import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VisitorPassRepository extends JpaRepository<VisitorPass, Long> {
    Optional<VisitorPass> findByPassNumber(String passNumber);
    List<VisitorPass> findAllByStatus(VisitorPassStatus status);
    List<VisitorPass> findAllByValidFromBeforeAndValidUntilAfter(Instant end, Instant start);
}
