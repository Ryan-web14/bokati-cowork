package com.sni.bokaticowork.features.payment.dto.request;

/**
 * Un motif choisi dans une liste, avec sa precision (obligatoire pour « Autre »), et une note.
 */
public record CashReasonRequest(String reasonCode, String reasonText, String note) {
}
