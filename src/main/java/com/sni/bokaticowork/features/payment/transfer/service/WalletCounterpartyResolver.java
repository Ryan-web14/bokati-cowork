package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Retrouve le portefeuille d'un autre abonne a partir de ce que l'emetteur connait de lui.
 *
 * <p>Un numero de portefeuille, un telephone ou un courriel · les trois se distinguent a la forme,
 * aucun champ « type » n'est demande. Le destinataire retrouve est un abonne, jamais un compte
 * interne : un transfert entre abonnes ne doit pas pouvoir viser la caisse de l'etablissement, qui a
 * ses propres chemins de paiement avec facture en face.</p>
 *
 * <p>Ce qu'on renvoie du destinataire est volontairement mince : un nom d'affichage, pas son
 * telephone ni son courriel. Chercher par numero doit confirmer une personne qu'on connait deja,
 * pas permettre d'en decouvrir une.</p>
 */
@Component
@RequiredArgsConstructor
public class WalletCounterpartyResolver {

    private static final String MEMBER = "MEMBER";

    private final WalletAccountRepository walletRepository;
    private final WalletService walletService;
    private final MemberRepository memberRepository;
    private final TransactionContextResolver contextResolver;
    private final com.sni.bokaticowork.core.utils.phone.PhoneNumberService phoneNumberService;

    /** Ce qu'on montre a l'emetteur avant qu'il ne confirme · assez pour reconnaitre, pas pour decouvrir. */
    public record Counterparty(WalletAccount wallet, String displayName) {
    }

    public Counterparty resolve(String reference, String currency) {
        if (!StringUtils.hasText(reference)) {
            throw new BadRequestException("Indiquez le destinataire · numéro de portefeuille, téléphone ou courriel");
        }
        String value = reference.trim();

        Optional<WalletAccount> direct = walletRepository.findByWalletNumber(value);
        if (direct.isPresent()) {
            return describe(requireMember(direct.get()));
        }

        Optional<Member> member = value.contains("@")
                ? memberRepository.findByEmailIgnoreCaseAndDeletedFalse(value)
                : memberRepository.findByPhoneAndDeletedFalse(phoneNumberService.normalizeForLookup(value));
        Member found = member.orElseThrow(() -> new ResourceNotFoundException(
                "Aucun abonné ne correspond à « " + value + " »"));

        // Le portefeuille du destinataire est ouvert s'il n'existe pas encore · recevoir ne demande
        // aucune demarche de sa part, c'est l'emetteur qui fait l'effort.
        WalletAccount wallet = walletRepository.findByOwnerAndCurrency(MEMBER, found.getMemberId(), currency)
                .orElseGet(() -> walletService.serviceWallet(
                        walletService.getOrCreate(MEMBER, found.getMemberId(), currency).walletNumber()));
        return describe(wallet);
    }

    private WalletAccount requireMember(WalletAccount wallet) {
        if (!MEMBER.equalsIgnoreCase(wallet.getOwnerType())) {
            throw new BadRequestException("Ce portefeuille n'est pas celui d'un abonné");
        }
        return wallet;
    }

    private Counterparty describe(WalletAccount wallet) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
        String name = StringUtils.hasText(party.name()) ? party.name() : wallet.getWalletNumber();
        return new Counterparty(wallet, name);
    }

}
