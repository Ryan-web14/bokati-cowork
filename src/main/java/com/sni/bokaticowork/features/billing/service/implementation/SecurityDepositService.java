package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.SecurityDepositDtos;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.SecurityDeposit;
import com.sni.bokaticowork.features.billing.model.SecurityDepositMovement;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositMovementRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lot C7 : depots de garantie (cautions). Une ligne de caution (caution d'abonnement, ou article
 * du catalogue marque comme caution) n'est pas taxable et part au compte 165 a la facture. La
 * caution s'inscrit au registre a l'emission ; une fois la facture encaissee, elle se restitue,
 * se retient avec un motif, ou s'impute sur une autre facture du client.
 *
 * <p>Une caution d'abonnement encore couverte par sa retenue de portefeuille se regle par la
 * sortie d'abonnement (rubrique « Caution liberee ou imputee »), pas par ce registre : sinon elle
 * serait rendue deux fois.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SecurityDepositService {

    static final String HELD = "HELD";
    static final String PARTIALLY_SETTLED = "PARTIALLY_SETTLED";
    static final String SETTLED = "SETTLED";
    static final String RELEASE = "RELEASE";
    static final String RETAIN = "RETAIN";
    static final String APPLY = "APPLY";
    static final String SUBSCRIPTION_DEPOSIT = "DEPOSIT";
    /** Source des mouvements de portefeuille d'une caution (ecriture D 165 / C portefeuille). */
    public static final String WALLET_SOURCE = "SECURITY_DEPOSIT";
    private static final Set<BillingDocumentStatus> OPEN = EnumSet.of(BillingDocumentStatus.ISSUED, BillingDocumentStatus.SENT,
            BillingDocumentStatus.VALIDATED, BillingDocumentStatus.PARTIALLY_PAID, BillingDocumentStatus.OVERDUE);

    private final SecurityDepositRepository deposits;
    private final SecurityDepositMovementRepository movements;
    private final BillingDocumentRepository documents;
    private final BillingDocumentLineRepository lines;
    private final ServiceCatalogItemRepository catalogItems;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentServiceImpl> billingServices;
    private final ObjectProvider<WalletHoldRepository> walletHolds;

    /** Decision CD6 : credit du portefeuille (facultatif dans les tests). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sni.bokaticowork.features.payment.service.support.WalletLedgerService walletLedger;

    /** Decision CD10 : lecture des factures anterieures (facultatif dans les tests). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** Ligne de caution : caution d'abonnement (hors facture d'acompte) ou article du catalogue marque comme caution. */
    public static boolean isSecurityDepositLine(BillingDocumentLine line, BillingDocument document, ServiceCatalogItemRepository catalog) {
        if (line == null || Boolean.TRUE.equals(line.getOptional())) {
            return false;
        }
        if (SUBSCRIPTION_DEPOSIT.equalsIgnoreCase(line.getSourceType())
                && (document == null || !StringUtils.hasText(document.getDepositForDocumentNumber()))
                && line.getLineType() != com.sni.bokaticowork.features.billing.enums.BillingLineType.ADJUSTMENT) {
            return true;
        }
        return catalog != null && StringUtils.hasText(line.getItemCode())
                && catalog.findByItemCode(line.getItemCode().trim()).map(ServiceCatalogItem::getSecurityDeposit).orElse(false);
    }

    // ================================================================== registre

    /** Facture emise : ses lignes de caution forment une caution au registre (une par facture). */
    public void registerFromInvoice(BillingDocument invoice) {
        if (invoice.getDocumentType() != BillingDocumentType.INVOICE || StringUtils.hasText(invoice.getDepositForDocumentNumber())
                || deposits.findByDocumentNumber(invoice.getDocumentNumber()).isPresent()) {
            return;
        }
        BigDecimal amount = BigDecimal.ZERO;
        String subscription = null;
        for (BillingDocumentLine line : lines.findAllByDocumentOrderByLineOrderAscIdAsc(invoice)) {
            if (isSecurityDepositLine(line, invoice, catalogItems)) {
                amount = amount.add(line.getTotalAmount() == null ? BigDecimal.ZERO : line.getTotalAmount());
                if (SUBSCRIPTION_DEPOSIT.equalsIgnoreCase(line.getSourceType()) && StringUtils.hasText(line.getSourceCode())) {
                    subscription = line.getSourceCode().trim();
                }
            }
        }
        if (amount.signum() <= 0) {
            return;
        }
        deposits.save(SecurityDeposit.builder().depositNumber(sequences.next("billing_security_deposit"))
                .documentNumber(invoice.getDocumentNumber()).subscriptionNumber(subscription)
                .customerType(invoice.getCustomerType()).customerCode(invoice.getCustomerCode()).customerName(invoice.getCustomerName())
                .amount(amount).currency(invoice.getCurrency() == null ? "XAF" : invoice.getCurrency()).status(HELD).build());
    }

    @Transactional(readOnly = true)
    public List<SecurityDepositDtos.DepositView> list(String customerType, String customerCode, boolean openOnly) {
        List<SecurityDeposit> found = StringUtils.hasText(customerType) && StringUtils.hasText(customerCode)
                ? deposits.findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(customerType, customerCode)
                : deposits.findByStatusInOrderByCreatedAtDesc(openOnly ? List.of(HELD, PARTIALLY_SETTLED)
                : List.of(HELD, PARTIALLY_SETTLED, SETTLED));
        return found.stream().filter(deposit -> !openOnly || deposit.remaining().signum() > 0).map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public SecurityDepositDtos.DepositView get(String number) {
        return view(deposit(number));
    }

    // ================================================================== mouvements

    /**
     * Restitution, retenue ou imputation. La facture de la caution doit etre encaissee ; le montant
     * ne depasse pas le reste ; une retenue demande son motif ; une imputation sa facture cible.
     */
    public SecurityDepositDtos.DepositView move(String number, SecurityDepositDtos.MovementRequest request) {
        SecurityDeposit deposit = deposit(number);
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase(Locale.ROOT);
        if (!Set.of(RELEASE, RETAIN, APPLY).contains(kind)) {
            throw BadRequestException.coded("CAUTION_INVALIDE", "kind vaut RELEASE (restitution), RETAIN (retenue) ou APPLY (imputation).");
        }
        BillingDocument invoice = documents.findByDocumentNumber(deposit.getDocumentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + deposit.getDocumentNumber()));
        if (invoice.getBalanceDue() != null && invoice.getBalanceDue().signum() > 0) {
            throw ConflictException.coded("CAUTION_NON_ENCAISSEE", "La facture " + invoice.getDocumentNumber()
                    + " qui porte la caution n'est pas soldée : rien à restituer, retenir ou imputer avant son encaissement.");
        }
        if (StringUtils.hasText(deposit.getSubscriptionNumber()) && subscriptionHoldActive(deposit.getSubscriptionNumber())) {
            throw ConflictException.coded("CAUTION_ABONNEMENT", "La caution de l'abonnement " + deposit.getSubscriptionNumber()
                    + " se règle à la sortie de l'abonnement (rubrique « Caution libérée ou imputée »).");
        }
        BigDecimal amount = request.amount() != null ? request.amount() : deposit.remaining();
        if (amount.signum() <= 0 || amount.compareTo(deposit.remaining()) > 0) {
            throw BadRequestException.coded("CAUTION_MONTANT", "Le montant doit être positif et ne pas dépasser le reste de la caution ("
                    + deposit.remaining().stripTrailingZeros().toPlainString() + ").");
        }
        if (RETAIN.equals(kind) && !StringUtils.hasText(request.reason())) {
            throw BadRequestException.coded("MOTIF_REQUIS", "Une retenue sur caution demande son motif (dégâts, impayés).");
        }
        String target = null;
        if (APPLY.equals(kind)) {
            target = imputationTarget(deposit, request.targetDocumentNumber(), amount);
            billingServices.getObject().applyPayment(target, amount);
            deposit.setAppliedAmount(deposit.getAppliedAmount().add(amount));
        } else if (RETAIN.equals(kind)) {
            deposit.setRetainedAmount(deposit.getRetainedAmount().add(amount));
        } else {
            deposit.setReleasedAmount(deposit.getReleasedAmount().add(amount));
        }
        movements.save(SecurityDepositMovement.builder().movementNumber(sequences.next("billing_security_deposit_movement"))
                .depositId(deposit.getId()).kind(kind).amount(amount).reason(StringUtils.hasText(request.reason()) ? request.reason().trim() : null)
                .targetDocumentNumber(target).movementDate(LocalDate.now()).createdBy(staffProfiles.currentAuthor().login()).build());
        deposit.setStatus(deposit.remaining().signum() == 0 ? SETTLED : PARTIALLY_SETTLED);
        return view(deposits.save(deposit));
    }

    // ================================================================== caution d'abonnement (CD6)

    /**
     * Facture soldee : la caution d'abonnement qu'elle porte est creditee sur le portefeuille du
     * client, ou la retenue posee a la creation de l'abonnement la bloque. Une seule fois par caution.
     */
    public void onInvoicePaid(BillingDocument invoice) {
        if (walletLedger == null || invoice.getBalanceDue() == null || invoice.getBalanceDue().signum() > 0) {
            return;
        }
        deposits.findByDocumentNumber(invoice.getDocumentNumber())
                .filter(deposit -> StringUtils.hasText(deposit.getSubscriptionNumber()) && deposit.getWalletEntryNumber() == null)
                .ifPresent(deposit -> {
                    WalletHoldRepository holds = walletHolds.getIfAvailable();
                    if (holds == null) {
                        return;
                    }
                    holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", deposit.getSubscriptionNumber()).stream()
                            .findFirst()
                            .ifPresent(hold -> {
                                var entry = walletLedger.credit(hold.getWallet(), deposit.getAmount(),
                                        com.sni.bokaticowork.features.payment.enums.WalletEntryType.ADJUSTMENT, WALLET_SOURCE,
                                        deposit.getDepositNumber(), invoice.getDocumentNumber(), "SYSTEM", "CAUTION:" + deposit.getDepositNumber());
                                deposit.setWalletEntryNumber(entry.getEntryNumber());
                                deposits.save(deposit);
                            });
                });
    }

    /**
     * Sortie d'abonnement (reglement de la retenue) : les factures reglees par la caution et ce qui
     * revient au portefeuille s'inscrivent au registre, sans ecriture propre (le portefeuille porte
     * deja ces mouvements).
     */
    public void onSubscriptionSettled(String subscriptionNumber, List<String[]> paidInvoices, BigDecimal returned) {
        if (!StringUtils.hasText(subscriptionNumber)) {
            return;
        }
        deposits.findBySubscriptionNumberOrderByCreatedAtAsc(subscriptionNumber).stream()
                .filter(deposit -> deposit.remaining().signum() > 0).findFirst().ifPresent(deposit -> {
                    for (String[] paid : paidInvoices) {
                        BigDecimal amount = new BigDecimal(paid[1]).min(deposit.remaining());
                        if (amount.signum() > 0) {
                            walletMovement(deposit, APPLY, amount, paid[0]);
                            deposit.setAppliedAmount(deposit.getAppliedAmount().add(amount));
                        }
                    }
                    BigDecimal back = returned == null ? BigDecimal.ZERO : returned.min(deposit.remaining());
                    if (back.signum() > 0) {
                        walletMovement(deposit, RELEASE, back, null);
                        deposit.setReleasedAmount(deposit.getReleasedAmount().add(back));
                    }
                    deposit.setStatus(deposit.remaining().signum() == 0 ? SETTLED : PARTIALLY_SETTLED);
                    deposits.save(deposit);
                });
    }

    private void walletMovement(SecurityDeposit deposit, String kind, BigDecimal amount, String target) {
        movements.save(SecurityDepositMovement.builder().movementNumber(sequences.next("billing_security_deposit_movement"))
                .depositId(deposit.getId()).kind(kind).amount(amount).reason("Sortie d'abonnement, réglé par le portefeuille")
                .targetDocumentNumber(target).movementDate(LocalDate.now()).createdBy("SYSTEM").walletSettled(true).build());
    }

    // ================================================================== reprise (CD10)

    /**
     * Cautions facturees avant le lot C7 : ligne de caution taxable, comptabilisee en produit avec de
     * la TVA. Liste pour le comptable, qui passe lui-meme les ecritures de reclassement (706 vers 165,
     * TVA a regulariser).
     */
    @Transactional(readOnly = true)
    public List<SecurityDepositDtos.LegacyLine> legacy() {
        if (jdbc == null) {
            return List.of();
        }
        return jdbc.queryForList("""
                SELECT d.document_number, d.customer_code, d.customer_name, d.issue_date, l.description, l.total_amount, l.taxable_amount,
                       l.vat_amount, l.additional_cent_amount,
                       EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = d.document_number) AS posted
                FROM billing_document_line l JOIN billing_document d ON d.id = l.document_id
                WHERE l.source_type = 'DEPOSIT' AND l.taxable = TRUE AND d.deposit_for_document_number IS NULL
                  AND d.document_type = 'INVOICE' AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL
                ORDER BY d.issue_date
                """).stream().map(row -> new SecurityDepositDtos.LegacyLine((String) row.get("document_number"), (String) row.get("customer_code"),
                (String) row.get("customer_name"), row.get("issue_date") instanceof java.sql.Date day ? day.toLocalDate() : null,
                (String) row.get("description"), (BigDecimal) row.get("total_amount"), (BigDecimal) row.get("taxable_amount"),
                ((BigDecimal) row.get("vat_amount")).add(row.get("additional_cent_amount") == null ? BigDecimal.ZERO
                        : (BigDecimal) row.get("additional_cent_amount")), Boolean.TRUE.equals(row.get("posted")))).toList();
    }

    // ================================================================== catalogue

    /** Marque un article du catalogue comme caution : ses lignes ne sont pas taxables et partent au 165. */
    public void flagCatalogItem(String itemCode, boolean securityDeposit) {
        ServiceCatalogItem item = catalogItems.findByItemCode(itemCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Article du catalogue introuvable : " + itemCode));
        item.setSecurityDeposit(securityDeposit);
        catalogItems.save(item);
    }

    // ================================================================== outils

    private String imputationTarget(SecurityDeposit deposit, String targetNumber, BigDecimal amount) {
        if (!StringUtils.hasText(targetNumber)) {
            throw BadRequestException.coded("CAUTION_INVALIDE", "Indiquez la facture sur laquelle imputer la caution (targetDocumentNumber).");
        }
        BillingDocument target = documents.findByDocumentNumber(targetNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + targetNumber));
        boolean sameCustomer = deposit.getCustomerCode() != null && deposit.getCustomerCode().equals(target.getCustomerCode());
        if (target.getDocumentType() != BillingDocumentType.INVOICE || !OPEN.contains(target.getStatus()) || !sameCustomer) {
            throw BadRequestException.coded("CAUTION_INVALIDE", "La caution s'impute sur une facture ouverte du même client.");
        }
        if (target.getBalanceDue() == null || amount.compareTo(target.getBalanceDue()) > 0) {
            throw BadRequestException.coded("CAUTION_MONTANT", "Le montant dépasse le reste dû de la facture " + target.getDocumentNumber() + ".");
        }
        return target.getDocumentNumber();
    }

    private boolean subscriptionHoldActive(String subscriptionNumber) {
        WalletHoldRepository holds = walletHolds.getIfAvailable();
        return holds != null && !holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", subscriptionNumber).isEmpty();
    }

    private SecurityDeposit deposit(String number) {
        return deposits.findByDepositNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caution introuvable : " + number));
    }

    private SecurityDepositDtos.DepositView view(SecurityDeposit deposit) {
        List<SecurityDepositDtos.MovementView> moves = deposit.getId() == null ? List.of()
                : movements.findByDepositIdOrderByCreatedAtAsc(deposit.getId()).stream()
                .map(move -> new SecurityDepositDtos.MovementView(move.getMovementNumber(), move.getKind(), move.getAmount(), move.getReason(),
                        move.getTargetDocumentNumber(), move.getMovementDate(), move.getCreatedBy(), move.getCreatedAt())).toList();
        return new SecurityDepositDtos.DepositView(deposit.getDepositNumber(), deposit.getDocumentNumber(), deposit.getSubscriptionNumber(),
                deposit.getCustomerType(), deposit.getCustomerCode(), deposit.getCustomerName(), deposit.getAmount(), deposit.getReleasedAmount(),
                deposit.getRetainedAmount(), deposit.getAppliedAmount(), deposit.remaining(), deposit.getCurrency(), deposit.getStatus(),
                deposit.getCreatedAt(), moves);
    }
}
