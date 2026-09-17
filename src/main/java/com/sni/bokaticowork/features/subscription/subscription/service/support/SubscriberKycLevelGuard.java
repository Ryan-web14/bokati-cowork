package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Verifie qu'un souscripteur atteint le niveau de verification exige par ce qu'il souscrit.
 *
 * <p>Le controle n'existait que pour l'abonnement, en methode privee de son operateur de creation.
 * Le pass exigeait pourtant deja un niveau, {@code PassPlanVersion.requiredKycLevel} etant present
 * dans le modele depuis le depart, mais personne ne le lisait : un pass se vendait quel que soit le
 * niveau de verification de son acheteur. Un controle modelise et jamais applique est plus
 * trompeur qu'un controle absent, parce qu'il donne a croire qu'il a lieu.</p>
 *
 * <p>Le niveau 1 vaut absence d'exigence : c'est le niveau par defaut de tout titulaire, y compris
 * de celui qui n'a jamais rien fourni.</p>
 */
@Component
@RequiredArgsConstructor
public class SubscriberKycLevelGuard {

    private static final int DEFAULT_LEVEL = 1;

    private final KycCaseRepository kycCaseRepository;

    /**
     * @param requiredLevel niveau exige, nul ou inferieur a 2 signifiant aucune exigence
     * @param owner         titulaire resolu
     * @param subject       ce qui est souscrit, pour que le message dise de quoi il s'agit
     */
    public void require(Integer requiredLevel, SubscriptionOwnerResolver.Owner owner, String subject) {
        if (requiredLevel == null || requiredLevel <= DEFAULT_LEVEL) {
            return;
        }
        OwnerKyc ownerKyc = resolveOwnerKyc(owner);
        int currentLevel = kycCaseRepository
                .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerKyc.ownerType(), ownerKyc.ownerId())
                .filter(kycCase -> kycCase.getStatus() == KycCaseStatus.APPROVED)
                .map(kycCase -> kycCase.getKycLevel() == null ? DEFAULT_LEVEL : kycCase.getKycLevel())
                .orElse(DEFAULT_LEVEL);
        if (currentLevel < requiredLevel) {
            throw new BadRequestException("KYC level " + requiredLevel + " required for this " + subject);
        }
    }

    private OwnerKyc resolveOwnerKyc(SubscriptionOwnerResolver.Owner owner) {
        if (owner.member() != null) {
            return new OwnerKyc(DocumentOwnerType.MEMBER, owner.member().getId());
        }
        if (owner.customer() != null) {
            return new OwnerKyc(DocumentOwnerType.CUSTOMER, owner.customer().getId());
        }
        if (owner.businessEntity() != null) {
            return new OwnerKyc(DocumentOwnerType.BUSINESS, owner.businessEntity().getId());
        }
        throw new BadRequestException("KYC owner could not be resolved");
    }

    private record OwnerKyc(DocumentOwnerType ownerType, Long ownerId) {
    }
}
