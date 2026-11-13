package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.billing.dto.request.DealRequest;
import com.sni.bokaticowork.features.billing.dto.request.DealStageRequest;
import com.sni.bokaticowork.features.billing.dto.response.DealDashboardResponse;
import com.sni.bokaticowork.features.billing.dto.response.DealResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDealStage;
import com.sni.bokaticowork.features.billing.enums.BillingDepositStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDeal;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.BillingDealRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDepositRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Affaires (lot B11) : fiche, rattachement des pieces, tableau des totaux, liste et export.
 *
 * <p>Une piece rejoint l'affaire de la piece dont elle derive (facture issue d'un devis, facture
 * d'acompte, avoir, rectificative). Le premier devis d'un client sans affaire ouverte en cree une ;
 * avec une seule affaire ouverte, il la rejoint.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DealService {

    static final String AUTO_CREATE_KEY = "billing.deal.auto-create";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final Set<BillingDealStage> OPEN = EnumSet.of(BillingDealStage.PROSPECTING, BillingDealStage.PROPOSAL,
            BillingDealStage.NEGOTIATION);
    private static final Set<BillingDocumentStatus> ACCEPTED = EnumSet.of(BillingDocumentStatus.ACCEPTED, BillingDocumentStatus.DEPOSIT_REQUESTED,
            BillingDocumentStatus.DEPOSIT_PAID, BillingDocumentStatus.CONVERTED);
    private static final Set<BillingDocumentType> INVOICES = EnumSet.of(BillingDocumentType.INVOICE, BillingDocumentType.CORRECTIVE_INVOICE,
            BillingDocumentType.DEBIT_NOTE);

    private final BillingDealRepository deals;
    private final BillingDocumentRepository documents;
    private final BillingDocumentLineRepository lines;
    private final BillingDepositRepository deposits;
    private final ServiceCatalogItemRepository catalog;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    // ------------------------------------------------------------------ fiche

    public DealResponse create(DealRequest request) {
        return view(deals.save(fill(BillingDeal.builder().code(sequences.next("billing_deal")).stage(BillingDealStage.PROSPECTING)
                .source("MANUAL").createdBy(staffProfiles.currentAuthor().login()).build(), request)));
    }

    public DealResponse update(String code, DealRequest request) {
        return view(deals.save(fill(deal(code), request)));
    }

    /** Affaire creee depuis une opportunite du CRM : client converti, montant, probabilite, echeance. */
    public DealResponse fromOpportunity(com.sni.bokaticowork.features.crm.model.Opportunity opportunity) {
        var lead = opportunity.getLead();
        String customerName = lead == null ? opportunity.getTitle()
                : StringUtils.hasText(lead.getCompany()) ? lead.getCompany() : lead.getFullName();
        StaffProfileService.Author owner = staffProfiles.currentAuthor();
        return view(deals.save(BillingDeal.builder().code(sequences.next("billing_deal"))
                .customerType(lead == null ? null : lead.getConvertedOwnerType())
                .customerCode(lead == null ? null : lead.getConvertedOwnerCode())
                .customerName(customerName == null ? "Prospect" : customerName).title(opportunity.getTitle())
                .owner(owner.login()).ownerName(owner.name()).expectedAmount(opportunity.getEstimatedAmount())
                .probability(opportunity.getProbability()).expectedCloseDate(opportunity.getExpectedCloseDate())
                .stage(BillingDealStage.PROPOSAL).source("CRM").crmOpportunityRef(opportunity.getOpportunityNumber())
                .createdBy(owner.login()).build()));
    }

    public DealResponse stage(String code, DealStageRequest request) {
        BillingDeal deal = deal(code);
        if (request.stage() == BillingDealStage.LOST && !StringUtils.hasText(request.lostReason())) {
            throw new BadRequestException("Une affaire perdue porte son motif");
        }
        deal.setStage(request.stage());
        deal.setLostReason(request.stage() == BillingDealStage.LOST ? request.lostReason().trim() : null);
        deal.setClosedAt(OPEN.contains(request.stage()) ? null : Instant.now());
        return view(deals.save(deal));
    }

    /** Rattache (ou detache, code vide) une piece a une affaire. */
    public DealResponse attach(String documentNumber, String code) {
        BillingDocument document = documents.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentNumber));
        if (!StringUtils.hasText(code)) {
            document.setDealCode(null);
            documents.save(document);
            return null;
        }
        BillingDeal deal = deal(code);
        if (deal.getCustomerCode() != null && document.getCustomerCode() != null && !deal.getCustomerCode().equals(document.getCustomerCode())) {
            throw new BadRequestException("La pièce et l'affaire n'ont pas le même client");
        }
        document.setDealCode(deal.getCode());
        documents.save(document);
        return view(deal);
    }

    // ------------------------------------------------------------------ rattachement automatique

    /**
     * Piece creee : elle rejoint l'affaire de la piece dont elle derive ; un devis sans affaire rejoint
     * l'unique affaire ouverte du client, ou en ouvre une (parametre billing.deal.auto-create).
     */
    void onDocumentCreated(BillingDocument document) {
        if (StringUtils.hasText(document.getDealCode())) {
            return;
        }
        String parent = StringUtils.hasText(document.getSourceCode()) ? document.getSourceCode() : document.getOriginalDocumentNumber();
        if (StringUtils.hasText(parent)) {
            Optional<String> inherited = documents.findByDocumentNumber(parent).map(BillingDocument::getDealCode).filter(StringUtils::hasText);
            if (inherited.isPresent()) {
                document.setDealCode(inherited.get());
                documents.save(document);
                return;
            }
        }
        if (document.getDocumentType() != BillingDocumentType.QUOTE || !StringUtils.hasText(document.getCustomerCode())
                || (platformSettings != null && !platformSettings.bool(AUTO_CREATE_KEY, true))) {
            return;
        }
        List<BillingDeal> open = deals.findByCustomerTypeAndCustomerCodeAndStageInOrderByCreatedAtDesc(document.getCustomerType(),
                document.getCustomerCode(), OPEN);
        BillingDeal deal;
        if (open.size() == 1) {
            deal = open.get(0);
        } else {
            StaffProfileService.Author owner = staffProfiles.currentAuthor();
            deal = deals.save(BillingDeal.builder().code(sequences.next("billing_deal")).customerType(document.getCustomerType())
                    .customerCode(document.getCustomerCode()).customerName(document.getCustomerName() == null ? document.getCustomerCode()
                            : document.getCustomerName())
                    .title(StringUtils.hasText(document.getTitle()) ? document.getTitle() : "Affaire " + document.getCustomerName())
                    .owner(owner.login()).ownerName(owner.name()).expectedAmount(document.getTotalAmount()).currency(document.getCurrency())
                    .stage(BillingDealStage.PROPOSAL).source("AUTO").createdBy(owner.login()).build());
        }
        document.setDealCode(deal.getCode());
        documents.save(document);
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public Page<DealResponse> list(BillingDealStage stage, String owner, String customerCode, String search, LocalDate from, LocalDate to,
                                   int page, int size) {
        return deals.search(stage == null ? EnumSet.allOf(BillingDealStage.class) : EnumSet.of(stage), blank(owner), blank(customerCode),
                        blank(search), from == null ? Instant.EPOCH : from.atStartOfDay(ZONE).toInstant(),
                        to == null ? Instant.parse("9999-12-31T00:00:00Z") : to.plusDays(1).atStartOfDay(ZONE).toInstant(),
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 500)))
                .map(DealService::view);
    }

    /** Export CSV de la liste filtree (separateur point-virgule). */
    @Transactional(readOnly = true)
    public String exportCsv(BillingDealStage stage, String owner, String customerCode, String search, LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder("Code;Client;Intitulé;Responsable;Étape;Montant prévu;Probabilité;Clôture prévue;Créée le\n");
        for (DealResponse deal : list(stage, owner, customerCode, search, from, to, 0, 500).getContent()) {
            csv.append(String.join(";", cell(deal.code()), cell(deal.customerName()), cell(deal.title()), cell(deal.ownerName()), cell(deal.stage()),
                    cell(deal.expectedAmount() == null ? null : deal.expectedAmount().stripTrailingZeros().toPlainString()),
                    cell(deal.probability() == null ? null : deal.probability().toString()),
                    cell(deal.expectedCloseDate() == null ? null : deal.expectedCloseDate().toString()),
                    cell(LocalDate.ofInstant(deal.createdAt(), ZONE).toString()))).append('\n');
        }
        return csv.toString();
    }

    /** Tableau de l'affaire. */
    @Transactional(readOnly = true)
    public DealDashboardResponse dashboard(String code) {
        BillingDeal deal = deal(code);
        List<BillingDocument> all = documents.findByDealCodeOrderByCreatedAtAsc(deal.getCode());
        BigDecimal quoted = BigDecimal.ZERO, accepted = BigDecimal.ZERO, invoiced = BigDecimal.ZERO, creditNotes = BigDecimal.ZERO;
        BigDecimal depositsInvoiced = BigDecimal.ZERO, depositsPaid = BigDecimal.ZERO, paidOnInvoices = BigDecimal.ZERO;
        BigDecimal remainingToCollect = BigDecimal.ZERO, deducted = BigDecimal.ZERO;
        BigDecimal plannedNet = BigDecimal.ZERO, plannedCost = BigDecimal.ZERO, realizedNet = BigDecimal.ZERO, realizedCost = BigDecimal.ZERO;
        BigDecimal costedNet = BigDecimal.ZERO, allNet = BigDecimal.ZERO;
        Map<String, Optional<BigDecimal>> costs = new HashMap<>();
        for (BillingDocument document : all) {
            BillingDocumentStatus status = document.getStatus();
            boolean live = status != BillingDocumentStatus.DRAFT && status != BillingDocumentStatus.CANCELLED;
            boolean deposit = StringUtils.hasText(document.getDepositForDocumentNumber());
            BigDecimal total = orZero(document.getTotalAmount());
            if (document.getDocumentType() == BillingDocumentType.QUOTE) {
                if (status != BillingDocumentStatus.CANCELLED) {
                    quoted = quoted.add(total);
                }
                if (ACCEPTED.contains(status)) {
                    accepted = accepted.add(total);
                    BigDecimal[] margin = margin(document, costs);
                    plannedNet = plannedNet.add(margin[0]);
                    plannedCost = plannedCost.add(margin[1]);
                }
            } else if (deposit && live) {
                depositsInvoiced = depositsInvoiced.add(total);
                depositsPaid = depositsPaid.add(orZero(document.getPaidAmount()));
            } else if (INVOICES.contains(document.getDocumentType()) && live) {
                invoiced = invoiced.add(total);
                paidOnInvoices = paidOnInvoices.add(orZero(document.getPaidAmount()));
                BigDecimal[] margin = margin(document, costs);
                realizedNet = realizedNet.add(margin[0]);
                realizedCost = realizedCost.add(margin[1]);
                costedNet = costedNet.add(margin[2]);
                allNet = allNet.add(margin[0]);
                deducted = deducted.add(deposits.findByFinalDocumentNumberOrderByRankAsc(document.getDocumentNumber()).stream()
                        .filter(item -> item.getStatus() == BillingDepositStatus.DEDUCTED)
                        .map(item -> orZero(item.getDeductedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
            } else if (document.getDocumentType() == BillingDocumentType.CREDIT_NOTE && live) {
                creditNotes = creditNotes.add(total);
            }
            if (BillingReceivables.receivable(document.getDocumentType(), status)) {
                remainingToCollect = remainingToCollect.add(orZero(document.getBalanceDue()));
            }
        }
        BigDecimal invoicedNet = invoiced.subtract(creditNotes);
        BigDecimal collected = depositsPaid.add(paidOnInvoices).subtract(deducted);
        return new DealDashboardResponse(view(deal), quoted, accepted, invoiced, creditNotes, invoicedNet, depositsInvoiced, depositsPaid, collected,
                accepted.subtract(invoicedNet).max(BigDecimal.ZERO), remainingToCollect, plannedNet.subtract(plannedCost),
                realizedNet.subtract(realizedCost),
                allNet.signum() == 0 ? null : costedNet.multiply(BigDecimal.valueOf(100)).divide(allNet, 1, RoundingMode.HALF_UP),
                all.stream().map(document -> new DealDashboardResponse.Document(document.getDocumentNumber(), document.getDocumentType().name(),
                        document.getStatus().name(), StringUtils.hasText(document.getDepositForDocumentNumber()), document.getIssueDate(),
                        document.getTotalAmount(), document.getPaidAmount(), document.getBalanceDue(), document.getCurrency(),
                        document.getCreatedAt())).toList());
    }

    /** HT de la piece, cout des articles connus, HT des lignes dont le cout est connu. */
    private BigDecimal[] margin(BillingDocument document, Map<String, Optional<BigDecimal>> costs) {
        BigDecimal net = BigDecimal.ZERO, cost = BigDecimal.ZERO, costed = BigDecimal.ZERO;
        for (BillingDocumentLine line : lines.findAllByDocumentOrderByLineOrderAscIdAsc(document)) {
            if (Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            BigDecimal lineNet = orZero(line.getSubtotalAmount()).subtract(orZero(line.getDiscountAmount()));
            net = net.add(lineNet);
            if (StringUtils.hasText(line.getItemCode())) {
                Optional<BigDecimal> unitCost = costs.computeIfAbsent(line.getItemCode(),
                        item -> catalog.findByItemCode(item).map(ServiceCatalogItem::getCostPrice));
                if (unitCost.isPresent()) {
                    cost = cost.add(unitCost.get().multiply(orZero(line.getQuantity())));
                    costed = costed.add(lineNet);
                }
            }
        }
        return new BigDecimal[]{net, cost, costed};
    }

    @Transactional(readOnly = true)
    public DealResponse get(String code) {
        return view(deal(code));
    }

    // ------------------------------------------------------------------ outils

    private BillingDeal fill(BillingDeal deal, DealRequest request) {
        if (StringUtils.hasText(request.customerCode())) {
            deal.setCustomerType(StringUtils.hasText(request.customerType()) ? request.customerType().trim() : "CUSTOMER");
            deal.setCustomerCode(request.customerCode().trim());
        }
        String name = StringUtils.hasText(request.customerName()) ? request.customerName().trim() : deal.getCustomerName();
        if (!StringUtils.hasText(name)) {
            name = deal.getCustomerCode();
        }
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Indiquez le client ou le prospect de l'affaire");
        }
        deal.setCustomerName(name);
        deal.setTitle(request.title().trim());
        StaffProfileService.Author owner = StringUtils.hasText(request.owner()) ? staffProfiles.authorOf(request.owner().trim())
                : deal.getOwner() != null ? staffProfiles.authorOf(deal.getOwner()) : staffProfiles.currentAuthor();
        deal.setOwner(owner.login());
        deal.setOwnerName(owner.name());
        deal.setExpectedAmount(request.expectedAmount());
        deal.setCurrency(StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase() : deal.getCurrency() == null ? "XAF" : deal.getCurrency());
        deal.setProbability(request.probability());
        deal.setExpectedCloseDate(request.expectedCloseDate());
        deal.setAnalyticSection(StringUtils.hasText(request.analyticSection()) ? request.analyticSection().trim() : null);
        deal.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
        return deal;
    }

    private BillingDeal deal(String code) {
        return deals.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Affaire introuvable : " + code));
    }

    static DealResponse view(BillingDeal deal) {
        return new DealResponse(deal.getCode(), deal.getCustomerType(), deal.getCustomerCode(), deal.getCustomerName(), deal.getTitle(),
                deal.getOwner(), deal.getOwnerName(), deal.getExpectedAmount(), deal.getCurrency(), deal.getProbability(), deal.getStage().name(),
                deal.getLostReason(), deal.getExpectedCloseDate(), deal.getClosedAt(), deal.getSource(), deal.getCrmOpportunityRef(),
                deal.getAnalyticSection(), deal.getNotes(), deal.getCreatedBy(), deal.getCreatedAt());
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace("\"", "\"\"");
        return clean.contains(";") || clean.contains("\"") || clean.contains("\n") ? "\"" + clean + "\"" : clean;
    }
}
