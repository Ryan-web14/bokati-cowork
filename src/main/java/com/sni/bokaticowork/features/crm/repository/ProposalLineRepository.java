package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.model.ProposalLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalLineRepository extends JpaRepository<ProposalLineItem, Long> {

    List<ProposalLineItem> findAllByProposalIdOrderBySortOrder(Long proposalId);
}
