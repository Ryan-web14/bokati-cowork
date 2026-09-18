package com.sni.bokaticowork.features.payment.security.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.security.enums.PinRequirement;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.enums.WalletSecurityScope;
import com.sni.bokaticowork.features.payment.security.model.WalletCredential;
import com.sni.bokaticowork.features.payment.security.model.WalletSecurityPolicy;
import com.sni.bokaticowork.features.payment.security.repository.WalletCredentialRepository;
import com.sni.bokaticowork.features.payment.security.repository.WalletSecurityPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Le code secret du portefeuille.
 *
 * <p>Facultatif par défaut, obligatoire au transfert. Exiger un code pour consulter un solde ajoute
 * une friction que rien ne justifie et pousse vers des codes triviaux notés quelque part. Un
 * transfert est l'exception : seule opération qui fait sortir de l'argent vers quelqu'un d'autre,
 * sans facture en face et sans annulation possible.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletSecurityServiceTest {

    @Mock private WalletSecurityPolicyRepository policyRepository;
    @Mock private WalletCredentialRepository credentialRepository;

    @Mock
    private com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService flagService;

    @InjectMocks
    private WalletSecurityService service;

    private WalletAccount wallet;
    private final List<WalletSecurityPolicy> policies = new ArrayList<>();
    private WalletCredential stored;

    @BeforeEach
    void setUp() {
        wallet = new WalletAccount();
        wallet.setId(1L);
        wallet.setWalletNumber("WAL-0001");
        wallet.setOwnerType("MEMBER");
        wallet.setOwnerCode("MEM-1");

        when(policyRepository.findCandidates(anyString(), anyString())).thenReturn(policies);
        when(credentialRepository.findByWalletId(anyLong()))
                .thenAnswer(call -> Optional.ofNullable(stored));
        when(credentialRepository.save(any())).thenAnswer(call -> {
            stored = call.getArgument(0);
            return stored;
        });
    }

    // -------------------------------------------------------------------------------------
    // Quand le code est exigé
    // -------------------------------------------------------------------------------------

    @Test
    void doesNotAskForAPinOnAnOrdinaryPayment() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        assertFalse(service.evaluate(wallet, WalletOperationType.BILL_PAYMENT, null).pinRequired());
    }

    @Test
    void alwaysAsksForAPinOnATransfer() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        assertTrue(service.evaluate(wallet, WalletOperationType.TRANSFER, null).pinRequired());
    }

    /**
     * La règle ne tient pas par la discipline de celui qui configure. Retirer TRANSFER de la liste
     * ne le retire pas de la règle.
     */
    @Test
    void keepsTheTransferPinEvenWhenSomeoneRemovesItFromTheList() {
        policy(PinRequirement.OPTIONAL, "MERCHANT_PAYMENT");

        assertTrue(service.evaluate(wallet, WalletOperationType.TRANSFER, null).pinRequired());
    }

    /** Même désactivé : c'est le sens du mot « imposé ». */
    @Test
    void keepsTheTransferPinEvenWhenThePolicyIsDisabled() {
        policy(PinRequirement.DISABLED, "");

        assertTrue(service.evaluate(wallet, WalletOperationType.TRANSFER, null).pinRequired());
        assertFalse(service.evaluate(wallet, WalletOperationType.BILL_PAYMENT, null).pinRequired());
    }

    /**
     * Refuser sèchement conduirait le titulaire à croire que le transfert a échoué, et à le rejouer.
     * L'opération est suspendue le temps qu'il crée un code.
     */
    @Test
    void asksTheOwnerToCreateAPinRatherThanRefusingOutright() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        WalletSecurityService.SecurityVerdict verdict =
                service.evaluate(wallet, WalletOperationType.TRANSFER, null);

        assertTrue(verdict.pinMissing());
        assertTrue(verdict.reason().contains("Créez votre code secret"));
    }

    @Test
    void addsASecondChannelAboveTheConfiguredAmount() {
        WalletSecurityPolicy policy = policy(PinRequirement.OPTIONAL, "TRANSFER");
        policy.setOtpThresholdAmount(new BigDecimal("500000"));

        assertFalse(service.evaluate(wallet, WalletOperationType.TRANSFER, new BigDecimal("100000")).otpRequired());
        assertTrue(service.evaluate(wallet, WalletOperationType.TRANSFER, new BigDecimal("500000")).otpRequired());
    }

    /** Le réglage d'un portefeuille l'emporte sur celui d'un type de titulaire, puis sur le global. */
    @Test
    void prefersTheMostSpecificPolicy() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");
        WalletSecurityPolicy own = policy(PinRequirement.REQUIRED, "TRANSFER");
        own.setScope(WalletSecurityScope.WALLET);
        own.setScopeCode("WAL-0001");

        assertTrue(service.evaluate(wallet, WalletOperationType.BILL_PAYMENT, null).pinRequired());
    }

    // -------------------------------------------------------------------------------------
    // Poser le code
    // -------------------------------------------------------------------------------------

    @Test
    void storesOnlyTheFingerprintNeverThePin() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        service.setPin(wallet, null, "4827");

        assertNotNull(stored.getPinHash());
        assertFalse(stored.getPinHash().contains("4827"));
        assertEquals("BCRYPT", stored.getPinAlgorithm());
    }

    /** Une suite ou une répétition est choisie parce qu'elle est facile à retenir, donc à deviner. */
    @Test
    void refusesATrivialPin() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        assertTrue(assertThrows(BadRequestException.class,
                () -> service.setPin(wallet, null, "1111")).getMessage().contains("même chiffre"));
        assertTrue(assertThrows(BadRequestException.class,
                () -> service.setPin(wallet, null, "1234")).getMessage().contains("suite"));
        assertTrue(assertThrows(BadRequestException.class,
                () -> service.setPin(wallet, null, "4321")).getMessage().contains("suite"));
    }

    @Test
    void refusesAPinOfTheWrongLength() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        assertThrows(BadRequestException.class, () -> service.setPin(wallet, null, "482"));
        assertThrows(BadRequestException.class, () -> service.setPin(wallet, null, "48a7"));
    }

    @Test
    void requiresTheCurrentPinToReplaceIt() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");
        service.setPin(wallet, null, "4827");

        assertThrows(BadRequestException.class, () -> service.setPin(wallet, "9999", "5938"));
        assertDoesNotThrow(() -> service.setPin(wallet, "4827", "5938"));
    }

    /** Après une réinitialisation, le titulaire n'a plus d'ancien code à fournir. C'est le but. */
    @Test
    void letsTheOwnerSetANewPinAfterAnAdministrativeReset() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");
        service.setPin(wallet, null, "4827");
        service.resetPin(wallet, "admin");

        assertDoesNotThrow(() -> service.setPin(wallet, null, "5938"));
        assertFalse(stored.getMustChangePin());
    }

    // -------------------------------------------------------------------------------------
    // Vérifier le code
    // -------------------------------------------------------------------------------------

    @Test
    void acceptsTheRightPinAndClearsTheFailures() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");
        service.setPin(wallet, null, "4827");
        stored.setFailedAttempts(2);

        service.verifyPin(wallet, "4827");

        assertEquals(0, stored.getFailedAttempts());
        assertNotNull(stored.getLastUsedAt());
    }

    @Test
    void countsAFailureWithoutLockingTooEarly() {
        WalletSecurityPolicy policy = policy(PinRequirement.OPTIONAL, "TRANSFER");
        policy.setMaxFailedAttempts(3);
        service.setPin(wallet, null, "4827");

        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "0000"));

        assertEquals(1, stored.getFailedAttempts());
        assertNull(stored.getLockedUntil());
    }

    @Test
    void locksAfterTooManyFailures() {
        WalletSecurityPolicy policy = policy(PinRequirement.OPTIONAL, "TRANSFER");
        policy.setMaxFailedAttempts(2);
        service.setPin(wallet, null, "4827");

        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "0000"));
        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "0000"));

        assertNotNull(stored.getLockedUntil());
        assertTrue(assertThrows(ConflictException.class,
                () -> service.verifyPin(wallet, "4827")).getMessage().contains("bloqué"));
    }

    /**
     * Une temporisation fixe se contourne par la patience. Une temporisation qui double rend l'essai
     * automatisé sans intérêt au bout de quelques tours, sans jamais bloquer définitivement
     * quelqu'un qui a simplement oublié.
     */
    @Test
    void doublesTheLockoutAtEachEpisode() {
        WalletSecurityPolicy policy = policy(PinRequirement.OPTIONAL, "TRANSFER");
        policy.setMaxFailedAttempts(1);
        policy.setLockoutMinutes(10);
        service.setPin(wallet, null, "4827");

        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "0000"));
        Instant firstLock = stored.getLockedUntil();

        stored.setLockedUntil(null);
        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "0000"));
        Instant secondLock = stored.getLockedUntil();

        assertEquals(2, stored.getLockoutCount());
        assertTrue(secondLock.isAfter(firstLock));
    }

    @Test
    void refusesAnExpiredPin() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");
        service.setPin(wallet, null, "4827");
        stored.setPinExpiresAt(Instant.now().minusSeconds(60));

        assertTrue(assertThrows(BadRequestException.class,
                () -> service.verifyPin(wallet, "4827")).getMessage().contains("expiré"));
    }

    @Test
    void refusesToVerifyWhenNoPinWasEverSet() {
        policy(PinRequirement.OPTIONAL, "TRANSFER");

        assertThrows(BadRequestException.class, () -> service.verifyPin(wallet, "4827"));
    }

    // -------------------------------------------------------------------------------------

    private WalletSecurityPolicy policy(PinRequirement requirement, String operations) {
        WalletSecurityPolicy policy = WalletSecurityPolicy.builder()
                .scope(WalletSecurityScope.GLOBAL)
                .pinRequirement(requirement)
                .pinRequiredOperations(operations)
                .pinLength(4)
                .maxFailedAttempts(5)
                .lockoutMinutes(15)
                .lockoutEscalation(Boolean.TRUE)
                .build();
        policies.add(policy);
        return policy;
    }
}
