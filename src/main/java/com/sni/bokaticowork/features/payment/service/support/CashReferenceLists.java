package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.model.CashReferenceValue;
import com.sni.bokaticowork.features.payment.repository.CashReferenceValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Lecture des listes preenregistrees de caisse (motifs, banques, types de piece).
 *
 * <p>Un motif choisi est verifie ici : il doit exister et etre actif ; la valeur « Autre,
 * preciser » exige un texte. Chaque emploi est compte, ce qui fait remonter les valeurs les plus
 * utilisees et signale a l'administrateur les « Autre » frequents.</p>
 */
@Component
@RequiredArgsConstructor
public class CashReferenceLists {

    public static final String SUSPENSION_REASON = "SUSPENSION_REASON";
    public static final String RESPONSIBILITY_TRANSFER_REASON = "RESPONSIBILITY_TRANSFER_REASON";
    public static final String OUT_OF_HOURS_REASON = "OUT_OF_HOURS_REASON";
    public static final String OTHER = "OTHER";

    private final CashReferenceValueRepository repository;

    /**
     * Libelle a enregistrer pour un motif choisi : le libelle de la liste, suivi de la precision
     * s'il y en a une.
     *
     * @throws BadRequestException si le code est absent, inconnu, inactif, ou si la precision manque
     */
    public String describe(String listCode, String code, String text) {
        CashReferenceValue value = require(listCode, code);
        String precision = StringUtils.hasText(text) ? text.trim() : null;
        if (Boolean.TRUE.equals(value.getRequiresText()) && precision == null) {
            throw new BadRequestException("Precisez le motif « " + value.getLabel() + " »");
        }
        value.setUsageCount((value.getUsageCount() == null ? 0L : value.getUsageCount()) + 1);
        repository.save(value);
        return precision == null ? value.getLabel() : value.getLabel() + " : " + precision;
    }

    /** Libelle d'un code, ou le code lui-meme s'il n'est plus dans la liste. */
    public String label(String listCode, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return repository.findByListCodeAndCode(listCode, code.trim().toUpperCase(Locale.ROOT))
                .map(CashReferenceValue::getLabel)
                .orElse(code);
    }

    public CashReferenceValue require(String listCode, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Choisissez un motif dans la liste");
        }
        CashReferenceValue value = repository.findByListCodeAndCode(listCode, code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Motif inconnu : " + code));
        if (!Boolean.TRUE.equals(value.getActive())) {
            throw new BadRequestException("Le motif « " + value.getLabel() + " » n'est plus propose");
        }
        return value;
    }
}
