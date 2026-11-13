package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Envoi du releve d'un client par courriel, PDF joint (lot B9, destinataires comme au lot B4). */
public record SendStatementRequest(@NotEmpty List<String> to, List<String> cc, List<String> bcc, LocalDate from, LocalDate until,
                                   @Size(max = 2000) String message) {
}
