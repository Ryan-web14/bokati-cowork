package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Un paiement ne s'enregistre pas deux fois (17/11/2026).
 *
 * <ol>
 *   <li><b>Verrou</b> : deux envois sur la meme intention (ou la meme facture) passent l'un apres
 *       l'autre, jamais en meme temps (verrou de transaction PostgreSQL, libere a la fin de la
 *       transaction). Le second voit donc ce que le premier a enregistre.</li>
 *   <li><b>Rejeu</b> : un paiement identique enregistre il y a moins de
 *       {@code bokati.payment.duplicate-window-seconds} secondes (30 par defaut) - meme mode, meme
 *       montant, meme auteur - est rendu tel quel au lieu d'en creer un second. C'est le double clic
 *       sur « Encaisser ».</li>
 * </ol>
 *
 * <p>Avant, deux appuis sur le bouton d'un paiement en especes faisaient deux paiements sur la
 * facture. L'en-tete {@code Idempotency-Key}, accepte en plus sur les routes de paiement, couvre le
 * cas d'un ecran qui renvoie la meme demande plus tard.</p>
 */
@Slf4j
@Component
public class PaymentDuplicateGuard {

    static final String WINDOW_KEY = "bokati.payment.duplicate-window-seconds";
    static final int DEFAULT_WINDOW_SECONDS = 30;

    @PersistenceContext
    private EntityManager entityManager;

    private PlatformSettings settings;

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    /** Verrou exclusif sur la cle jusqu'a la fin de la transaction en cours. */
    public void lock(String key) {
        if (entityManager == null || key == null) {
            return;
        }
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:k))) l")
                .setParameter("k", "payment:" + key)
                .getSingleResult();
    }

    /** Le paiement identique le plus recent, s'il date de moins que la fenetre. */
    public Optional<PaymentTransaction> recentTwin(Collection<PaymentTransaction> candidates, PaymentMethod method,
                                                   BigDecimal amount, String actor) {
        return recentTwin(candidates, method, amount, actor, Instant.now());
    }

    Optional<PaymentTransaction> recentTwin(Collection<PaymentTransaction> candidates, PaymentMethod method,
                                            BigDecimal amount, String actor, Instant now) {
        int window = windowSeconds();
        if (window <= 0 || candidates == null) {
            return Optional.empty();
        }
        Instant since = now.minusSeconds(window);
        return candidates.stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCEEDED || t.getStatus() == PaymentTransactionStatus.PROCESSING)
                .filter(t -> t.getPaymentMethod() == method)
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(since))
                .filter(t -> amount == null || (t.getAmount() != null && t.getAmount().compareTo(amount) == 0))
                .filter(t -> actor == null || t.getReceivedBy() == null || actor.equalsIgnoreCase(t.getReceivedBy()))
                .max(Comparator.comparing(PaymentTransaction::getCreatedAt))
                .map(t -> {
                    log.info("Paiement {} rendu au lieu d'un doublon (meme mode, montant et auteur, il y a moins de {} s)",
                            t.getTransactionNumber(), window);
                    return t;
                });
    }

    int windowSeconds() {
        return settings == null ? DEFAULT_WINDOW_SECONDS : settings.integer(WINDOW_KEY, DEFAULT_WINDOW_SECONDS);
    }
}
