package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lot K1 (S2, decision D2) : « Corriger » une piece validee entierement fausse.
 *
 * <ul>
 *   <li>piece saisie a la main : contre-passation et piece juste en une action, les trois pieces
 *   liees (la piece juste porte {@code correctionOf}, l'originale {@code correctedBy}) ;</li>
 *   <li>piece automatique : la piece juste vient de l'evenement, recomptabilise avec les donnees
 *   relues a la source (pas de lignes saisies : un compte faux se reclasse).</li>
 * </ul>
 *
 * <p>Au-dela du seuil de seconde validation, la contre-passation et la piece juste restent en
 * brouillard jusqu'a la validation d'un autre comptable.</p>
 */
@Service
@RequiredArgsConstructor
public class EntryCorrectionService {

    private final AccountingEntryService entries;
    private final PostingEngine engine;

    @Transactional
    public AccountingResponses.Correction correct(String reference, AccountingRequests.Correct request) {
        AccEntry original = entries.entry(reference);
        if (original.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException(original.getStatus() == EntryStatus.REVERSED
                    ? "La pièce " + original.getEntryNumber() + " est déjà contre-passée par " + original.getReversedByEntry()
                    : "Seule une pièce validée se corrige ; un brouillard ou une pièce en attente se modifie ou se rejette");
        }
        if (original.getPostingEventId() != null) {
            if (request.lines() != null && !request.lines().isEmpty()) {
                throw new BadRequestException("La pièce " + original.getEntryNumber() + " est automatique : la pièce juste vient de l'événement, "
                        + "recomptabilisé avec les données à jour (corrigez d'abord la source ou le schéma). Pour changer un compte ou un tiers, utilisez « Reclasser »");
            }
            PostingDtos.RepostResult result = engine.repost(original.getPostingEventId(), request.reason(), request.date());
            AccountingResponses.Entry reversal = result.reversal() == null ? null : entries.get(result.reversal());
            AccountingResponses.Entry correction = result.newEntry() == null ? null : entries.get(result.newEntry());
            return new AccountingResponses.Correction(reversal, correction, result.event(), List.of(result.message()));
        }
        LocalDate date = request.date() != null ? request.date() : entries.correctionDate(original, original.getJournalCode());
        AccountingResponses.Entry reversal = entries.reverse(original.getEntryNumber(), new AccountingRequests.Reverse(request.reason(), date));
        AccountingResponses.Entry correction = entries.createCorrection(entries.entry(original.getEntryNumber()), request, date);
        List<String> messages = new ArrayList<>(reversal.messages());
        messages.addAll(correction.messages());
        return new AccountingResponses.Correction(reversal, correction, null, messages);
    }
}
