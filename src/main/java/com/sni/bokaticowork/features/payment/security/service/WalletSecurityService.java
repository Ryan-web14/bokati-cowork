package com.sni.bokaticowork.features.payment.security.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.security.enums.PinRequirement;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.model.WalletCredential;
import com.sni.bokaticowork.features.payment.security.model.WalletSecurityPolicy;
import com.sni.bokaticowork.features.payment.security.repository.WalletCredentialRepository;
import com.sni.bokaticowork.features.payment.security.repository.WalletSecurityPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Le code secret d'un portefeuille · quand il est exige, et ce qu'il vaut.
 *
 * <p>Facultatif par defaut, obligatoire au transfert. Exiger un code pour consulter un solde ou
 * regler une facture deja connue ajoute une friction que rien ne justifie, et pousse les titulaires
 * vers des codes triviaux notes quelque part. Un transfert est l'exception : seule operation qui
 * fait sortir de l'argent vers quelqu'un d'autre, sans facture en face et sans annulation possible.</p>
 *
 * <p>Si le titulaire n'a pas encore de code, l'operation n'est pas refusee sechement : elle est
 * <b>suspendue le temps qu'il en cree un</b>. Refuser sans explication le conduirait a croire que
 * le transfert a echoue, et a le rejouer.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletSecurityService {

    private static final String ALGORITHM = "BCRYPT";
    private static final int MAX_LOCKOUT_MINUTES = 24 * 60;

    private final WalletSecurityPolicyRepository policyRepository;
    private final WalletCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Verdict d'un controle de securite.
     *
     * @param pinRequired    le code est exige pour cette operation
     * @param pinMissing     il est exige, et le titulaire n'en a pas encore · a lui proposer d'en poser un
     * @param otpRequired    le montant depasse le seuil, un second canal s'ajoute
     */
    public record SecurityVerdict(boolean pinRequired, boolean pinMissing, boolean otpRequired, String reason) {

        public boolean satisfied() {
            return !pinRequired && !otpRequired;
        }
    }

    // -----------------------------------------------------------------------------------------
    // Politique
    // -----------------------------------------------------------------------------------------

    /**
     * Politique applicable, du plus precis au plus general.
     *
     * <p>Le reglage d'un portefeuille l'emporte sur celui d'un type de titulaire, qui l'emporte sur
     * le reglage global. Sans aucune politique, on retombe sur la plus prudente qui soit tout en
     * restant utilisable : facultatif partout, impose au transfert.</p>
     */
    @Transactional(readOnly = true)
    public WalletSecurityPolicy policyFor(WalletAccount wallet) {
        Instant now = Instant.now();
        List<WalletSecurityPolicy> candidates = policyRepository.findCandidates(
                wallet.getOwnerType(), wallet.getWalletNumber());

        return candidates.stream()
                .filter(policy -> policy.activeAt(now))
                .min(Comparator.comparingInt(WalletSecurityPolicy::specificity))
                .orElseGet(() -> WalletSecurityPolicy.builder()
                        .pinRequirement(PinRequirement.OPTIONAL)
                        .pinRequiredOperations(WalletOperationType.TRANSFER.name())
                        .build());
    }

    /**
     * Le code est-il exige pour cette operation, et le titulaire en a-t-il un.
     */
    @Transactional(readOnly = true)
    public SecurityVerdict evaluate(WalletAccount wallet, WalletOperationType operation, java.math.BigDecimal amount) {
        WalletSecurityPolicy policy = policyFor(wallet);
        boolean pinRequired = policy.requiresPinFor(operation);
        boolean hasPin = credentialRepository.findByWalletId(wallet.getId()).isPresent();
        boolean otpRequired = policy.getOtpThresholdAmount() != null
                && amount != null
                && amount.compareTo(policy.getOtpThresholdAmount()) >= 0;

        if (pinRequired && !hasPin) {
            return new SecurityVerdict(true, true, otpRequired,
                    "Créez votre code secret pour poursuivre cette opération");
        }
        return new SecurityVerdict(pinRequired, false, otpRequired, null);
    }

    // -----------------------------------------------------------------------------------------
    // Le code
    // -----------------------------------------------------------------------------------------

    /**
     * Pose ou remplace le code.
     *
     * <p>Le remplacement exige l'ancien, sauf apres une reinitialisation administrative · dans ce
     * cas le titulaire n'a plus d'ancien code a fournir, et c'est bien le but.</p>
     */
    @Transactional
    public void setPin(WalletAccount wallet, String currentPin, String newPin) {
        WalletSecurityPolicy policy = policyFor(wallet);
        assertAcceptable(newPin, policy.getPinLength());

        Optional<WalletCredential> existing = credentialRepository.findByWalletId(wallet.getId());
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getMustChangePin())) {
            WalletCredential credential = existing.get();
            if (!StringUtils.hasText(currentPin) || !passwordEncoder.matches(currentPin, credential.getPinHash())) {
                throw new BadRequestException("Code secret actuel incorrect");
            }
        }

        WalletCredential credential = existing.orElseGet(() -> WalletCredential.builder().wallet(wallet).build());
        credential.setPinHash(passwordEncoder.encode(newPin));
        credential.setPinAlgorithm(ALGORITHM);
        credential.setPinSetAt(Instant.now());
        credential.setPinExpiresAt(policy.getPinExpiryDays() == null
                ? null
                : Instant.now().plus(Duration.ofDays(policy.getPinExpiryDays())));
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePin(Boolean.FALSE);
        credentialRepository.save(credential);
    }

    /**
     * Verifie le code.
     *
     * <p>Un echec incremente le compteur ; le seuil atteint, le portefeuille est temporise, et
     * chaque verrouillage double la temporisation. Une temporisation fixe se contourne par la
     * patience · une temporisation qui double rend l'essai automatise sans interet au bout de
     * quelques tours, sans jamais bloquer definitivement un titulaire qui a simplement oublie.</p>
     *
     * @throws ConflictException si le portefeuille est temporise
     * @throws BadRequestException si le code est faux, expire, ou absent
     */
    @Transactional
    public void verifyPin(WalletAccount wallet, String pin) {
        WalletCredential credential = credentialRepository.findByWalletId(wallet.getId())
                .orElseThrow(() -> new BadRequestException("Aucun code secret n'est défini sur ce portefeuille"));

        Instant now = Instant.now();
        if (credential.lockedAt(now)) {
            long minutes = Math.max(1, Duration.between(now, credential.getLockedUntil()).toMinutes());
            throw new ConflictException("wallet",
                    "code secret bloqué, réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : ""));
        }
        if (credential.expiredAt(now)) {
            throw new BadRequestException("Votre code secret a expiré, définissez-en un nouveau");
        }

        if (!StringUtils.hasText(pin) || !passwordEncoder.matches(pin, credential.getPinHash())) {
            registerFailure(wallet, credential, now);
            throw new BadRequestException("Code secret incorrect");
        }

        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setLastUsedAt(now);
        credentialRepository.save(credential);
    }

    /** Reinitialise le code. L'administrateur ne le lit jamais, il le retire. */
    @Transactional
    public void resetPin(WalletAccount wallet, String resetBy) {
        WalletCredential credential = credentialRepository.findByWalletId(wallet.getId())
                .orElseThrow(() -> new BadRequestException("Aucun code secret n'est défini sur ce portefeuille"));
        credential.setMustChangePin(Boolean.TRUE);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);
        log.info("Code secret du portefeuille {} reinitialise par {}", wallet.getWalletNumber(), resetBy);
    }

    // -----------------------------------------------------------------------------------------

    private void registerFailure(WalletAccount wallet, WalletCredential credential, Instant now) {
        WalletSecurityPolicy policy = policyFor(wallet);
        int attempts = (credential.getFailedAttempts() == null ? 0 : credential.getFailedAttempts()) + 1;
        credential.setFailedAttempts(attempts);

        if (attempts >= policy.getMaxFailedAttempts()) {
            int episodes = (credential.getLockoutCount() == null ? 0 : credential.getLockoutCount()) + 1;
            credential.setLockoutCount(episodes);
            credential.setLockedUntil(now.plus(Duration.ofMinutes(lockoutMinutes(policy, episodes))));
            credential.setFailedAttempts(0);
            log.warn("Portefeuille {} · code secret bloque apres {} echecs, episode {}",
                    wallet.getWalletNumber(), attempts, episodes);
        }
        credentialRepository.save(credential);
    }

    /** La temporisation double a chaque episode, bornee a vingt-quatre heures. */
    private long lockoutMinutes(WalletSecurityPolicy policy, int episodes) {
        long base = policy.getLockoutMinutes() == null ? 15 : policy.getLockoutMinutes();
        if (!Boolean.TRUE.equals(policy.getLockoutEscalation())) {
            return base;
        }
        long escalated = base * (1L << Math.min(episodes - 1, 10));
        return Math.min(escalated, MAX_LOCKOUT_MINUTES);
    }

    /**
     * Refuse les codes triviaux.
     *
     * <p>Une suite ou une repetition ne protege de rien : ce sont les premiers essayes, et ils sont
     * choisis precisement parce qu'ils sont faciles a retenir · donc faciles a deviner.</p>
     */
    private void assertAcceptable(String pin, int expectedLength) {
        if (!StringUtils.hasText(pin) || !pin.matches("\\d{" + expectedLength + "}")) {
            throw new BadRequestException("Le code secret doit comporter " + expectedLength + " chiffres");
        }
        if (pin.chars().distinct().count() == 1) {
            throw new BadRequestException("Un code composé du même chiffre est trop facile à deviner");
        }
        if (isSequential(pin)) {
            throw new BadRequestException("Un code formé d'une suite est trop facile à deviner");
        }
    }

    private boolean isSequential(String pin) {
        boolean ascending = true;
        boolean descending = true;
        for (int index = 1; index < pin.length(); index++) {
            int delta = pin.charAt(index) - pin.charAt(index - 1);
            ascending &= delta == 1;
            descending &= delta == -1;
        }
        return ascending || descending;
    }
}
