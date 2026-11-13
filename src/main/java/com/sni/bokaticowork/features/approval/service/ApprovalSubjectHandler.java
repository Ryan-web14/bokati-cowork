package com.sni.bokaticowork.features.approval.service;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;

/**
 * Ce qu'un centre fait de la decision prise sur un de ses objets (lot B15). Appele quand la
 * decision passe par les routes communes {@code /approvals/...} ; un centre qui decide par ses
 * propres routes traite le resultat lui-meme.
 */
public interface ApprovalSubjectHandler {

    /** Type de demande traite (FUND_REQUEST, INVOICE_CHANGE...). */
    String requestType();

    /** Chemin de l'ecran de l'objet, pour les notifications. */
    default String actionPath(ApprovalRequest request) {
        return "/approvals/" + request.getNumber();
    }

    /** Controles propres au centre avant une decision (par exemple : jamais sur sa propre avance). */
    default void beforeDecision(ApprovalRequest request, String actor) {
    }

    /** Un ou plusieurs niveaux approuves ; {@code outcome.finalApproval()} si la demande est approuvee. */
    default void onApproved(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
    }

    default void onRejected(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
    }

    default void onReturned(ApprovalRequest request, ApprovalDtos.Outcome outcome) {
    }
}
