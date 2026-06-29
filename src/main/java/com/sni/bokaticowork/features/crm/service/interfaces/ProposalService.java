package com.sni.bokaticowork.features.crm.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.crm.dto.ProposalDtos.*;
import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import org.springframework.data.domain.Pageable;

public interface ProposalService {

    ProposalResponse create(CreateProposalRequest request);

    ProposalResponse update(Long id, UpdateProposalRequest request);

    ProposalResponse get(Long id);

    ProposalResponse getByNumber(String proposalNumber);

    PaginatedResponse<ProposalResponse> list(ProposalStatus status, Long opportunityId, String searchText, Pageable pageable);

    ProposalResponse addLine(Long proposalId, AddProposalLineRequest request);

    ProposalResponse updateLine(Long proposalId, Long lineId, UpdateProposalLineRequest request);

    ProposalResponse removeLine(Long proposalId, Long lineId);

    ProposalResponse send(Long id);

    ProposalResponse accept(Long id);

    ProposalResponse reject(Long id, RejectProposalRequest request);
}
