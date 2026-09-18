package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassCredentialType;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassCredential;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Le support d'un pass.
 *
 * <p>Distinct du pass lui-même, et c'est tout l'intérêt : révoquer un code n'annule pas le droit.
 * Un téléphone perdu appelle un nouveau support, pas un nouveau pass.</p>
 *
 * <p>Un QR fixe se photographie, et une capture partagée dans un groupe ouvre la porte à tout le
 * monde, indéfiniment. Le code tournant réduit la fenêtre à quelques minutes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassCredentialServiceTest {

    @Mock private PassRepository passRepository;
    @Mock private PassCredentialRepository credentialRepository;
    @Mock private QrCodeRenderer qrCodeRenderer;

    @InjectMocks
    private PassCredentialService service;

    private Pass pass;
    private final List<PassCredential> active = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "rotationMinutes", 15);
        pass = Pass.builder()
                .passNumber("PASS-0001")
                .passType(PassType.DAY_PASS)
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-1")
                .status(PassStatus.ACTIVE)
                .build();
        pass.setId(1L);

        when(passRepository.findByPassNumber(anyString())).thenReturn(Optional.of(pass));
        when(credentialRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(credentialRepository.findActiveByPassId(anyLong())).thenReturn(active);
        when(credentialRepository.existsActiveValue(anyString())).thenReturn(false);
        when(qrCodeRenderer.toDataUri(anyString())).thenReturn("data:image/png;base64,AAAA");
    }

    // -------------------------------------------------------------------------------------

    @Test
    void issuesARotatingQrCodeWithItsImage() {
        PassCredentialService.IssuedCredential issued =
                service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin");

        assertEquals(PassCredentialType.QR_CODE, issued.credentialType());
        assertTrue(issued.rotating());
        assertNotNull(issued.validUntil());
        assertEquals("data:image/png;base64,AAAA", issued.imageDataUri());
        assertEquals(16, issued.value().length());
    }

    /** Le secret ne contient ni O, ni I, ni L, ni 0, ni 1 : il se dicte parfois à l'accueil. */
    @Test
    void producesASecretWithoutAmbiguousCharacters() {
        String value = service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin").value();

        assertFalse(value.matches(".*[OIL01].*"), "secret ambigu : " + value);
    }

    @Test
    void producesADifferentSecretEachTime() {
        String first = service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin").value();
        String second = service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin").value();

        assertNotEquals(first, second);
    }

    /** Garder l'ancien laisserait deux codes ouvrir la même porte, dont un que le titulaire croit périmé. */
    @Test
    void revokesThePreviousCredentialOfTheSameType() {
        PassCredential previous = credential(PassCredentialType.QR_CODE, true, Instant.now().plusSeconds(600));
        active.add(previous);

        service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin");

        assertNotNull(previous.getRevokedAt());
        assertTrue(previous.getRevokedReason().contains("Remplacé"));
    }

    /** Seul un QR se dessine · un code d'accès se lit, un badge sans fil se présente. */
    @Test
    void doesNotDrawAnImageForANonVisualCredential() {
        PassCredentialService.IssuedCredential issued =
                service.issue("PASS-0001", PassCredentialType.ACCESS_CODE, false, "admin");

        assertNull(issued.imageDataUri());
        assertFalse(issued.rotating());
    }

    @Test
    void refusesToIssueOnACancelledPass() {
        pass.setStatus(PassStatus.CANCELLED);

        assertTrue(assertThrows(BadRequestException.class,
                () -> service.issue("PASS-0001", PassCredentialType.QR_CODE, true, "admin"))
                .getMessage().contains("n'est plus utilisable"));
    }

    // -------------------------------------------------------------------------------------
    // Rotation à la lecture
    // -------------------------------------------------------------------------------------

    @Test
    void issuesOneOnFirstReadWhenThePassHasNoCredentialYet() {
        PassCredentialService.IssuedCredential issued = service.current("PASS-0001", PassCredentialType.QR_CODE);

        assertNotNull(issued.value());
        assertTrue(issued.rotating());
    }

    @Test
    void keepsTheSameCodeWhileItIsStillValid() {
        PassCredential existing = credential(PassCredentialType.QR_CODE, true, Instant.now().plusSeconds(600));
        active.add(existing);

        PassCredentialService.IssuedCredential issued = service.current("PASS-0001", PassCredentialType.QR_CODE);

        assertEquals(existing.getValue(), issued.value());
        assertNull(existing.getRevokedAt());
    }

    /**
     * Renouveler à la lecture plutôt que par un traitement de fond évite de faire tourner des
     * milliers de codes que personne ne regarde.
     */
    @Test
    void rotatesOnReadWhenTheCodeHasExpired() {
        PassCredential stale = credential(PassCredentialType.QR_CODE, true, Instant.now().minusSeconds(60));
        active.add(stale);

        PassCredentialService.IssuedCredential issued = service.current("PASS-0001", PassCredentialType.QR_CODE);

        assertNotEquals(stale.getValue(), issued.value());
        assertNotNull(stale.getRevokedAt());
    }

    /** Un code fixe périmé n'est pas renouvelé en silence : il suit la validité du pass. */
    @Test
    void doesNotRotateANonRotatingCredential() {
        PassCredential fixed = credential(PassCredentialType.QR_CODE, false, Instant.now().plusSeconds(86400));
        active.add(fixed);

        assertEquals(fixed.getValue(), service.current("PASS-0001", PassCredentialType.QR_CODE).value());
    }

    // -------------------------------------------------------------------------------------
    // Révocation
    // -------------------------------------------------------------------------------------

    /** Le geste du téléphone perdu : on retire la clef, pas le droit. */
    @Test
    void revokesEveryCredentialWithoutTouchingThePass() {
        active.add(credential(PassCredentialType.QR_CODE, true, Instant.now().plusSeconds(600)));
        active.add(credential(PassCredentialType.ACCESS_CODE, false, null));

        assertEquals(2, service.revokeAll("PASS-0001", "Téléphone perdu"));

        assertTrue(active.stream().allMatch(credential -> credential.getRevokedAt() != null));
        assertEquals(PassStatus.ACTIVE, pass.getStatus());
    }

    @Test
    void refusesToRevokeAnUnknownCredential() {
        when(credentialRepository.findActiveByValue(anyString())).thenReturn(Optional.empty());

        assertThrows(com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException.class,
                () -> service.revoke("INCONNU", "Test"));
    }

    // -------------------------------------------------------------------------------------

    private PassCredential credential(PassCredentialType type, boolean rotating, Instant validUntil) {
        return PassCredential.builder()
                .pass(pass)
                .credentialType(type)
                .value("SECRET" + type.name() + rotating)
                .rotating(rotating)
                .validUntil(validUntil)
                .issuedAt(Instant.now().minusSeconds(120))
                .build();
    }
}
