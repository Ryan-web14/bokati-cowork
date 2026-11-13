package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.WithholdingDtos;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.Withholding;
import com.sni.bokaticowork.features.billing.model.WithholdingType;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.WithholdingRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lot C6 : retenues a la source. Une administration ou une grande entreprise paie une facture
 * sous deduction d'une retenue (TVA retenue, precompte) qu'elle verse a l'Etat. La retenue solde la
 * facture a hauteur de son montant, comme un reglement ; la comptabilite passe D compte de la
 * retenue / C 411 du client. L'attestation de retenue est attendue puis rattachee.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WithholdingService {

    static final String RECORDED = "RECORDED";
    static final String CANCELLED = "CANCELLED";
    private static final Set<BillingDocumentType> WITHHOLDABLE = EnumSet.of(BillingDocumentType.INVOICE,
            BillingDocumentType.CORRECTIVE_INVOICE, BillingDocumentType.DEBIT_NOTE);
    private static final Set<BillingDocumentStatus> NOT_PAYABLE = EnumSet.of(BillingDocumentStatus.DRAFT, BillingDocumentStatus.CANCELLED);

    private final WithholdingRepository withholdings;
    private final com.sni.bokaticowork.features.billing.repository.WithholdingTypeRepository types;
    private final BillingDocumentRepository documents;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentServiceImpl> billingServices;

    // ================================================================== retenues

    @Transactional
    public WithholdingDtos.WithholdingView record(String documentNumber, WithholdingDtos.RecordRequest request) {
        BillingDocument invoice = documents.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + documentNumber));
        if (!WITHHOLDABLE.contains(invoice.getDocumentType()) || NOT_PAYABLE.contains(invoice.getStatus())) {
            throw BadRequestException.coded("RETENUE_IMPOSSIBLE", "Une retenue se saisit sur une facture émise (" + documentNumber + " : "
                    + invoice.getDocumentType() + " " + invoice.getStatus() + ").");
        }
        WithholdingType type = type(request.typeCode());
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw BadRequestException.coded("RETENUE_IMPOSSIBLE", "Le type de retenue « " + type.getLabel() + " » est désactivé.");
        }
        BigDecimal base = base(invoice, type.getBase());
        BigDecimal amount = request.amount() != null ? request.amount()
                : type.getRate() == null ? null : base.multiply(type.getRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (amount == null || amount.signum() <= 0) {
            throw BadRequestException.coded("RETENUE_MONTANT", "Indiquez le montant de la retenue « " + type.getLabel() + " » (type sans taux).");
        }
        BigDecimal due = invoice.getBalanceDue() == null ? BigDecimal.ZERO : invoice.getBalanceDue();
        if (amount.compareTo(due) > 0) {
            throw BadRequestException.coded("RETENUE_MONTANT", "La retenue (" + plain(amount) + ") dépasse le reste dû de la facture ("
                    + plain(due) + ").");
        }
        Withholding saved = withholdings.save(Withholding.builder()
                .withholdingNumber(sequences.next("billing_withholding"))
                .documentNumber(invoice.getDocumentNumber()).typeCode(type.getCode()).account(type.getAccount())
                .baseAmount(base).rate(request.amount() == null ? type.getRate() : null).amount(amount)
                .currency(invoice.getCurrency() == null ? "XAF" : invoice.getCurrency())
                .customerType(invoice.getCustomerType()).customerCode(invoice.getCustomerCode())
                .certificateReference(trim(request.certificateReference())).certificateDocument(trim(request.certificateDocument()))
                .certificateAt(StringUtils.hasText(request.certificateDocument()) ? Instant.now() : null)
                .status(RECORDED).withheldOn(request.withheldOn() != null ? request.withheldOn() : LocalDate.now())
                .recordedBy(staffProfiles.currentAuthor().login())
                .build());
        // La retenue regle la facture a hauteur de son montant (echeancier et acomptes suivent).
        billingServices.getObject().applyPayment(invoice.getDocumentNumber(), amount);
        return view(saved, type);
    }

    /** Attestation recue : la retenue sort de la liste des attestations attendues. */
    @Transactional
    public WithholdingDtos.WithholdingView certificate(String number, WithholdingDtos.CertificateRequest request) {
        Withholding withholding = withholding(number);
        if (CANCELLED.equals(withholding.getStatus())) {
            throw ConflictException.coded("RETENUE_ANNULEE", "La retenue " + number + " est annulée.");
        }
        if (StringUtils.hasText(request.certificateReference())) {
            withholding.setCertificateReference(request.certificateReference().trim());
        }
        withholding.setCertificateDocument(request.certificateDocument().trim());
        withholding.setCertificateAt(Instant.now());
        return view(withholdings.save(withholding), null);
    }

    /** Annulation (erreur de saisie) : la facture redevient due d'autant ; l'ecriture est contre-passee. */
    @Transactional
    public WithholdingDtos.WithholdingView cancel(String number, String reason) {
        Withholding withholding = withholding(number);
        if (CANCELLED.equals(withholding.getStatus())) {
            throw ConflictException.coded("RETENUE_ANNULEE", "La retenue " + number + " est déjà annulée.");
        }
        billingServices.getObject().reversePayment(withholding.getDocumentNumber(), withholding.getAmount());
        withholding.setStatus(CANCELLED);
        withholding.setCancelledAt(Instant.now());
        withholding.setCancelledBy(staffProfiles.currentAuthor().login());
        withholding.setCancelReason(reason.trim());
        return view(withholdings.save(withholding), null);
    }

    @Transactional(readOnly = true)
    public List<WithholdingDtos.WithholdingView> ofDocument(String documentNumber) {
        return withholdings.findByDocumentNumberOrderByCreatedAtAsc(documentNumber).stream().map(w -> view(w, null)).toList();
    }

    /**
     * Liste : d'un client, ou celles dont l'attestation manque ({@code certificate=missing}), ou
     * les plus recentes.
     */
    @Transactional(readOnly = true)
    public List<WithholdingDtos.WithholdingView> list(String customerType, String customerCode, String certificate) {
        List<Withholding> found = "missing".equalsIgnoreCase(certificate) ? withholdings.findAwaitingCertificate()
                : StringUtils.hasText(customerType) && StringUtils.hasText(customerCode)
                ? withholdings.findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(customerType, customerCode)
                : withholdings.findTop200ByOrderByCreatedAtDesc();
        return found.stream().map(w -> view(w, null)).toList();
    }

    // ================================================================== types

    @Transactional(readOnly = true)
    public List<WithholdingDtos.TypeView> types(boolean includeInactive) {
        return types.findAllOrdered().stream().filter(type -> includeInactive || Boolean.TRUE.equals(type.getActive())).map(WithholdingService::view).toList();
    }

    @Transactional
    public WithholdingDtos.TypeView createType(WithholdingDtos.TypeRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (types.findByCode(code).isPresent()) {
            throw ConflictException.coded("REFERENCE_EXISTANTE", "Le type de retenue " + code + " existe déjà.");
        }
        return view(types.save(WithholdingType.builder().code(code).label(request.label().trim()).rate(rate(request.rate()))
                .base(base(request.base())).account(request.account().trim())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder()).build()));
    }

    @Transactional
    public WithholdingDtos.TypeView updateType(String code, WithholdingDtos.TypeUpdateRequest request) {
        WithholdingType type = type(code);
        if (StringUtils.hasText(request.label())) {
            type.setLabel(request.label().trim());
        }
        if (request.rate() != null) {
            type.setRate(rate(request.rate()));
        }
        if (StringUtils.hasText(request.base())) {
            type.setBase(base(request.base()));
        }
        if (StringUtils.hasText(request.account())) {
            type.setAccount(request.account().trim());
        }
        if (request.displayOrder() != null) {
            type.setDisplayOrder(request.displayOrder());
        }
        return view(types.save(type));
    }

    @Transactional
    public WithholdingDtos.TypeView setTypeActive(String code, boolean active) {
        WithholdingType type = type(code);
        type.setActive(active);
        return view(types.save(type));
    }

    // ================================================================== outils

    /** Base de la retenue : TTC, HT (total moins taxes) ou TVA de la facture. */
    static BigDecimal base(BillingDocument invoice, String base) {
        BigDecimal total = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        BigDecimal taxes = invoice.getTaxAmount() == null ? BigDecimal.ZERO : invoice.getTaxAmount();
        return switch (base) {
            case "TVA" -> invoice.getVatAmount() == null ? BigDecimal.ZERO : invoice.getVatAmount();
            case "HT" -> total.subtract(taxes);
            default -> total;
        };
    }

    private WithholdingType type(String code) {
        return types.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Type de retenue introuvable : " + code));
    }

    private Withholding withholding(String number) {
        return withholdings.findByWithholdingNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Retenue introuvable : " + number));
    }

    private static BigDecimal rate(BigDecimal rate) {
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw BadRequestException.coded("REFERENCE_INVALIDE", "Le taux d'une retenue est compris entre 0 et 100.");
        }
        return rate;
    }

    private static String base(String base) {
        String value = base.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TTC", "HT", "TVA").contains(value)) {
            throw BadRequestException.coded("REFERENCE_INVALIDE", "La base d'une retenue vaut TTC, HT ou TVA.");
        }
        return value;
    }

    private WithholdingDtos.WithholdingView view(Withholding withholding, WithholdingType known) {
        WithholdingType type = known != null ? known : types.findByCode(withholding.getTypeCode()).orElse(null);
        return new WithholdingDtos.WithholdingView(withholding.getWithholdingNumber(), withholding.getDocumentNumber(), withholding.getTypeCode(),
                type == null ? withholding.getTypeCode() : type.getLabel(), withholding.getAccount(), withholding.getBaseAmount(),
                withholding.getRate(), withholding.getAmount(), withholding.getCurrency(), withholding.getCustomerType(),
                withholding.getCustomerCode(), withholding.getCertificateReference(), withholding.getCertificateDocument(),
                withholding.getCertificateAt(), RECORDED.equals(withholding.getStatus()) && withholding.getCertificateDocument() == null,
                withholding.getStatus(), withholding.getWithheldOn(), withholding.getRecordedBy(), withholding.getCancelledAt(),
                withholding.getCancelReason(), withholding.getCreatedAt());
    }

    private static WithholdingDtos.TypeView view(WithholdingType type) {
        return new WithholdingDtos.TypeView(type.getCode(), type.getLabel(), type.getRate(), type.getBase(), type.getAccount(),
                type.getDisplayOrder(), Boolean.TRUE.equals(type.getActive()));
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
