package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalSubjectHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Centre de la facturation (lot C3) : decisions sur les derogations de remise. */
@Component
@RequiredArgsConstructor
public class DiscountApprovalHandler implements ApprovalSubjectHandler {

    private final ObjectProvider<DiscountApprovalService> discounts;

    @Override
    public String requestType() {
        return DiscountApprovalService.TYPE;
    }

    @Override
    public String actionPath(ApprovalRequest request) {
        return "/billing/documents/" + request.getSubjectRef();
    }

    @Override
    public void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        discounts.getObject().onDecision(request, outcome, DiscountApprovalService.APPROVED);
    }

    @Override
    public void onRejected(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        discounts.getObject().onDecision(request, outcome, DiscountApprovalService.REJECTED);
    }

    @Override
    public void onReturned(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        discounts.getObject().onDecision(request, outcome, "RETURNED");
    }
}
