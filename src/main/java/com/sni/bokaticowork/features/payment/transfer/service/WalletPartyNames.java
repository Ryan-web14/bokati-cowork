package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Le nom d'affichage d'un portefeuille · celui qu'on montre a l'autre partie.
 *
 * <p>Un numero de portefeuille ne dit rien a personne : « WLT-00000004 » ne permet ni de
 * reconnaitre le destinataire avant de confirmer, ni de relire son releve ensuite. Le nom est donc
 * rendu partout ou le numero l'est.</p>
 *
 * <p>Ce qu'on rend reste mince, comme a la resolution : un nom d'affichage, jamais le telephone ni
 * le courriel · reconnaitre une personne qu'on connait, pas en decouvrir une. Faute de nom, le
 * numero fait office · on ne rend jamais une case vide.</p>
 */
@Component
@RequiredArgsConstructor
public class WalletPartyNames {

    private final TransactionContextResolver contextResolver;

    @Transactional(readOnly = true)
    public String of(WalletAccount wallet) {
        if (wallet == null) {
            return null;
        }
        TransactionContextResolver.PartyView party =
                contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
        return StringUtils.hasText(party.name()) ? party.name() : wallet.getWalletNumber();
    }

    /**
     * Une memoire le temps d'une reponse · une page de vingt transferts cite souvent les memes
     * destinataires, et chaque nom coute une lecture.
     */
    public Lookup lookup() {
        return new Lookup(this);
    }

    public static final class Lookup {

        private final WalletPartyNames names;
        private final Map<Long, String> seen = new HashMap<>();

        private Lookup(WalletPartyNames names) {
            this.names = names;
        }

        public String of(WalletAccount wallet) {
            if (wallet == null) {
                return null;
            }
            if (wallet.getId() == null) {
                return names.of(wallet);
            }
            return seen.computeIfAbsent(wallet.getId(), id -> names.of(wallet));
        }
    }
}
