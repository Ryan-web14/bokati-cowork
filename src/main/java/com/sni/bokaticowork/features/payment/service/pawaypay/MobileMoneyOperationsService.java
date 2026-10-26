package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyCallbackResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * L'outil admin des paiements mobile money · voir chaque paiement, le comparer a ce que PawaPay en
 * dit, et trancher un litige.
 *
 * <p>Trois sources peuvent diverger : notre depot ({@code pawapay_deposit}), notre transaction de
 * paiement, et PawaPay. La fiche les pose cote a cote et nomme chaque ecart, avec l'action qui le
 * resout. Une resolution manuelle passe par le meme traitement qu'un rappel de l'operateur ·
 * affectation a la facture, reservation confirmee, recu, notifications · et ne s'applique qu'a un
 * depot encore en attente ou non resolu. Revenir sur un paiement reussi releve du remboursement.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMoneyOperationsService {

    /** Au plus ce nombre de depots relus chez PawaPay par rapprochement · chaque relecture est un appel. */
    static final int RECONCILIATION_LIMIT = 200;

    private static final Set<String> RESOLVABLE = Set.of("PROCESSING", "SUBMITTED_UNCONFIRMED", "CREATED", "ACCEPTED", "UNRESOLVED");

    private final PawapayDepositRepository depositRepository;
    private final PawapayCallbackRepository callbackRepository;
    private final PawapayDepositService depositService;
    private final MobileMoneySupervisionService supervision;
    private final PawapayCallbackProcessor callbackProcessor;
    private final MobileMoneyResolutionNotifier resolutionNotifier;
    private final ObjectProvider<PawapayClient> client;

    // -----------------------------------------------------------------------------------------
    // Vues
    // -----------------------------------------------------------------------------------------

    public record Filter(Instant from, Instant to, String status, String provider, String customerCode,
                         String phone, String reference) {
    }

    /** Ce que PawaPay dit du depot, relu en direct · {@code available} faux si PawaPay n'a pas pu etre lu. */
    public record ProviderView(boolean available, boolean found, String status, BigDecimal amount, String currency,
                               String providerTransactionId, String failureCode, String failureMessage, String error) {
    }

    /** Un ecart entre nos donnees et celles de PawaPay, avec ce qu'il faut faire. */
    public record Discrepancy(String severity, String code, String message, String suggestedAction) {
    }

    public record Detail(MobileMoneyDepositResponse deposit, String intentStatus, String transactionStatus,
                         ProviderView provider, List<Discrepancy> discrepancies, List<MobileMoneyCallbackResponse> callbacks) {
    }

    public record StatusTotal(String status, String currency, long count, BigDecimal amount) {
    }

    public record ReconciliationLine(MobileMoneyDepositResponse deposit, ProviderView provider, List<Discrepancy> discrepancies) {
    }

    public record Reconciliation(Instant from, Instant to, int checked, boolean truncated, List<ReconciliationLine> discrepancies) {
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<MobileMoneyDepositResponse> search(Filter filter, Pageable pageable) {
        Page<MobileMoneyDepositResponse> page = depositRepository.findAll(specification(filter), pageable)
                .map(depositService::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Transactional(readOnly = true)
    public List<StatusTotal> summary(Instant from, Instant to) {
        return depositRepository.summarize(from, to).stream()
                .map(row -> new StatusTotal((String) row[0], (String) row[1], ((Number) row[2]).longValue(),
                        row[3] instanceof BigDecimal amount ? amount : new BigDecimal(String.valueOf(row[3]))))
                .toList();
    }

    @Transactional(readOnly = true)
    public Detail detail(String depositId) {
        PawapayDeposit deposit = find(depositId);
        ProviderView provider = providerView(deposit.getDepositId());
        String intentStatus = deposit.getPaymentIntent() == null || deposit.getPaymentIntent().getStatus() == null
                ? null : deposit.getPaymentIntent().getStatus().name();
        String transactionStatus = deposit.getPaymentTransaction() == null || deposit.getPaymentTransaction().getStatus() == null
                ? null : deposit.getPaymentTransaction().getStatus().name();
        return new Detail(depositService.toResponse(deposit), intentStatus, transactionStatus, provider,
                discrepancies(deposit, transactionStatus, provider),
                callbackRepository.findByReferenceIdOrderByReceivedAtDesc(deposit.getDepositId()).stream()
                        .map(MobileMoneyCallbackResponse::of).toList());
    }

    // -----------------------------------------------------------------------------------------
    // Rapprochement
    // -----------------------------------------------------------------------------------------

    /**
     * Relit chez PawaPay les depots d'une periode et ne rend que ceux qui divergent.
     *
     * <p>PawaPay n'offre pas de liste de tous ses depots · on relit les notres un a un, au plus
     * {@link #RECONCILIATION_LIMIT}, les plus recents d'abord. Un depot connu de PawaPay et inconnu
     * de nous ne peut apparaitre qu'au releve du tableau de bord PawaPay.</p>
     */
    @Transactional(readOnly = true)
    public Reconciliation reconcile(Instant from, Instant to) {
        List<PawapayDeposit> deposits = depositRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                from, to, PageRequest.of(0, RECONCILIATION_LIMIT + 1));
        boolean truncated = deposits.size() > RECONCILIATION_LIMIT;
        List<ReconciliationLine> lines = new ArrayList<>();
        for (PawapayDeposit deposit : deposits.subList(0, Math.min(deposits.size(), RECONCILIATION_LIMIT))) {
            ProviderView provider = providerView(deposit.getDepositId());
            String transactionStatus = deposit.getPaymentTransaction() == null || deposit.getPaymentTransaction().getStatus() == null
                    ? null : deposit.getPaymentTransaction().getStatus().name();
            List<Discrepancy> found = discrepancies(deposit, transactionStatus, provider);
            if (!found.isEmpty()) {
                lines.add(new ReconciliationLine(depositService.toResponse(deposit), provider, found));
            }
        }
        return new Reconciliation(from, to, Math.min(deposits.size(), RECONCILIATION_LIMIT), truncated, lines);
    }

    // -----------------------------------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------------------------------

    public enum Action { SYNC, MARK_SUCCEEDED, MARK_FAILED }

    /**
     * Tranche un depot.
     *
     * <p>{@code SYNC} relit PawaPay et applique sa reponse · c'est le cas courant, et il suffit des
     * qu'un rappel a ete perdu. {@code MARK_SUCCEEDED} et {@code MARK_FAILED} forcent l'issue quand
     * PawaPay ne tranche pas (depot non resolu, litige avec preuve de l'operateur) : motif obligatoire,
     * reference operateur obligatoire pour un succes, et alerte aux super-administrateurs.</p>
     */
    @Transactional
    public Detail resolve(String depositId, Action action, String reason, String providerTransactionId, String actor) {
        PawapayDeposit deposit = find(depositId);
        if (action == null) {
            throw new BadRequestException("Indiquez l'action : SYNC, MARK_SUCCEEDED ou MARK_FAILED.");
        }
        if (action == Action.SYNC) {
            supervision.recheck(deposit.getDepositId());
            return detail(deposit.getDepositId());
        }
        if (!RESOLVABLE.contains(deposit.getStatus())) {
            throw new BadRequestException("Ce paiement est déjà " + deposit.getStatus()
                    + " · une résolution manuelle ne s'applique qu'à un paiement en attente ou non résolu. "
                    + "Pour revenir sur un paiement réussi, passez par un remboursement.");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 10) {
            throw new BadRequestException("Expliquez la décision en au moins 10 caractères · elle est tracée et transmise aux super-administrateurs.");
        }
        if (action == Action.MARK_SUCCEEDED && !StringUtils.hasText(providerTransactionId)) {
            throw new BadRequestException("Pour déclarer un paiement réussi, indiquez la référence de l'opérateur qui le prouve.");
        }

        ObjectNode failure = null;
        if (action == Action.MARK_FAILED) {
            failure = JsonNodeFactory.instance.objectNode();
            failure.put("failureCode", "MANUAL_RESOLUTION");
            failure.put("failureMessage", reason.trim());
        }
        String status = action == Action.MARK_SUCCEEDED ? "COMPLETED" : "FAILED";
        callbackProcessor.process(new PawapayCallbackPayload(deposit.getDepositId(), status, null, null,
                deposit.getClientReferenceId(), null, null,
                action == Action.MARK_SUCCEEDED ? providerTransactionId.trim() : deposit.getProviderTransactionId(),
                null, null, null, null, null, null, null, failure));
        log.warn("Paiement mobile money {} resolu a la main en {} par {} · {}", deposit.getDepositId(), status, actor, reason.trim());
        resolutionNotifier.resolved(deposit, action.name(), reason.trim(), providerTransactionId, actor);
        return detail(deposit.getDepositId());
    }

    // -----------------------------------------------------------------------------------------
    // Ecarts
    // -----------------------------------------------------------------------------------------

    /** Les ecarts entre notre depot, notre transaction et PawaPay · vide si tout concorde. */
    static List<Discrepancy> discrepancies(PawapayDeposit deposit, String transactionStatus, ProviderView provider) {
        List<Discrepancy> found = new ArrayList<>();
        String ours = deposit.getStatus() == null ? "" : deposit.getStatus();
        boolean oursPaid = "SUCCEEDED".equals(ours);
        boolean oursPending = RESOLVABLE.contains(ours);

        if (!provider.available()) {
            found.add(new Discrepancy("INFO", "PROVIDER_UNAVAILABLE",
                    "PawaPay n'a pas pu être lu : " + provider.error(), "Réessayer plus tard"));
        } else {
            String theirs = provider.found() && provider.status() != null ? provider.status().toUpperCase(Locale.ROOT) : "NOT_FOUND";
            boolean theirsPaid = "COMPLETED".equals(theirs) || "SUCCESSFUL".equals(theirs);
            boolean theirsFailed = Set.of("FAILED", "REJECTED", "EXPIRED", "CANCELLED", "NOT_FOUND").contains(theirs);

            if (theirsPaid && !oursPaid) {
                found.add(new Discrepancy(oursPending ? "WARNING" : "CRITICAL", "PAID_AT_PROVIDER_ONLY",
                        "PawaPay confirme le paiement, notre base le voit " + ours + ".",
                        oursPending ? "SYNC" : "Vérifier : le client a payé mais le paiement est enregistré " + ours));
            }
            if (theirsFailed && oursPaid) {
                found.add(new Discrepancy("CRITICAL", "PAID_HERE_ONLY",
                        "Notre base enregistre le paiement, PawaPay le voit " + theirs + ".",
                        "Ne rien rendre au client avant vérification · contacter PawaPay avec la référence"));
            }
            if (theirsFailed && oursPending) {
                found.add(new Discrepancy("WARNING", "FAILED_AT_PROVIDER",
                        "PawaPay a clos le paiement en " + theirs + ", notre base attend encore.", "SYNC"));
            }
            if (provider.amount() != null && deposit.getAmount() != null
                    && provider.amount().compareTo(deposit.getAmount()) != 0) {
                found.add(new Discrepancy("CRITICAL", "AMOUNT_MISMATCH",
                        "Montant chez PawaPay " + provider.amount().toPlainString() + ", chez nous "
                                + deposit.getAmount().stripTrailingZeros().toPlainString() + ".",
                        "Vérifier le montant réellement prélevé avant toute résolution"));
            }
            if (StringUtils.hasText(provider.currency()) && StringUtils.hasText(deposit.getCurrency())
                    && !provider.currency().equalsIgnoreCase(deposit.getCurrency())) {
                found.add(new Discrepancy("CRITICAL", "CURRENCY_MISMATCH",
                        "Devise chez PawaPay " + provider.currency() + ", chez nous " + deposit.getCurrency() + ".",
                        "Vérifier avant toute résolution"));
            }
        }
        if (oursPaid && transactionStatus != null && !"SUCCEEDED".equals(transactionStatus)) {
            found.add(new Discrepancy("CRITICAL", "TRANSACTION_NOT_SETTLED",
                    "Le dépôt est réussi mais la transaction de paiement est " + transactionStatus + ".",
                    "Vérifier l'affectation à la facture"));
        }
        if ("UNRESOLVED".equals(ours)) {
            found.add(new Discrepancy("WARNING", "UNRESOLVED",
                    "Aucune réponse définitive de l'opérateur après le délai de suivi.",
                    "SYNC, puis MARK_SUCCEEDED ou MARK_FAILED avec la preuve de l'opérateur"));
        }
        return found;
    }

    ProviderView providerView(String depositId) {
        PawapayClient pawapay = client.getIfAvailable();
        if (pawapay == null) {
            return new ProviderView(false, false, null, null, null, null, null, null, "PawaPay n'est pas activé");
        }
        try {
            PawapayClient.DepositStatus status = pawapay.getDepositStatus(depositId);
            if (!status.found()) {
                return new ProviderView(true, false, "NOT_FOUND", null, null, null, null, null, null);
            }
            JsonNode raw = status.raw();
            JsonNode failure = status.failureReason();
            return new ProviderView(true, true, status.providerStatus(), decimal(raw, "amount"), text(raw, "currency"),
                    status.providerTransactionId(), text(failure, "failureCode"), text(failure, "failureMessage"), null);
        } catch (RuntimeException ex) {
            log.warn("Lecture PawaPay du depot {} impossible : {}", depositId, ex.getMessage());
            return new ProviderView(false, false, null, null, null, null, null, null, ex.getMessage());
        }
    }

    private PawapayDeposit find(String depositId) {
        return depositRepository.findByDepositId(depositId == null ? "" : depositId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Paiement mobile money introuvable : " + depositId));
    }

    private static Specification<PawapayDeposit> specification(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (f.from() != null) where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            if (f.to() != null) where.add(cb.lessThan(root.get("createdAt"), f.to()));
            if (StringUtils.hasText(f.status())) where.add(cb.equal(root.get("status"), f.status().trim().toUpperCase(Locale.ROOT)));
            if (StringUtils.hasText(f.provider())) where.add(cb.equal(root.get("provider"), f.provider().trim().toUpperCase(Locale.ROOT)));
            if (StringUtils.hasText(f.customerCode())) where.add(cb.equal(root.get("customerCode"), f.customerCode().trim()));
            if (StringUtils.hasText(f.phone())) {
                where.add(cb.like(root.get("phoneNumber"), "%" + f.phone().replaceAll("[^0-9]", "") + "%"));
            }
            if (StringUtils.hasText(f.reference())) {
                String ref = f.reference().trim();
                where.add(cb.or(cb.equal(root.get("depositId"), ref), cb.equal(root.get("providerTransactionId"), ref),
                        cb.equal(root.get("intentNumber"), ref), cb.equal(root.get("transactionNumber"), ref),
                        cb.equal(root.get("clientReferenceId"), ref)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Le tri par defaut de la liste · le plus recent d'abord. */
    public static Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
