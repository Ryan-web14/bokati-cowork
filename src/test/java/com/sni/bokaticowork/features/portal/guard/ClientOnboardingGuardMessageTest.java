package com.sni.bokaticowork.features.portal.guard;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Un refus dit où en est le dossier.
 *
 * <p>« Votre vérification d'identité est requise » laissait le titulaire sans rien à faire et le
 * guichet sans rien à répondre : dossier pas commencé, pièces en cours d'examen, ou pièce refusée
 * à redéposer sont trois situations différentes, et une seule demande une action du client.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientOnboardingGuardMessageTest {

    @Mock private KycDocumentRepository kycDocumentRepository;

    private final ClientOnboardingGuard guard = new ClientOnboardingGuard(null, null, null, null);

    private ClientOnboardingGuard guardWith(KycDocumentRepository repository) {
        ReflectionTestUtils.setField(guard, "kycDocumentRepository", repository);
        return guard;
    }

    private String explain(KycCaseStatus status) {
        KycCase kycCase = status == null ? null
                : KycCase.builder().code("KCS-202607-000002").status(status)
                        .ownerType(DocumentOwnerType.MEMBER).ownerId(1L).build();
        return ReflectionTestUtils.<String>invokeMethod(guardWith(kycDocumentRepository), "explain", kycCase);
    }

    @Test
    void withoutAnyCaseItSaysNothingHasBeenSubmittedYet() {
        assertThat(explain(null))
                .contains("n'a pas encore commencé")
                .contains("/client/me/onboarding-status");
    }

    @Test
    void underReviewItSaysToWaitRatherThanToAct() {
        assertThat(explain(KycCaseStatus.UNDER_REVIEW))
                .contains("en cours de vérification")
                .contains("KCS-202607-000002");
        assertThat(explain(KycCaseStatus.SUBMITTED)).contains("en cours de vérification");
    }

    @Test
    void rejectedItAsksForNewPiecesAndNamesThem() {
        KycCase kycCase = KycCase.builder().code("KCS-202607-000002").status(KycCaseStatus.REJECTED)
                .ownerType(DocumentOwnerType.MEMBER).ownerId(1L).build();
        DocumentType type = DocumentType.builder().code("CNI").name("Carte nationale d'identité membre").build();
        when(kycDocumentRepository.findAllByKycCaseOrderByIdAsc(any())).thenReturn(List.of(
                KycDocument.builder().status(KycDocumentVerificationStatus.REJECTED)
                        .document(Document.builder().code("DOC-202607-000036").documentType(type).build()).build(),
                KycDocument.builder().status(KycDocumentVerificationStatus.VERIFIED)
                        .document(Document.builder().code("DOC-202607-000001").documentType(type).build()).build()));

        assertThat(ReflectionTestUtils.<String>invokeMethod(guardWith(kycDocumentRepository), "explain", kycCase))
                .contains("refusées");

        List<String> pieces = ReflectionTestUtils.<List<String>>invokeMethod(guardWith(kycDocumentRepository), "rejectedPieces", kycCase);
        assertThat(pieces).containsExactly("Carte nationale d'identité membre");
    }

    @Test
    void anExpiredCaseAsksForUpToDatePieces() {
        assertThat(explain(KycCaseStatus.EXPIRED)).contains("échéance");
        assertThat(explain(KycCaseStatus.RENEWAL_REQUIRED)).contains("échéance");
    }

    @Test
    void anUnfinishedCaseAsksForTheMissingPieces() {
        assertThat(explain(KycCaseStatus.IN_PROGRESS)).contains("incomplet");
        assertThat(explain(KycCaseStatus.NOT_STARTED)).contains("incomplet");
    }

    @Test
    void withoutACaseNoPieceIsNamed() {
        List<String> pieces = ReflectionTestUtils.<List<String>>invokeMethod(guardWith(kycDocumentRepository), "rejectedPieces", new Object[]{null});
        assertThat(pieces).isEmpty();
    }
}
