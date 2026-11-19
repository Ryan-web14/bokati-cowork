package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalSubjectHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Lot B4 : centre des reservations, decisions sur les ajustements de prix au-dela du plafond. */
@Component
@RequiredArgsConstructor
public class BookingPriceAdjustmentHandler implements ApprovalSubjectHandler {

    private final ObjectProvider<BookingPriceAdjuster> adjuster;

    @Override
    public String requestType() {
        return BookingPriceAdjuster.TYPE;
    }

    @Override
    public String actionPath(ApprovalRequest request) {
        return "/bookings/" + request.getSubjectRef();
    }

    @Override
    public void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        if (outcome.finalApproval()) {
            adjuster.getObject().onDecision(request.getSubjectRef(), true, outcome.comment());
        }
    }

    @Override
    public void onRejected(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        adjuster.getObject().onDecision(request.getSubjectRef(), false, outcome.comment());
    }

    @Override
    public void onReturned(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
        adjuster.getObject().onDecision(request.getSubjectRef(), false, outcome.comment());
    }
}
