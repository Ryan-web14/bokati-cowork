package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class FiscalHashService {

    public static final String ALGORITHM  = "SHA-256";
    public static final String GENESIS    = "GENESIS";

    /**
     * Calcule le hash SHA-256 d'une facture à partir de ses données immuables
     * et du hash de la facture précédente (chaînage).
     *
     * Payload : fiscalNumber|fiscalDate|customerCode|totalAmount|previousHash
     *
     * Appelé exclusivement dans validate(), dans la même transaction que le save().
     */
    public String compute(BillingDocument doc, String previousHash) {
        String payload = String.join("|",
                doc.getFiscalNumber(),
                doc.getFiscalDate().toString(),
                doc.getCustomerCode(),
                doc.getTotalAmount().toPlainString(),
                previousHash == null ? GENESIS : previousHash
        );
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITHM)
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
