package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionBeneficiary;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * La liste nominative d'une promotion.
 *
 * <p>Trois exigences sont figées ici, chacune venant d'un problème concret : un retrait qui efface
 * rend le geste indéfendable, un import muet oblige à chercher les refus à la main, et un avantage
 * déjà consommé ne se reprend pas.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromotionBeneficiaryServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionBeneficiaryRepository beneficiaryRepository;

    @InjectMocks
    private PromotionBeneficiaryService service;

    private final Promotion promotion = new Promotion();

    @BeforeEach
    void setUp() {
        promotion.setId(1L);
        promotion.setCode("GESTE");
        when(promotionRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(promotion));
        when(beneficiaryRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(beneficiaryRepository.find(any(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void addsANamedBeneficiaryWithItsReason() {
        PromotionBeneficiary beneficiary = service.add("GESTE", "MEMBER", "MEM-1", "Client mécontent", "admin");

        assertEquals("MEM-1", beneficiary.getSubscriberCode());
        assertEquals("Client mécontent", beneficiary.getAddedReason());
        assertEquals("admin", beneficiary.getAddedBy());
    }

    /**
     * Réactiver plutôt que créer une seconde ligne conserve l'histoire du premier geste, et respecte
     * l'unicité posée en base.
     */
    @Test
    void reinstatesAPreviouslyRevokedBeneficiaryInsteadOfDuplicating() {
        PromotionBeneficiary revoked = beneficiary();
        revoked.setRevokedAt(Instant.now());
        revoked.setRevokedReason("Erreur");
        when(beneficiaryRepository.find(any(), anyString(), anyString())).thenReturn(Optional.of(revoked));

        PromotionBeneficiary result = service.add("GESTE", "MEMBER", "MEM-1", "Deuxième geste", "admin");

        assertNull(result.getRevokedAt());
        assertNull(result.getRevokedReason());
        assertEquals("Deuxième geste", result.getAddedReason());
    }

    /** Le retrait ne supprime pas : la ligne reste, avec qui l'a retirée et pourquoi. */
    @Test
    void keepsTheLineWhenABeneficiaryIsRemoved() {
        when(beneficiaryRepository.find(any(), anyString(), anyString())).thenReturn(Optional.of(beneficiary()));

        PromotionBeneficiary result = service.revoke("GESTE", "MEMBER", "MEM-1", "Accordé par erreur", "admin");

        assertNotNull(result.getRevokedAt());
        assertEquals("Accordé par erreur", result.getRevokedReason());
        assertEquals("admin", result.getRevokedBy());
        assertNotNull(result.getAddedAt());
    }

    @Test
    void refusesToRemoveAnAdvantageAlreadyUsed() {
        PromotionBeneficiary used = beneficiary();
        used.setRedeemedAt(Instant.now());
        when(beneficiaryRepository.find(any(), anyString(), anyString())).thenReturn(Optional.of(used));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.revoke("GESTE", "MEMBER", "MEM-1", "Trop tard", "admin"));

        assertTrue(ex.getMessage().contains("déjà utilisé"));
    }

    @Test
    void tracesWhoWasNotifiedAndThroughWhichChannel() {
        when(beneficiaryRepository.find(any(), anyString(), anyString())).thenReturn(Optional.of(beneficiary()));

        PromotionBeneficiary result = service.markNotified("GESTE", "MEMBER", "MEM-1", "EMAIL");

        assertNotNull(result.getNotifiedAt());
        assertEquals("EMAIL", result.getNotificationChannel());
    }

    // -------------------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------------------

    /**
     * Une ligne refusée n'empêche pas les autres d'entrer : un fichier de quarante noms ne doit pas
     * échouer en entier à cause d'un seul code mal saisi.
     */
    @Test
    void importsWhatItCanAndSaysWhyForTheRest() {
        PromotionBeneficiaryService.ImportReport report = service.importAll("GESTE", List.of(
                new String[]{"MEMBER", "MEM-1"},
                new String[]{"MEMBER", ""},
                new String[]{"MEMBER", "MEM-2"},
                new String[]{"MEMBER", "MEM-1"}
        ), "Partenariat", "admin");

        assertEquals(4, report.submitted());
        assertEquals(2, report.accepted());
        assertEquals(2, report.rejected());

        assertTrue(report.lines().get(0).accepted());
        assertEquals("Type et code de souscripteur requis", report.lines().get(1).outcome());
        assertTrue(report.lines().get(2).accepted());
        assertEquals("Doublon dans le fichier", report.lines().get(3).outcome());
        // Le numéro de ligne permet à l'appelant de retrouver la sienne dans son fichier.
        assertEquals(4, report.lines().get(3).line());
    }

    @Test
    void refusesAnEmptyImport() {
        assertThrows(BadRequestException.class, () -> service.importAll("GESTE", List.of(), null, "admin"));
    }

    @Test
    void reportsTheRealUsageOfANominativeCampaign() {
        when(beneficiaryRepository.countActive(1L)).thenReturn(40L);
        when(beneficiaryRepository.countRedeemed(1L)).thenReturn(11L);

        PromotionBeneficiaryService.Usage usage = service.usage("GESTE");

        assertEquals(40L, usage.active());
        assertEquals(11L, usage.redeemed());
    }

    // -------------------------------------------------------------------------------------

    private PromotionBeneficiary beneficiary() {
        return PromotionBeneficiary.builder()
                .promotion(promotion)
                .subscriberType("MEMBER")
                .subscriberCode("MEM-1")
                .addedAt(Instant.parse("2026-09-01T10:00:00Z"))
                .addedBy("admin")
                .addedReason("Client mécontent")
                .build();
    }
}
