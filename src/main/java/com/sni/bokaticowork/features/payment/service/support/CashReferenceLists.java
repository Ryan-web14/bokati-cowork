package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.reference.model.ReferenceValue;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Listes preenregistrees de caisse (motifs, banques, types de piece).
 *
 * <p>Depuis le lot L0, ces listes vivent dans le socle commun ({@link ReferenceLists}, table
 * {@code reference_value}, module {@code PAYMENT}) ; cette classe garde les noms des listes de la
 * caisse et les routes {@code /cash-registers/reference-lists} restent les memes.</p>
 */
@Component
@RequiredArgsConstructor
public class CashReferenceLists {

    public static final String SUSPENSION_REASON = "SUSPENSION_REASON";
    public static final String RESPONSIBILITY_TRANSFER_REASON = "RESPONSIBILITY_TRANSFER_REASON";
    public static final String OUT_OF_HOURS_REASON = "OUT_OF_HOURS_REASON";
    public static final String OTHER = ReferenceLists.OTHER;

    private final ReferenceLists lists;

    /**
     * Libelle a enregistrer pour un motif choisi : le libelle de la liste, suivi de la precision
     * s'il y en a une.
     *
     * @throws BadRequestException si le code est absent, inconnu, inactif, ou si la precision manque
     */
    public String describe(String listCode, String code, String text) {
        return lists.describe(listCode, code, text);
    }

    /** Libelle d'un code, ou le code lui-meme s'il n'est plus dans la liste. */
    public String label(String listCode, String code) {
        return lists.label(listCode, code);
    }

    public ReferenceValue require(String listCode, String code) {
        return lists.require(listCode, code);
    }
}
