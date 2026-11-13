package com.sni.bokaticowork.features.fund.service;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalSubjectHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Centre de la caisse (lot B15) : une demande de fonds decidee par les routes communes
 * {@code /approvals/...} produit le meme effet que par les routes de la caisse.
 */
@Component
@RequiredArgsConstructor
public class FundApprovalHandler implements ApprovalSubjectHandler {

    private final ObjectProvider<FundRequestService> requests;

    @Override
    public String requestType() {
        return FundRequestService.APPROVAL_TYPE;
    }

    @Override
    public String actionPath(ApprovalRequest request) {
        return FundNotifier.PATH + request.getSubjectRef();
    }

    @Override
    public void beforeDecision(ApprovalRequest request, String actor) {
        requests.getObject().assertNotBeneficiary(request.getSubjectRef(), actor);
    }

    @Override
    public void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        requests.getObject().applyApproval(request.getSubjectRef(), outcome);
    }

    @Override
    public void onRejected(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        requests.getObject().applyRejection(request.getSubjectRef(), outcome);
    }

    @Override
    public void onReturned(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        requests.getObject().applyReturn(request.getSubjectRef(), outcome);
    }
}
