package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassEventRepository extends JpaRepository<PassEvent, Long> {

    List<PassEvent> findAllByPassOrderByOccurredAtDesc(Pass pass);
}
