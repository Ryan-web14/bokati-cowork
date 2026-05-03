package com.sni.bokaticowork.features.visitor.repository;

import com.sni.bokaticowork.features.visitor.model.VisitorCheckIn;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorCheckInRepository extends JpaRepository<VisitorCheckIn, Long> {
    Optional<VisitorCheckIn> findFirstByPassAndCheckedOutAtIsNullOrderByCheckedInAtDesc(VisitorPass pass);
    List<VisitorCheckIn> findAllByOrderByCheckedInAtDesc();
}
