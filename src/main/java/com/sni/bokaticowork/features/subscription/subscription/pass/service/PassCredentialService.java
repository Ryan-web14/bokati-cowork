package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassCredentialType;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassCredential;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Le support par lequel un pass se presente.
 *
 * <p>Distinct du pass lui-meme, et c'est ce qui permet de revoquer un code sans annuler le droit :
 * un telephone perdu appelle un nouveau support, pas un nouveau pass.</p>
 *
 * <h2>Pourquoi un code tournant</h2>
 *
 * <p>Un QR fixe se photographie. Une capture d'ecran partagee dans un groupe ouvre la porte a tout
 * le monde, indefiniment, et rien dans les usages ne distingue le titulaire de celui qui a recu
 * l'image. Un code a duree limitee reduit la fenetre : la capture ne vaut que jusqu'a la prochaine
 * rotation.</p>
 *
 * <p>Le code n'est pas une signature cryptographique, et il ne pretend pas l'etre. C'est un secret
 * aleatoire assez long pour ne pas se deviner, associe en base a un pass · la verification est une
 * lecture, pas un calcul, ce qui la rend revocable a tout instant.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassCredentialService {

    /** Ni O, ni I, ni L, ni 0, ni 1 · un code d'acces se dicte parfois a l'accueil. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int SECRET_LENGTH = 16;
    private static final int MAX_ATTEMPTS = 12;

    private final PassRepository passRepository;
    private final PassCredentialRepository credentialRepository;
    private final QrCodeRenderer qrCodeRenderer;
    private final SecureRandom random = new SecureRandom();

    @Value("${bokati.pass.credential.rotation-minutes:15}")
    private int rotationMinutes;

    /**
     * @param imageDataUri image QR encodee en base64, nulle pour un support qui ne s'affiche pas
     */
    public record IssuedCredential(
            String value,
            PassCredentialType credentialType,
            boolean rotating,
            Instant validUntil,
            String imageDataUri
    ) {
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Emet un support pour un pass.
     *
     * <p>Les supports actifs du meme type sont revoques : garder l'ancien laisserait deux codes
     * ouvrir la meme porte, dont un que le titulaire croit perime.</p>
     */
    @Transactional
    public IssuedCredential issue(String passNumber, PassCredentialType type, boolean rotating, String issuedBy) {
        Pass pass = pass(passNumber);
        if (pass.getStatus() == PassStatus.CANCELLED || pass.getStatus() == PassStatus.EXPIRED) {
            throw new BadRequestException("Ce pass n'est plus utilisable, aucun support ne peut être émis");
        }

        revokeActive(pass, type, "Remplacé par un nouveau support");

        Instant validUntil = rotating ? Instant.now().plus(Duration.ofMinutes(rotationMinutes)) : pass.getValidUntil();
        PassCredential credential = credentialRepository.save(PassCredential.builder()
                .pass(pass)
                .credentialType(type == null ? PassCredentialType.QR_CODE : type)
                .value(uniqueSecret())
                .rotating(rotating)
                .validUntil(validUntil)
                .issuedBy(issuedBy)
                .build());

        return render(credential);
    }

    /**
     * Rend le support courant, en le renouvelant s'il tourne et qu'il est arrive a echeance.
     *
     * <p>C'est ce que le portail appelle a chaque affichage. Renouveler a la lecture plutot que par
     * un traitement de fond evite de faire tourner des milliers de codes que personne ne regarde.</p>
     */
    @Transactional
    public IssuedCredential current(String passNumber, PassCredentialType type) {
        Pass pass = pass(passNumber);
        PassCredentialType resolved = type == null ? PassCredentialType.QR_CODE : type;

        PassCredential active = credentialRepository.findActiveByPassId(pass.getId()).stream()
                .filter(credential -> credential.getCredentialType() == resolved)
                .findFirst()
                .orElse(null);

        if (active == null) {
            return issue(passNumber, resolved, true, "SYSTEM");
        }
        if (Boolean.TRUE.equals(active.getRotating()) && !active.usableAt(Instant.now())) {
            return issue(passNumber, resolved, true, "SYSTEM");
        }
        return render(active);
    }

    /**
     * Revoque un support. Le pass n'est pas touche · un telephone perdu ne retire pas le droit,
     * il retire la clef.
     */
    @Transactional
    public void revoke(String value, String reason) {
        PassCredential credential = credentialRepository.findActiveByValue(value)
                .orElseThrow(() -> new ResourceNotFoundException("Support introuvable ou déjà révoqué"));
        credential.setRevokedAt(Instant.now());
        credential.setRevokedReason(reason);
        credentialRepository.save(credential);
        log.info("Support revoque sur le pass {} · {}", credential.getPass().getPassNumber(), reason);
    }

    @Transactional
    public int revokeAll(String passNumber, String reason) {
        Pass pass = pass(passNumber);
        List<PassCredential> active = credentialRepository.findActiveByPassId(pass.getId());
        active.forEach(credential -> {
            credential.setRevokedAt(Instant.now());
            credential.setRevokedReason(reason);
            credentialRepository.save(credential);
        });
        return active.size();
    }

    // -----------------------------------------------------------------------------------------

    private void revokeActive(Pass pass, PassCredentialType type, String reason) {
        credentialRepository.findActiveByPassId(pass.getId()).stream()
                .filter(credential -> type == null || credential.getCredentialType() == type)
                .forEach(credential -> {
                    credential.setRevokedAt(Instant.now());
                    credential.setRevokedReason(reason);
                    credentialRepository.save(credential);
                });
    }

    private IssuedCredential render(PassCredential credential) {
        // Seul un QR se dessine · un code d'acces se lit, un badge sans fil se presente.
        String image = credential.getCredentialType() == PassCredentialType.QR_CODE
                ? qrCodeRenderer.toDataUri(credential.getValue())
                : null;
        return new IssuedCredential(
                credential.getValue(),
                credential.getCredentialType(),
                Boolean.TRUE.equals(credential.getRotating()),
                credential.getValidUntil(),
                image);
    }

    private String uniqueSecret() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder secret = new StringBuilder(SECRET_LENGTH);
            for (int index = 0; index < SECRET_LENGTH; index++) {
                secret.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String candidate = secret.toString();
            if (!credentialRepository.existsActiveValue(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Impossible de générer un support unique");
    }

    private Pass pass(String passNumber) {
        if (!StringUtils.hasText(passNumber)) {
            throw new BadRequestException("Numéro de pass requis");
        }
        return passRepository.findByPassNumber(passNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Pass " + passNumber + " introuvable"));
    }
}
