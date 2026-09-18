package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageType;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidationChannel;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassUsage;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassUsageRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le décompte.
 *
 * <p>{@code PARTIALLY_USED} et {@code CONSUMED} existaient depuis le début sans que rien ne les
 * pose : un pass restait {@code ACTIVE} jusqu'à son expiration, quoi qu'on en ait fait. Un pass à
 * vingt journées dont on ne sait pas combien ont été utilisées n'est pas un pass, c'est une
 * promesse.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassUsageServiceTest {

    @Mock private PassRepository passRepository;
    @Mock private PassUsageRepository usageRepository;
    @Mock private PassBeneficiaryRepository beneficiaryRepository;
    @Mock private PassValidationService validationService;
    @Mock private PassEventWriter eventWriter;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private PassUsageService service;

    private Pass pass;

    @BeforeEach
    void setUp() {
        pass = Pass.builder()
                .passNumber("PASS-0001")
                .passType(PassType.DAY_PASS)
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-TITULAIRE")
                .status(PassStatus.ACTIVE)
                .maxUses(3)
                .usedCount(0)
                .build();
        pass.setId(1L);

        when(sequenceGenerator.next(anyString())).thenReturn("USG-0001");
        when(usageRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(passRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(beneficiaryRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(usageRepository.findByReference(anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(beneficiaryRepository.find(anyLong(), any(), any())).thenReturn(Optional.empty());
        givenValidationAccepts(null);
    }

    // -------------------------------------------------------------------------------------

    @Test
    void recordsTheUsageAndMovesThePassToPartiallyUsed() {
        PassUsage usage = service.use(request(null));

        assertEquals("USG-0001", usage.getUsageNumber());
        assertEquals(1, pass.getUsedCount());
        assertEquals(PassStatus.PARTIALLY_USED, pass.getStatus());
        verify(eventWriter).writeEvent(eq(pass), eq(PassEventType.PASS_USED), any());
    }

    @Test
    void movesToConsumedWhenTheQuotaIsReached() {
        pass.setUsedCount(2);

        service.use(request(null));

        assertEquals(3, pass.getUsedCount());
        assertEquals(PassStatus.CONSUMED, pass.getStatus());
    }

    /** Toute consommation repasse par la validation : l'oubli est rendu impossible. */
    @Test
    void refusesToConsumeWhatTheValidationRejects() {
        when(validationService.validate(any())).thenReturn(
                new PassValidationService.ValidationResult(false, "Ce pass a expiré", null, null, null));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.use(request(null)));

        assertEquals("Ce pass a expiré", ex.getMessage());
        verify(usageRepository, never()).save(any());
    }

    /**
     * Un double scan à la borne, ou un client qui réappuie parce que rien ne s'est affiché, ne doit
     * pas consommer deux journées.
     */
    @Test
    void returnsTheExistingUsageOnASecondScanOfTheSameEvent() {
        PassUsage already = PassUsage.builder().usageNumber("USG-0001").pass(pass).build();
        when(usageRepository.findByReference(1L, "BOOKING", "BKG-1")).thenReturn(Optional.of(already));

        PassUsage usage = service.use(request(null));

        assertSame(already, usage);
        assertEquals(0, pass.getUsedCount());
        verify(usageRepository, never()).save(any());
    }

    @Test
    void countsAgainstTheBeneficiaryQuotaWhenOneIsDesignated() {
        PassBeneficiary beneficiary = PassBeneficiary.builder()
                .pass(pass).beneficiaryType("MEMBER").beneficiaryCode("MEM-COLLEGUE")
                .maxUsesForBeneficiary(6).usedCount(2).addedAt(Instant.now()).build();
        givenValidationAccepts(beneficiary);

        service.use(request("MEM-COLLEGUE"));

        assertEquals(3, beneficiary.getUsedCount());
        assertEquals(1, pass.getUsedCount());
    }

    // -------------------------------------------------------------------------------------
    // Contre-passation
    // -------------------------------------------------------------------------------------

    @Test
    void givesTheUseBackWithoutErasingTheLine() {
        pass.setUsedCount(2);
        pass.setStatus(PassStatus.PARTIALLY_USED);
        PassUsage usage = PassUsage.builder()
                .usageNumber("USG-0001").pass(pass)
                .usedByType("MEMBER").usedByCode("MEM-TITULAIRE").build();
        when(usageRepository.findByNumber("USG-0001")).thenReturn(Optional.of(usage));

        PassUsage reversed = service.reverse("USG-0001", "Erreur de saisie", "admin");

        assertNotNull(reversed.getReversedAt());
        assertEquals("Erreur de saisie", reversed.getReversalReason());
        assertEquals(1, pass.getUsedCount());
        verify(usageRepository).save(usage);
    }

    /**
     * Une contre-passation qui ramène à zéro rend le pass à son état d'origine : le laisser
     * « partiellement utilisé » sans aucun usage derrière serait faux.
     */
    @Test
    void returnsThePassToActiveWhenTheLastUseIsReversed() {
        pass.setUsedCount(1);
        pass.setStatus(PassStatus.PARTIALLY_USED);
        PassUsage usage = PassUsage.builder().usageNumber("USG-0001").pass(pass).build();
        when(usageRepository.findByNumber("USG-0001")).thenReturn(Optional.of(usage));

        service.reverse("USG-0001", "Erreur", "admin");

        assertEquals(0, pass.getUsedCount());
        assertEquals(PassStatus.ACTIVE, pass.getStatus());
    }

    @Test
    void reopensAConsumedPassWhenAUseIsReversed() {
        pass.setUsedCount(3);
        pass.setStatus(PassStatus.CONSUMED);
        PassUsage usage = PassUsage.builder().usageNumber("USG-0001").pass(pass).build();
        when(usageRepository.findByNumber("USG-0001")).thenReturn(Optional.of(usage));

        service.reverse("USG-0001", "Geste commercial", "admin");

        assertEquals(PassStatus.PARTIALLY_USED, pass.getStatus());
    }

    @Test
    void ignoresASecondReversalOfTheSameUsage() {
        PassUsage usage = PassUsage.builder()
                .usageNumber("USG-0001").pass(pass).reversedAt(Instant.now()).build();
        when(usageRepository.findByNumber("USG-0001")).thenReturn(Optional.of(usage));

        service.reverse("USG-0001", "Encore", "admin");

        assertEquals(0, pass.getUsedCount());
        verify(passRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------------------

    private void givenValidationAccepts(PassBeneficiary beneficiary) {
        when(validationService.validate(any())).thenReturn(
                new PassValidationService.ValidationResult(true, null, pass, beneficiary, 3));
    }

    private PassUsageService.UseRequest request(String bearerCode) {
        return new PassUsageService.UseRequest(
                "PASS-0001", null, "MEMBER", bearerCode, PassUsageType.CHECK_IN,
                "SITE-A", null, null, PassValidationChannel.QR_SCAN, "borne",
                "BOOKING", "BKG-1", null);
    }
}
