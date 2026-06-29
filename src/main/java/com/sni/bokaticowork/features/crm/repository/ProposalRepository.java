package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import com.sni.bokaticowork.features.crm.model.CommercialProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<CommercialProposal, Long>, JpaSpecificationExecutor<CommercialProposal> {

    Optional<CommercialProposal> findByProposalNumber(String number);

    List<CommercialProposal> findAllByOpportunityId(Long opportunityId);

    List<CommercialProposal> findAllByStatus(ProposalStatus status);
}
