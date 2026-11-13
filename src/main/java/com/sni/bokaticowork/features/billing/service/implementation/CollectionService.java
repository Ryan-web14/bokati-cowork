package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.billing.dto.CollectionDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateInstallmentInput;
import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleResponse;
import com.sni.bokaticowork.features.billing.dunning.service.DunningHold;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.PaymentPromise;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.PaymentPromiseRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Lot C12 : recouvrement. Une promesse de paiement datee suspend les relances jusqu'a sa date ; a
 * echeance, elle est tenue si le reste du a baisse du montant promis, sinon rompue, et les relances
 * reprennent. Un plan de reglement transforme le reste du en echeancier (lot B9) ; au-dela de
 * {@value #PLAN_MONTHS_KEY} mois, il demande le droit d'approbation de la facturation (N+1).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CollectionService implements DunningHold {

    static final String PLAN_MONTHS_KEY = "billing.collection.plan-approval-months";
    static final String OPEN = "OPEN";
    private static final Set<String> CHANNELS = Set.of("PHONE", "EMAIL", "WHATSAPP", "VISIT", "PORTAL");
    private static final Set<BillingDocumentStatus> COLLECTIBLE = EnumSet.of(BillingDocumentStatus.ISSUED, BillingDocumentStatus.SENT,
            BillingDocumentStatus.VALIDATED, BillingDocumentStatus.PARTIALLY_PAID, BillingDocumentStatus.OVERDUE);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaymentPromiseRepository promises;
    private final BillingDocumentRepository documents;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<PaymentScheduleService> schedules;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    // ================================================================== promesses

    public CollectionDtos.PromiseView promise(String documentNumber, CollectionDtos.PromiseRequest request) {
        BillingDocument invoice = collectible(documentNumber);
        if (request.promisedDate().isBefore(LocalDate.now())) {
            throw BadRequestException.coded("PROMESSE_INVALIDE", "La date promise est passée.");
        }
        BigDecimal due = invoice.getBalanceDue();
        BigDecimal amount = request.amount() != null ? request.amount() : due;
        if (amount.compareTo(due) > 0) {
            throw BadRequestException.coded("PROMESSE_INVALIDE", "Le montant promis dépasse le reste dû (" + due.stripTrailingZeros().toPlainString() + ").");
        }
        String channel = StringUtils.hasText(request.channel()) ? request.channel().trim().toUpperCase(Locale.ROOT) : "PHONE";
        if (!CHANNELS.contains(channel)) {
            throw BadRequestException.coded("PROMESSE_INVALIDE", "Canal : PHONE, EMAIL, WHATSAPP, VISIT ou PORTAL.");
        }
        // Une nouvelle promesse remplace celle en cours.
        promises.findByDocumentNumberAndStatus(invoice.getDocumentNumber(), OPEN).forEach(previous -> close(previous, "CANCELLED"));
        PaymentPromise saved = promises.save(PaymentPromise.builder().promiseNumber(sequences.next("billing_payment_promise"))
                .documentNumber(invoice.getDocumentNumber()).customerType(invoice.getCustomerType()).customerCode(invoice.getCustomerCode())
                .promisedDate(request.promisedDate()).amount(amount).balanceAtPromise(due).channel(channel)
                .note(StringUtils.hasText(request.note()) ? request.note().trim() : null).status(OPEN)
                .createdBy(staffProfiles.currentAuthor().login()).build());
        return view(saved);
    }

    public CollectionDtos.PromiseView cancelPromise(String promiseNumber) {
        PaymentPromise promise = promises.findByPromiseNumber(promiseNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Promesse introuvable : " + promiseNumber));
        if (!OPEN.equals(promise.getStatus())) {
            throw ConflictException.coded("PROMESSE_CLOSE", "La promesse " + promiseNumber + " est déjà close.");
        }
        return view(close(promise, "CANCELLED"));
    }

    @Transactional
    public List<CollectionDtos.PromiseView> promises(String documentNumber, boolean openOnly) {
        if (StringUtils.hasText(documentNumber)) {
            holdReason(documentNumber.trim());
            return promises.findByDocumentNumberOrderByCreatedAtDesc(documentNumber.trim()).stream()
                    .filter(promise -> !openOnly || OPEN.equals(promise.getStatus())).map(this::view).toList();
        }
        return promises.findByStatusOrderByPromisedDateAsc(OPEN).stream().map(this::view).toList();
    }

    /**
     * Relance suspendue tant qu'une promesse court ; a echeance, la promesse est jugee tenue ou
     * rompue, et la relance reprend.
     */
    @Override
    public Optional<String> holdReason(String documentNumber) {
        if (documentNumber == null) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now();
        Optional<String> reason = Optional.empty();
        for (PaymentPromise promise : promises.findByDocumentNumberAndStatus(documentNumber, OPEN)) {
            if (!promise.getPromisedDate().isBefore(today)) {
                reason = Optional.of("Promesse de paiement au " + DAY.format(promise.getPromisedDate()) + " : relance suspendue jusque-là");
                continue;
            }
            BigDecimal due = documents.findByDocumentNumber(documentNumber).map(BillingDocument::getBalanceDue).orElse(BigDecimal.ZERO);
            boolean kept = due.compareTo(promise.getBalanceAtPromise().subtract(promise.getAmount())) <= 0;
            close(promise, kept ? "KEPT" : "BROKEN");
        }
        return reason;
    }

    // ================================================================== plan de reglement

    /** Plan de reglement : le reste du en echeances egales (la derniere porte l'arrondi). */
    public PaymentScheduleResponse plan(String documentNumber, CollectionDtos.PlanRequest request) {
        BillingDocument invoice = collectible(documentNumber);
        int interval = request.intervalMonths() == null ? 1 : request.intervalMonths();
        int months = (request.installments() - 1) * interval;
        int threshold = platformSettings == null ? 3 : platformSettings.integer(PLAN_MONTHS_KEY, 3);
        if (months > threshold && !hasAuthority("BILLING:APPROVE")) {
            throw new ForbiddenException("Un plan de plus de " + threshold + " mois demande l'accord d'un responsable (droit d'approbation de la facturation).");
        }
        BigDecimal due = invoice.getBalanceDue();
        BigDecimal part = due.divide(BigDecimal.valueOf(request.installments()), 0, RoundingMode.DOWN);
        List<CreateInstallmentInput> installments = new ArrayList<>();
        for (int index = 0; index < request.installments(); index++) {
            BigDecimal amount = index == request.installments() - 1 ? due.subtract(part.multiply(BigDecimal.valueOf(index))) : part;
            installments.add(new CreateInstallmentInput("Échéance " + (index + 1) + " sur " + request.installments(), amount,
                    request.firstDueDate().plusMonths((long) index * interval)));
        }
        PaymentScheduleService service = schedules.getObject();
        return service.createSchedule(invoice.getDocumentNumber(), new CreatePaymentScheduleRequest(installments,
                "Plan de règlement" + (StringUtils.hasText(request.notes()) ? " : " + request.notes().trim() : "")));
    }

    // ================================================================== outils

    private BillingDocument collectible(String documentNumber) {
        BillingDocument invoice = documents.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + documentNumber));
        if (!COLLECTIBLE.contains(invoice.getStatus()) || invoice.getBalanceDue() == null || invoice.getBalanceDue().signum() <= 0) {
            throw BadRequestException.coded("RECOUVREMENT_IMPOSSIBLE", "La facture " + documentNumber + " n'a pas de reste dû à recouvrer.");
        }
        return invoice;
    }

    private PaymentPromise close(PaymentPromise promise, String status) {
        promise.setStatus(status);
        promise.setClosedAt(Instant.now());
        return promises.save(promise);
    }

    private static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority())
                || authority.replace(':', '_').equals(granted.getAuthority()) || "ROLE_SUPER_ADMIN".equals(granted.getAuthority()));
    }

    private CollectionDtos.PromiseView view(PaymentPromise promise) {
        return new CollectionDtos.PromiseView(promise.getPromiseNumber(), promise.getDocumentNumber(), promise.getCustomerType(),
                promise.getCustomerCode(), promise.getPromisedDate(), promise.getAmount(), promise.getBalanceAtPromise(), promise.getChannel(),
                promise.getNote(), promise.getStatus(), promise.getCreatedBy(), promise.getClosedAt(), promise.getCreatedAt());
    }
}
