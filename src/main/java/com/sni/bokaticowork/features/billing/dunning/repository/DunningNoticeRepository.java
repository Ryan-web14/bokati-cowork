package com.sni.bokaticowork.features.billing.dunning.repository;

import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DunningNoticeRepository extends JpaRepository<DunningNotice, Long> {

    boolean existsByDocumentNumberAndStep_Id(String documentNumber, Long stepId);

    /** L'etape est lue avec la relance : sa vue la lit hors transaction (open-in-view coupe). */
    @EntityGraph(attributePaths = "step")
    List<DunningNotice> findByDocumentNumberOrderByExecutedAtDesc(String documentNumber);

    @EntityGraph(attributePaths = "step")
    List<DunningNotice> findByCustomerTypeAndCustomerCodeOrderByExecutedAtDesc(String customerType, String customerCode);

    @EntityGraph(attributePaths = "step")
    List<DunningNotice> findTop200ByOrderByExecutedAtDesc();
}
