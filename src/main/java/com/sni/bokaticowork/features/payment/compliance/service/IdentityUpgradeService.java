package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Relie le niveau de verification aux plafonds · par une demande de pieces, pas par un refus sec.
 *
 * <p>Le module KYC existe et le module des plafonds existe ; ce qui manquait etait la phrase qui
 * les relie : le niveau de verification determine ce qui est permis, et une operation qui depasse
 * ce que ce niveau autorise ne se refuse pas sechement, elle dit au client ce qu'on attend de lui.
 * Ici, on le lui dit avec la liste exacte des pieces qui manquent a son dossier, et on la lui
 * envoie · pas seulement dans le message d'erreur, qu'il ne relira pas.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUpgradeService {

    private final KycCaseRepository kycCaseRepository;
    private final KycService kycService;
    private final MemberRepository memberRepository;
    private final TransactionContextResolver contextResolver;
    private final OutboxService outboxService;

    /** Ce qu'il faudrait fournir · vide si le dossier est complet et qu'on attend la revue. */
    public record DocumentsRequest(String kycCaseCode, KycCaseStatus kycStatus, List<String> missingDocuments, String message) {
    }

    /**
     * Ce que le titulaire doit fournir pour depasser son plafond.
     *
     * <p>Sans dossier · rien a demander, il faut d'abord en ouvrir un, et c'est l'inscription qui le
     * fait. Dossier approuve · le palier est deja le sien, la seule voie est la derogation nominative.
     * Sinon · les pieces manquantes, nommees.</p>
     */
    @Transactional(readOnly = true)
    public Optional<DocumentsRequest> whatIsMissing(WalletAccount wallet) {
        return kycCase(wallet).map(kycCase -> {
            List<String> missing = kycService.getMissingRequirements(kycCase.getCode()).stream()
                    .filter(KycRequirementStatus::isRequired)
                    .map(KycRequirementStatus::getDocumentTypeName)
                    .filter(StringUtils::hasText)
                    .toList();
            String message;
            if (kycCase.getStatus() == KycCaseStatus.APPROVED) {
                message = "Votre dossier est vérifié · ce plafond est celui de votre niveau, une dérogation se demande à l'accueil";
            } else if (missing.isEmpty()) {
                message = "Votre dossier est complet et en cours d'examen · le plafond sera relevé à son approbation";
            } else {
                message = "Pour relever votre plafond, ajoutez à votre dossier : " + String.join(", ", missing);
            }
            return new DocumentsRequest(kycCase.getCode(), kycCase.getStatus(), missing, message);
        });
    }

    /**
     * Refus de plafond enrichi · le chemin de sortie devient la liste des pieces.
     *
     * <p>Et la demande part par courriel, parce qu'un message d'erreur se ferme et s'oublie. Le
     * courriel, lui, reste dans la boite avec la liste a fournir.</p>
     */
    @Transactional
    public WalletLimitService.LimitVerdict enrich(WalletAccount wallet, WalletLimitService.LimitVerdict verdict) {
        if (verdict.allowed()) {
            return verdict;
        }
        Optional<DocumentsRequest> request = whatIsMissing(wallet);
        if (request.isEmpty()) {
            return verdict;
        }
        DocumentsRequest documents = request.get();
        if (!documents.missingDocuments().isEmpty()) {
            requestDocuments(wallet, documents, verdict.reason());
        }
        return new WalletLimitService.LimitVerdict(false, verdict.reason(), documents.message());
    }

    private void requestDocuments(WalletAccount wallet, DocumentsRequest documents, String because) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
        if (!StringUtils.hasText(party.email())) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientEmail", party.email());
        payload.put("recipientName", party.name());
        payload.put("recipientType", wallet.getOwnerType());
        payload.put("recipientCode", wallet.getOwnerCode());
        payload.put("subject", "Complétez votre dossier pour relever votre plafond");
        payload.put("templateCode", "WALLET_KYC_DOCUMENTS_REQUESTED");
        payload.put("walletNumber", wallet.getWalletNumber());
        payload.put("kycCaseCode", documents.kycCaseCode());
        payload.put("missingDocuments", String.join(", ", documents.missingDocuments()));
        payload.put("reason", because);
        outboxService.publish("WALLET_KYC_DOCUMENTS_REQUESTED", "WALLET", wallet.getWalletNumber(), payload);
        log.info("Portefeuille {} · pieces demandees pour relever le plafond : {}", wallet.getWalletNumber(),
                documents.missingDocuments());
    }

    private Optional<KycCase> kycCase(WalletAccount wallet) {
        if (!"MEMBER".equalsIgnoreCase(wallet.getOwnerType())) {
            return Optional.empty();
        }
        return memberRepository.findByMemberIdAndDeletedFalse(wallet.getOwnerCode())
                .flatMap(member -> kycCaseRepository
                        .findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, member.getId()));
    }
}
