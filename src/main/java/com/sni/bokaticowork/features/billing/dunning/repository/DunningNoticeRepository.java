package com.sni.bokaticowork.features.billing.dunning.repository;

import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DunningNoticeRepository extends JpaRepository<DunningNotice, Long> {

    boolean existsByDocumentNumberAndStep_Id(String documentNumber, Long stepId);

    List<DunningNotice> findByDocumentNumberOrderByExecutedAtDesc(String documentNumber);

    List<DunningNotice> findByCustomerTypeAndCustomerCodeOrderByExecutedAtDesc(String customerType, String customerCode);

    List<DunningNotice> findTop200ByOrderByExecutedAtDesc();
}
