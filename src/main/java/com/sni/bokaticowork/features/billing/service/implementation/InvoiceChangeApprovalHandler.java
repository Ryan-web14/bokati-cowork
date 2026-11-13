package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalSubjectHandler;
import com.sni.bokaticowork.features.billing.enums.BillingChangeRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Centre de la facturation (lots B14 et B15) : decisions sur les modifications de facture. */
@Component
@RequiredArgsConstructor
public class InvoiceChangeApprovalHandler implements ApprovalSubjectHandler {

    private final ObjectProvider<InvoiceChangeService> changes;

    @Override
    public String requestType() {
        return InvoiceChangeService.TYPE;
    }

    @Override
    public String actionPath(ApprovalRequest request) {
        return "/billing/invoices/" + request.getSubjectRef();
    }

    @Override
    public void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        changes.getObject().onApproved(request, outcome);
    }

    @Override
    public void onRejected(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        changes.getObject().onClosed(request, outcome, BillingChangeRequestStatus.REJECTED);
    }

    @Override
    public void onReturned(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        changes.getObject().onClosed(request, outcome, BillingChangeRequestStatus.RETURNED);
    }
}
