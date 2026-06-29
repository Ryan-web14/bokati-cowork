package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.DocumentSequence;
import com.sni.bokaticowork.features.billing.repository.DocumentSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DocumentSequenceService {

    private final DocumentSequenceRepository sequenceRepository;

    /**
     * Code machine DGID (5 chars). Fallback "00000" si non configuré.
     * Exemple SEFC : CBFOS
     */
    @Value("${app.company.machine-code:00000}")
    private String machineCode;

    /**
     * Génère le prochain numéro fiscal SEFC.
     * Format : FAC20260617-00001abc-BKTWK
     *
     * SELECT FOR UPDATE sérialise les validations concurrentes pour éviter
     * les doublons de numéros (numérotation continue garantie).
     */
    @Transactional
    public String nextFiscalNumber(BillingDocumentType type, LocalDate fiscalDate) {
        int year = fiscalDate.getYear();
        DocumentSequence seq = sequenceRepository
                .findLockedByTypeAndYear(type.name(), year)
                .orElseGet(() -> createSequence(type, year));

        long next = seq.getCurrentValue() + 1;
        seq.setCurrentValue(next);
        seq.setUpdatedAt(Instant.now());
        sequenceRepository.save(seq);

        String machine = (machineCode != null && !machineCode.isBlank())
                ? machineCode.toUpperCase()
                : "00000";
        String nonce = computeNonce(type, fiscalDate, next, machine);

        // FAC20260617-00001abc-BKTWK
        return String.format("%s%s-%05d%s-%s",
                seq.getPrefix(),
                fiscalDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                next,
                nonce,
                machine);
    }

    /**
     * Nonce anti-contrefaçon de 3 chars base36 (déterministe, vérifiable sans état).
     * Dérivé de SHA-256(type + date + séq + machine).
     */
    private String computeNonce(BillingDocumentType type, LocalDate date, long seq, String machine) {
        String payload = type.name() + date + seq + machine;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            int val = ((hash[0] & 0xFF) << 16 | (hash[1] & 0xFF) << 8 | (hash[2] & 0xFF))
                      % (36 * 36 * 36);
            String encoded = Integer.toString(val, 36);
            return String.format("%3s", encoded).replace(' ', '0');
        } catch (NoSuchAlgorithmException e) {
            return "000";
        }
    }

    private DocumentSequence createSequence(BillingDocumentType type, int year) {
        String prefix = switch (type) {
            case QUOTE            -> "DEV";
            case INVOICE          -> "FAC";
            case CREDIT_NOTE      -> "AVR";
            case DEBIT_NOTE       -> "DBN";
            case PROFORMA_INVOICE -> "PRF";
        };
        DocumentSequence seq = new DocumentSequence();
        seq.setDocumentType(type.name());
        seq.setYear(year);
        seq.setPrefix(prefix);
        seq.setCurrentValue(0L);
        seq.setCreatedAt(Instant.now());
        seq.setUpdatedAt(Instant.now());
        return sequenceRepository.save(seq);
    }
}
