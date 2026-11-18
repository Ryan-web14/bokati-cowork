package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Exigences KYC en ET / OU (17/11/2026). */
class KycRequirementSlotsTest {

    private static DocumentRequirement req(String code, boolean required, String group) {
        return DocumentRequirement.builder().ownerType(DocumentOwnerType.MEMBER).documentTypeCode(code).documentTypeName(code)
                .required(required).active(true).alternativeGroup(group).alternativeGroupLabel(group == null ? null : "Pièce d'identité").build();
    }

    private static KycDocument piece(String code, KycDocumentVerificationStatus status) {
        return KycDocument.builder().documentType(code).status(status).build();
    }

    /** Carte OU passeport (obligatoires, meme groupe) ET justificatif de domicile. */
    private final List<DocumentRequirement> requirements = List.of(
            req("MEMBER_NATIONAL_ID", true, "IDENTITE"),
            req("MEMBER_PASSPORT", true, "IDENTITE"),
            req("MEMBER_PROOF_OF_ADDRESS", true, null));

    private KycRequirementSlots.Evaluation evaluate(List<KycDocument> pieces) {
        return KycRequirementSlots.evaluate(requirements, pieces, type -> false, doc -> true);
    }

    @Test
    void onePieceOfTheGroupIsEnough() {
        KycRequirementSlots.Evaluation evaluation = evaluate(List.of(
                piece("MEMBER_PASSPORT", KycDocumentVerificationStatus.VERIFIED),
                piece("MEMBER_PROOF_OF_ADDRESS", KycDocumentVerificationStatus.VERIFIED)));

        assertThat(evaluation.complete()).isTrue();
        assertThat(evaluation.allVerified()).isTrue();
        assertThat(evaluation.missing()).isEmpty();
        assertThat(evaluation.slots()).hasSize(2);
    }

    @Test
    void anEmptyGroupIsMissingAsOneChoice() {
        KycRequirementSlots.Evaluation evaluation = evaluate(List.of(piece("MEMBER_PROOF_OF_ADDRESS", KycDocumentVerificationStatus.PENDING)));

        assertThat(evaluation.complete()).isFalse();
        assertThat(evaluation.missing()).containsExactly("MEMBER_NATIONAL_ID|MEMBER_PASSPORT");
    }

    @Test
    void aRejectedAlternativeDoesNotBlockWhenAnotherIsVerified() {
        List<KycDocument> pieces = List.of(
                piece("MEMBER_PASSPORT", KycDocumentVerificationStatus.REJECTED),
                piece("MEMBER_NATIONAL_ID", KycDocumentVerificationStatus.VERIFIED),
                piece("MEMBER_PROOF_OF_ADDRESS", KycDocumentVerificationStatus.VERIFIED));

        KycRequirementSlots.Evaluation evaluation = evaluate(pieces);

        assertThat(evaluation.blockingRejection()).isFalse();
        assertThat(evaluation.allVerified()).isTrue();
    }

    @Test
    void aRejectedPieceWithoutAlternativeBlocks() {
        KycRequirementSlots.Evaluation evaluation = evaluate(List.of(
                piece("MEMBER_PASSPORT", KycDocumentVerificationStatus.REJECTED),
                piece("MEMBER_PROOF_OF_ADDRESS", KycDocumentVerificationStatus.VERIFIED)));

        assertThat(evaluation.blockingRejection()).isTrue();
        assertThat(evaluation.allVerified()).isFalse();
    }

    @Test
    void theOtherPieceOfTheGroupIsNoLongerExpected() {
        List<KycDocument> pieces = List.of(piece("MEMBER_PASSPORT", KycDocumentVerificationStatus.PENDING));
        KycRequirementSlots.Evaluation evaluation = evaluate(pieces);

        assertThat(evaluation.satisfiedByAlternative("MEMBER_NATIONAL_ID", pieces)).isTrue();
        assertThat(evaluation.satisfiedByAlternative("MEMBER_PROOF_OF_ADDRESS", pieces)).isFalse();
    }

    @Test
    void allOptionalNeedsAtLeastOnePiece() {
        List<DocumentRequirement> optional = List.of(req("MEMBER_NATIONAL_ID", false, null), req("MEMBER_PASSPORT", false, null));

        KycRequirementSlots.Evaluation empty = KycRequirementSlots.evaluate(optional, List.of(), type -> false, doc -> true);
        KycRequirementSlots.Evaluation one = KycRequirementSlots.evaluate(optional,
                List.of(piece("MEMBER_PASSPORT", KycDocumentVerificationStatus.VERIFIED)), type -> false, doc -> true);

        assertThat(empty.complete()).isFalse();
        assertThat(empty.allVerified()).isFalse();
        assertThat(one.allVerified()).isTrue();
    }
}
