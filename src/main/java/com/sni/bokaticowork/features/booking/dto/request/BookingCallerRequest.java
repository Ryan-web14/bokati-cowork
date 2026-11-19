package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Lot B5 (decision D5) : rendez-vous pris au telephone par l'equipe.
 *
 * <p>{@code callNote} : note d'appel gardee sur la reservation et sur la fiche du prospect.
 * Un appelant qui n'est ni membre, ni client, ni entreprise devient un prospect du CRM (source
 * telephone), relie a la reservation, sauf {@code createLead: false} ; {@code leadNumber} relie un
 * prospect existant. Le prospect est converti a la venue ou au paiement.</p>
 */
public record BookingCallerRequest(
        @Size(max = 160) String fullName,
        @Size(max = 40) String phone,
        @Email @Size(max = 160) String email,
        @Size(max = 160) String company,
        @Size(max = 1000) String callNote,
        @Size(max = 80) String leadNumber,
        Boolean createLead
) {
}
