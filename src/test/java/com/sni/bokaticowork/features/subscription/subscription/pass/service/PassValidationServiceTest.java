package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidityRuleType;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassValidityRule;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassUsageRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassValidityRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Le service de validation, appelé par tous les canaux.
 *
 * <p>C'est la pièce qui manquait : le moment où quelqu'un se présente et où le pass doit être
 * accepté ou refusé. Dupliquer cette règle entre le guichet, la réservation et la borne produirait
 * trois comportements divergents, et c'est toujours celui qu'on n'a pas testé qui laisse passer.</p>
 *
 * <p>Chaque refus est vérifié avec son motif : un refus muet renvoie la personne vers l'accueil, où
 * personne ne saura davantage lui répondre.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassValidationServiceTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");
    /** Un mardi, à 10 h. */
    private static final Instant TUESDAY_MORNING =
            ZonedDateTime.of(2026, 9, 15, 10, 0, 0, 0, APP_ZONE).toInstant();

    @Mock private PassRepository passRepository;
    @Mock private PassCredentialRepository credentialRepository;
    @Mock private PassValidityRuleRepository validityRuleRepository;
    @Mock private PassBeneficiaryRepository beneficiaryRepository;
    @Mock private PassUsageRepository usageRepository;

    @InjectMocks
    private PassValidationService service;

    private Pass pass;
    private final List<PassValidityRule> rules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        pass = Pass.builder()
                .passNumber("PASS-0001")
                .passType(PassType.DAY_PASS)
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-TITULAIRE")
                .status(PassStatus.ACTIVE)
                .maxUses(10)
                .usedCount(0)
                .shareable(Boolean.FALSE)
                .build();
        pass.setId(1L);

        when(passRepository.findByPassNumber(anyString())).thenReturn(Optional.of(pass));
        when(validityRuleRepository.findApplicable(anyLong(), any())).thenReturn(rules);
        when(beneficiaryRepository.find(anyLong(), any(), any())).thenReturn(Optional.empty());
    }

    // -------------------------------------------------------------------------------------
    // État du pass
    // -------------------------------------------------------------------------------------

    @Test
    void acceptsAnActivePassWithinItsPeriod() {
        PassValidationService.ValidationResult result = validate(null);

        assertTrue(result.valid());
        assertNull(result.reason());
        assertEquals(10, result.remainingUses());
    }

    @Test
    void explainsEachRefusalInFrench() {
        pass.setStatus(PassStatus.SUSPENDED);
        assertEquals("Ce pass est suspendu", validate(null).reason());

        pass.setStatus(PassStatus.CANCELLED);
        assertEquals("Ce pass a été annulé", validate(null).reason());

        pass.setStatus(PassStatus.PENDING_ACTIVATION);
        assertEquals("Ce pass n'est pas encore activé", validate(null).reason());
    }

    @Test
    void refusesAPassOutsideItsPeriod() {
        pass.setValidFrom(TUESDAY_MORNING.plusSeconds(3600));
        assertEquals("Ce pass n'est pas encore valable", validate(null).reason());

        pass.setValidFrom(null);
        pass.setValidUntil(TUESDAY_MORNING.minusSeconds(1));
        assertEquals("Ce pass a expiré", validate(null).reason());
    }

    @Test
    void refusesAPassWhoseQuotaIsExhausted() {
        pass.setUsedCount(10);

        assertEquals("Ce pass est entièrement consommé", validate(null).reason());
    }

    @Test
    void acceptsAPassWithoutQuota() {
        pass.setMaxUses(null);

        PassValidationService.ValidationResult result = validate(null);

        assertTrue(result.valid());
        assertNull(result.remainingUses());
    }

    // -------------------------------------------------------------------------------------
    // Qui a le droit de s'en servir
    // -------------------------------------------------------------------------------------

    @Test
    void acceptsTheHolder() {
        assertTrue(validate("MEM-TITULAIRE").valid());
    }

    @Test
    void refusesSomeoneElseOnANonShareablePass() {
        assertEquals("Ce pass n'est utilisable que par son titulaire", validate("MEM-AUTRE").reason());
    }

    /** Sans cette table, COMPANY_SHARED_PASS reste un mot. */
    @Test
    void acceptsADesignatedBeneficiaryOnAShareablePass() {
        pass.setShareable(Boolean.TRUE);
        givenBeneficiary("MEM-COLLEGUE", 6, 2);

        PassValidationService.ValidationResult result = validate("MEM-COLLEGUE");

        assertTrue(result.valid());
        assertEquals("MEM-COLLEGUE", result.beneficiary().getBeneficiaryCode());
    }

    @Test
    void refusesSomeoneNotOnTheListOfAShareablePass() {
        pass.setShareable(Boolean.TRUE);

        assertEquals("Cette personne n'est pas autorisée à utiliser ce pass", validate("MEM-INCONNU").reason());
    }

    /**
     * Le quota individuel se vérifie ici, pas au décompte : annoncer l'accès puis le refuser à la
     * porte serait pire que de refuser tout de suite.
     */
    @Test
    void refusesABeneficiaryWhoReachedTheirOwnQuota() {
        pass.setShareable(Boolean.TRUE);
        givenBeneficiary("MEM-COLLEGUE", 6, 6);

        assertEquals("Cette personne a atteint son quota sur ce pass", validate("MEM-COLLEGUE").reason());
    }

    @Test
    void refusesARevokedBeneficiary() {
        pass.setShareable(Boolean.TRUE);
        PassBeneficiary beneficiary = givenBeneficiary("MEM-PARTI", null, 0);
        beneficiary.setRevokedAt(TUESDAY_MORNING.minusSeconds(3600));

        assertEquals("Cette personne n'est plus autorisée à utiliser ce pass", validate("MEM-PARTI").reason());
    }

    // -------------------------------------------------------------------------------------
    // Règles de validité fines
    // -------------------------------------------------------------------------------------

    @Test
    void appliesADayOfWeekRule() {
        rule(PassValidityRuleType.DAY_OF_WEEK, null, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY", true);
        assertTrue(validate(null).valid());

        rules.clear();
        rule(PassValidityRuleType.DAY_OF_WEEK, null, "SATURDAY,SUNDAY", true);
        assertEquals("Ce pass n'est pas utilisable ce jour de la semaine", validate(null).reason());
    }

    @Test
    void appliesATimeRangeRule() {
        rule(PassValidityRuleType.TIME_RANGE, null, "08:00,18:00", true);
        assertTrue(validate(null).valid());

        rules.clear();
        rule(PassValidityRuleType.TIME_RANGE, null, "18:00,22:00", true);
        assertEquals("Ce pass n'est pas utilisable à cette heure", validate(null).reason());
    }

    /** Une date bloquée est toujours une interdiction, quel que soit le drapeau. */
    @Test
    void alwaysRefusesABlackoutDate() {
        rule(PassValidityRuleType.BLACKOUT_DATE, null, "2026-09-15", true);

        assertEquals("Ce pass n'est pas utilisable à cette date", validate(null).reason());
    }

    @Test
    void appliesADailyCap() {
        rule(PassValidityRuleType.MAX_PER_DAY, "1", null, true);
        when(usageRepository.countOnDay(anyLong(), any(), any())).thenReturn(1L);

        assertEquals("Ce pass a déjà été utilisé le nombre de fois autorisé aujourd'hui",
                validate(null).reason());
    }

    @Test
    void appliesAConcurrencyCap() {
        rule(PassValidityRuleType.MAX_CONCURRENT, "1", null, true);
        when(usageRepository.countInFlight(anyLong())).thenReturn(1L);

        assertEquals("Ce pass est déjà en cours d'utilisation", validate(null).reason());
    }

    /**
     * Une interdiction l'emporte sur une autorisation. « Valable en semaine » et « interdit les
     * jours fériés » doivent cohabiter, et le jour férié doit gagner.
     */
    @Test
    void letsADenialWinOverAnAllowance() {
        rule(PassValidityRuleType.DAY_OF_WEEK, null, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY", true);
        rule(PassValidityRuleType.BLACKOUT_DATE, null, "2026-09-15", true);

        assertFalse(validate(null).valid());
    }

    /** Une règle illisible n'autorise rien : refuser se corrige, laisser passer non. */
    @Test
    void refusesRatherThanGuessWhenARuleCannotBeRead() {
        rule(PassValidityRuleType.TIME_RANGE, null, "n'importe quoi", true);

        assertFalse(validate(null).valid());
    }

    @Test
    void refusesAPassOnTheWrongSite() {
        rule(PassValidityRuleType.LOCATION, "SITE-A", null, true);

        PassValidationService.ValidationResult result = service.validate(
                new PassValidationService.ValidationRequest("PASS-0001", null, "MEMBER", null,
                        "SITE-B", null, TUESDAY_MORNING));

        assertEquals("Ce pass n'est pas utilisable sur ce site", result.reason());
    }

    @Test
    void refusesAnUnknownPass() {
        when(passRepository.findByPassNumber(anyString())).thenReturn(Optional.empty());

        assertEquals("Ce pass est introuvable", validate(null).reason());
    }

    // -------------------------------------------------------------------------------------

    private PassValidationService.ValidationResult validate(String bearerCode) {
        return service.validate(new PassValidationService.ValidationRequest(
                "PASS-0001", null, "MEMBER", bearerCode, null, null, TUESDAY_MORNING));
    }

    private PassBeneficiary givenBeneficiary(String code, Integer maxUses, int usedCount) {
        PassBeneficiary beneficiary = PassBeneficiary.builder()
                .pass(pass)
                .beneficiaryType("MEMBER")
                .beneficiaryCode(code)
                .maxUsesForBeneficiary(maxUses)
                .usedCount(usedCount)
                .addedAt(TUESDAY_MORNING.minusSeconds(86400))
                .build();
        when(beneficiaryRepository.find(1L, "MEMBER", code)).thenReturn(Optional.of(beneficiary));
        return beneficiary;
    }

    private void rule(PassValidityRuleType type, String value, String valueList, boolean allow) {
        PassValidityRule rule = PassValidityRule.builder()
                .pass(pass)
                .ruleType(type)
                .value(value)
                .valueList(valueList)
                .allowRule(allow)
                .build();
        rule.setId((long) rules.size() + 1);
        rules.add(rule);
    }
}
