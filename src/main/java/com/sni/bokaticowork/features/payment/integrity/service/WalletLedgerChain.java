package com.sni.bokaticowork.features.payment.integrity.service;

import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * L'empreinte d'une ecriture de portefeuille, et de celle qui la precede.
 *
 * <p>Le declencheur de base interdit deja toute mise a jour du journal. Le chainage repond a une
 * autre question : non pas « peut-on modifier » mais « <b>pourrait-on prouver</b> qu'on ne l'a pas
 * fait ». Un administrateur de base de donnees peut desactiver un declencheur ; il ne peut pas
 * recalculer en silence toutes les empreintes qui suivent celle qu'il aurait touchee, parce que
 * chacune depend de la precedente.</p>
 *
 * <p>Le calcul est volontairement fige ici, en un seul endroit : la moindre divergence entre la
 * facon d'ecrire une empreinte et celle de la verifier rendrait toute la chaine fausse, et
 * indistinguable d'une falsification reelle.</p>
 */
public final class WalletLedgerChain {

    /** Maillon d'origine · la premiere ecriture d'un portefeuille n'a rien derriere elle. */
    public static final String GENESIS = "GENESIS";

    private WalletLedgerChain() {
    }

    /**
     * Empreinte d'une ecriture.
     *
     * <p>Y entre tout ce qui fait l'ecriture : sa reference, le portefeuille, le sens, le montant,
     * la devise, la nature, le solde obtenu et l'instant. Le solde resultant y figure parce que
     * c'est lui qu'une falsification chercherait a deplacer · le montant seul se contrefait sans
     * toucher au solde affiche.</p>
     */
    public static String hash(WalletLedgerEntry entry, String previousHash) {
        String payload = String.join("|",
                nullSafe(entry.getEntryNumber()),
                entry.getWallet() == null ? "" : nullSafe(entry.getWallet().getWalletNumber()),
                entry.getDirection() == null ? "" : entry.getDirection().name(),
                entry.getAmount() == null ? "" : entry.getAmount().stripTrailingZeros().toPlainString(),
                nullSafe(entry.getCurrency()),
                entry.getEntryType() == null ? "" : entry.getEntryType().name(),
                entry.getBalanceAfter() == null ? "" : entry.getBalanceAfter().stripTrailingZeros().toPlainString(),
                instant(entry.getCreatedAt()),
                previousHash == null ? GENESIS : previousHash);
        return digest(payload);
    }

    private static String digest(String payload) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // SHA-256 est garanti par la plateforme · si elle manque, le journal ne vaut plus rien.
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }

    private static String instant(Instant moment) {
        return moment == null ? "" : String.valueOf(moment.toEpochMilli());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
