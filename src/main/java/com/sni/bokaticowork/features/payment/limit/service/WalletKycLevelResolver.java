package com.sni.bokaticowork.features.payment.limit.service;

import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Traduit un dossier de verification en niveau de plafond.
 *
 * <p>Le projet n'a pas de « niveau de KYC » numerote : il a un dossier, avec un statut. La
 * numerotation vit ici et nulle part ailleurs · plutot que d'ajouter une colonne qui pourrait
 * diverger du dossier reel, on la calcule depuis lui, ce qui rend impossible d'avoir un niveau 2
 * sans dossier approuve.</p>
 *
 * <p>Deux niveaux seulement se gagnent automatiquement : le premier pour tout le monde, le second
 * une fois le dossier approuve. Le troisieme ne se deduit d'aucun statut · il s'accorde, en posant
 * une politique nommee sur le portefeuille. C'est volontaire : au-dela d'un certain montant la
 * decision engage l'etablissement, et elle doit porter le nom de quelqu'un plutot que de tomber
 * d'un calcul.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletKycLevelResolver {

    private static final int BASE_LEVEL = 1;
    private static final int VERIFIED_LEVEL = 2;

    private final KycCaseRepository kycCaseRepository;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;

    /** Niveau du titulaire · le plancher en l'absence de tout dossier. */
    @Transactional(readOnly = true)
    public int levelOf(WalletAccount wallet) {
        if (wallet == null) {
            return BASE_LEVEL;
        }
        return ownerKey(wallet)
                .flatMap(key -> kycCaseRepository
                        .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(key.ownerType(), key.ownerId()))
                .map(kycCase -> kycCase.getStatus() == KycCaseStatus.APPROVED ? VERIFIED_LEVEL : BASE_LEVEL)
                .orElse(BASE_LEVEL);
    }

    private record OwnerKey(DocumentOwnerType ownerType, Long ownerId) {
    }

    /**
     * Retrouve le titulaire derriere le portefeuille.
     *
     * <p>Le portefeuille designe son titulaire par sa reference commerciale ; le dossier de
     * verification le designe par son identifiant technique. Le pont se fait ici, et un titulaire
     * introuvable ne fait pas echouer l'operation · il retombe simplement au plancher, ou les
     * plafonds sont les plus stricts.</p>
     */
    private Optional<OwnerKey> ownerKey(WalletAccount wallet) {
        String ownerType = wallet.getOwnerType() == null
                ? "" : wallet.getOwnerType().trim().toUpperCase(Locale.ROOT);
        String ownerCode = wallet.getOwnerCode() == null ? "" : wallet.getOwnerCode().trim();
        if (ownerCode.isEmpty()) {
            return Optional.empty();
        }
        return switch (ownerType) {
            case "MEMBER" -> memberRepository.findByMemberIdAndDeletedFalse(ownerCode)
                    .map(member -> new OwnerKey(DocumentOwnerType.MEMBER, member.getId()));
            case "CUSTOMER" -> customerRepository.findByCustomerId(ownerCode)
                    .map(customer -> new OwnerKey(DocumentOwnerType.CUSTOMER, customer.getId()));
            default -> Optional.empty();
        };
    }
}
