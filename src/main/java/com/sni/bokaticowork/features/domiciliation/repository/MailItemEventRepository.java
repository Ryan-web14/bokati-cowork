package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.MailItemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailItemEventRepository extends JpaRepository<MailItemEvent, Long> {

    List<MailItemEvent> findByMailItem_IdOrderByOccurredAtAsc(Long mailItemId);
}
