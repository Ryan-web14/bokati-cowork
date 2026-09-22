package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PawapayCallbackRepository extends JpaRepository<PawapayCallback, Long> {

    List<PawapayCallback> findByReferenceIdOrderByReceivedAtDesc(String referenceId);

    Page<PawapayCallback> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<PawapayCallback> findByOutcomeOrderByReceivedAtDesc(PawapayCallback.Outcome outcome, Pageable pageable);
}
