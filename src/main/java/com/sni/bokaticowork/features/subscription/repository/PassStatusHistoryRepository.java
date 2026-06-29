package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassStatusHistoryRepository extends JpaRepository<PassStatusHistory, Long> {

    List<PassStatusHistory> findAllByPassOrderByChangedAtDesc(Pass pass);
}
