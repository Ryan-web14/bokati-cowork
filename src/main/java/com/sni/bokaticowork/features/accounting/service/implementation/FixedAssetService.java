package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.DepreciationCalculator;
import com.sni.bokaticowork.features.accounting.service.support.FixedAssetIntake;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Immobilisations (lot F18) : categories, fiches (pre-remplies depuis les factures fournisseurs,
 * ou creees a la main), composants, immobilisations en cours, mise en service et PV, plans
 * d'amortissement versionnes (lineaire, degressif, unites d'oeuvre ; comptable et fiscal),
 * revisions, transferts, affectations, entretien, depreciations, reclassements, regularisation
 * de la TVA deduite, sorties avec PV et cession. Chaque operation sur un bien produit sa piece.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FixedAssetService implements FixedAssetIntake {

    public static final String READ = "FIXED_ASSET:READ";
    public static final String MANAGE = "FIXED_ASSET:MANAGE";
    public static final String REVISE = "FIXED_ASSET:REVISE";
    public static final String IMPAIR = "FIXED_ASSET:IMPAIR";
    public static final String DISPOSE = "FIXED_ASSET:DISPOSE";
    public static final String SETTINGS = "FIXED_ASSET_SETTINGS:MANAGE";

    public static final String ACCOUNTING = "ACCOUNTING";
    public static final String TAX = "TAX";
    /** Fiches presentes au fichier (la sortie et l'annulation les en retirent). */
    public static final List<String> LIVE = List.of("DRAFT", "IN_PROGRESS", "IN_SERVICE", "FULLY_DEPRECIATED");
    /** Dotations deja comptabilisees. */
    public static final Set<String> DONE = Set.of("POSTED", "CAUGHT_UP");
    /** Dotations a comptabiliser. */
    public static final Set<String> OPEN = Set.of("PLANNED", "SKIPPED_LOCKED");
    public static final List<String> REVISION_REASONS = List.of("DURATION_CHANGE", "RESIDUAL_CHANGE", "METHOD_CHANGE", "SUBSEQUENT_EXPENSE",
            "IMPAIRMENT", "CREDIT_NOTE", "ERROR_CORRECTION", "OTHER");
    public static final List<String> DISPOSAL_REASONS = List.of("SALE", "SCRAP", "THEFT", "LOSS", "DISASTER", "DONATION", "EXCHANGE",
            "COMPONENT_REPLACED", "SUPPLIER_RETURN");

    private final FaCategoryRepository categories;
    private final com.sni.bokaticowork.features.accounting.repository.FaDepreciationProfileRepository profiles;
    private final FixedAssetRepository assets;
    private final FixedAssetValueLineRepository valueLines;
    private final FixedAssetInventoryLinkRepository links;
    private final FixedAssetPlanRepository plans;
    private final FixedAssetScheduleRepository schedules;
    private final FixedAssetMeterReadingRepository readings;
    private final FixedAssetEventRepository events;
    private final FixedAssetDisposalRepository disposals;
    private final FixedAssetImpairmentRepository impairments;
    private final FixedAssetMaintenanceRepository maintenances;
    private final AccAccountRepository accounts;
    private final AccEntryRepository entryRepository;
    private final AccPeriodRepository periodRepository;
    private final AccAnalyticSectionRepository sections;
    private final AccountingEntryService entries;
    private final AccountingPeriodService periods;
    private final AccountingLetteringService lettering;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;
    private final VoucherService vouchers;
    private final CashActorResolver actors;
    private final DataSource dataSource;
    private final EntityManager entityManager;
    private final ObjectProvider<AssetRepository> inventoryAssets;
    private final ObjectProvider<ResourceRepository> resources;
    private final ObjectProvider<ResourceAmenityLinkRepository> amenityLinks;

    // ================================================================== categories

    public record CategoryRequest(String code, String label, String assetAccount, String depreciationAccount, String expenseAccount,
                                  String impairmentAccount, String impairmentExpenseAccount, String impairmentReversalAccount,
                                  String disposalValueAccount, String disposalIncomeAccount, String method, Integer durationMonths,
                                  BigDecimal coefficient, BigDecimal threshold, String tracking, Boolean trackOffBooks, String taxMethod,
                                  Integer taxDurationMonths, String componentTypes, Boolean vatRecoverable, Boolean inProgress, Boolean active,
                                  BigDecimal annualRate) {
    }

    @Transactional(readOnly = true)
    public List<FaCategory> categories() {
        return categories.findAllByOrderByCodeAsc();
    }

    /** Cree ou modifie une categorie ; les comptes d'une categorie qui a des fiches au fichier ne changent plus (reclasser les fiches). */
    public FaCategory saveCategory(CategoryRequest request) {
        require(SETTINGS);
        if (request == null || !StringUtils.hasText(request.code()) || !StringUtils.hasText(request.label())) {
            throw new BadRequestException("Code et libelle de la categorie obligatoires");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        FaCategory category = categories.findById(code).orElseGet(() -> FaCategory.builder().code(code).build());
        boolean used = assets.findAll().stream().anyMatch(asset -> code.equals(asset.getCategoryCode()) && LIVE.contains(asset.getAccountingStatus())
                && Boolean.TRUE.equals(asset.getBooked()));
        String method = upper(request.method(), "LINEAR");
        if (!List.of("LINEAR", "DECLINING", "UNITS", "NONE").contains(method)) {
            throw new BadRequestException("Mode d'amortissement inconnu : " + method + " (LINEAR, DECLINING, UNITS ou NONE)");
        }
        String assetAccount = account(request.assetAccount(), "2", "Compte du bien");
        String depreciation = "NONE".equals(method) ? optionalAccount(request.depreciationAccount(), "28", "Compte d'amortissement")
                : account(request.depreciationAccount(), "28", "Compte d'amortissement");
        String expense = "NONE".equals(method) ? optionalAccount(request.expenseAccount(), "68", "Compte de dotation")
                : account(request.expenseAccount(), "68", "Compte de dotation");
        if (used && (!Objects.equals(category.getAssetAccount(), assetAccount) || !Objects.equals(category.getDepreciationAccount(), depreciation)
                || !Objects.equals(category.getImpairmentAccount(), blankToNull(request.impairmentAccount())))) {
            throw new BadRequestException("La categorie " + code + " a des fiches au fichier : ses comptes ne changent plus ; reclassez les fiches vers une autre categorie");
        }
        BigDecimal annualRate = request.annualRate();
        if (annualRate != null && (annualRate.signum() <= 0 || annualRate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("Le taux annuel est un pourcentage entre 0 et 100");
        }
        Integer duration = request.durationMonths() != null ? request.durationMonths()
                : annualRate == null ? null : DepreciationCalculator.monthsForRate(annualRate);
        if (!"NONE".equals(method) && (duration == null || duration <= 0)) {
            throw new BadRequestException("Duree d'amortissement obligatoire (en mois)");
        }
        if ("DECLINING".equals(method) && duration < 36) {
            throw new BadRequestException("Le degressif s'applique aux biens d'au moins 3 ans");
        }
        category.setLabel(request.label().trim());
        category.setAssetAccount(assetAccount);
        category.setDepreciationAccount(depreciation);
        category.setExpenseAccount(expense);
        category.setImpairmentAccount(optionalAccount(request.impairmentAccount(), "29", "Compte de depreciation"));
        category.setImpairmentExpenseAccount(optionalAccount(request.impairmentExpenseAccount(), "69", "Compte de dotation aux depreciations"));
        category.setImpairmentReversalAccount(optionalAccount(request.impairmentReversalAccount(), "79", "Compte de reprise de depreciation"));
        category.setDisposalValueAccount(StringUtils.hasText(request.disposalValueAccount()) ? account(request.disposalValueAccount(), "81", "Compte de valeur des cessions")
                : assetAccount.startsWith("21") ? "811" : "812");
        category.setDisposalIncomeAccount(StringUtils.hasText(request.disposalIncomeAccount()) ? account(request.disposalIncomeAccount(), "82", "Compte de produit des cessions")
                : assetAccount.startsWith("21") ? "821" : "822");
        category.setMethod(method);
        category.setDurationMonths("NONE".equals(method) ? null : duration);
        category.setCoefficient(request.coefficient());
        category.setAnnualRate("NONE".equals(method) ? null : annualRate);
        category.setThreshold(request.threshold() == null ? new BigDecimal("100000") : request.threshold());
        category.setTracking("LOT".equalsIgnoreCase(request.tracking()) ? "LOT" : "UNIT");
        category.setTrackOffBooks(Boolean.TRUE.equals(request.trackOffBooks()));
        String taxMethod = blankToNull(request.taxMethod()) == null ? null : request.taxMethod().trim().toUpperCase(Locale.ROOT);
        if (taxMethod != null && !List.of("LINEAR", "DECLINING").contains(taxMethod)) {
            throw new BadRequestException("Mode fiscal inconnu : " + taxMethod);
        }
        category.setTaxMethod(taxMethod);
        category.setTaxDurationMonths(taxMethod == null ? null : request.taxDurationMonths() == null ? duration : request.taxDurationMonths());
        category.setComponentTypes(blankToNull(request.componentTypes()));
        category.setVatRecoverable(request.vatRecoverable() == null || request.vatRecoverable());
        category.setInProgress(Boolean.TRUE.equals(request.inProgress()));
        category.setActive(request.active() == null || request.active());
        return categories.save(category);
    }

    // ================================================================== fiches

    public record AssetRequest(String categoryCode, String designation, String brand, String model, String serialNumber, Integer quantity,
                               String entryMode, String supplierCode, String supplierName, String supplierInvoiceNumber, String orderNumber,
                               String fundRequestRef, String justificationRef, LocalDate acquisitionDate, LocalDate receivedDate,
                               BigDecimal entryValue, BigDecimal residualValue, BigDecimal deductedVat, Boolean trackedOffBooks,
                               String counterAccount, String entryNumber, String organisation, String site, String resourceCode,
                               Long amenityLinkId, String locationLabel, Map<String, String> analytic, LocalDate warrantyEnd,
                               String maintenanceContract, LocalDate maintenanceEnd, String insurer, String policyNumber, BigDecimal insuredValue,
                               String documentRefs, String photoRef, String notes, String method, Integer durationMonths, BigDecimal coefficient,
                               BigDecimal totalUnits, String taxMethod, Integer taxDurationMonths, String targetCategoryCode,
                               List<String> inventoryAssetCodes) {
    }

    public record AssetView(FixedAsset asset, FaCategory category, BigDecimal netValue, Map<String, String> analytic, List<FixedAssetValueLine> values,
                            List<FixedAsset> components, List<String> suggestedComponents, List<FixedAssetSchedule> schedule,
                            List<FixedAssetEvent> events, List<FixedAssetDisposal> disposals, List<FixedAssetImpairment> impairments,
                            List<FixedAssetMaintenance> maintenance, List<String> inventoryAssets, List<String> warnings) {
    }

    @Transactional(readOnly = true)
    public AssetView view(String code) {
        return view(asset(code), List.of());
    }

    AssetView view(FixedAsset asset, List<String> warnings) {
        FaCategory category = categories.findById(asset.getCategoryCode()).orElse(null);
        List<String> suggested = category == null || category.getComponentTypes() == null ? List.of()
                : Arrays.stream(category.getComponentTypes().split(",")).map(String::trim).filter(StringUtils::hasText).toList();
        return new AssetView(asset, category, netValue(asset), analytic(asset), valueLines.findAllByAssetIdOrderByValueDateAscIdAsc(asset.getId()),
                assets.findAllByParentIdOrderByCodeAsc(asset.getId()), suggested, current(asset, ACCOUNTING),
                events.findAllByAssetIdOrderByCreatedAtAscIdAsc(asset.getId()), disposals.findAllByAssetIdOrderByCreatedAtAsc(asset.getId()),
                impairments.findAllByAssetIdOrderByCreatedAtAsc(asset.getId()), maintenances.findAllByAssetIdOrderByMaintenanceDateAsc(asset.getId()),
                links.findAllByFixedAssetId(asset.getId()).stream().map(FixedAssetInventoryLink::getInventoryAssetCode).toList(), warnings);
    }

    /**
     * Fiche creee a la main (rare : la plupart viennent d'une facture). Avec un compte de
     * contrepartie, l'entree est ecrite (production immobilisee, apport, location-acquisition,
     * don) ; avec un numero de piece, elle est deja en comptabilite ; sinon la fiche attend.
     */
    public AssetView create(AssetRequest request) {
        require(MANAGE);
        if (request == null || !StringUtils.hasText(request.designation())) {
            throw new BadRequestException("Designation du bien obligatoire");
        }
        FaCategory category = category(request.categoryCode());
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BadRequestException("La categorie " + category.getCode() + " est inactive");
        }
        boolean offBooks = Boolean.TRUE.equals(request.trackedOffBooks());
        if (offBooks && !Boolean.TRUE.equals(category.getTrackOffBooks())) {
            throw new BadRequestException("La categorie " + category.getCode() + " ne suit pas de petits biens hors comptabilite");
        }
        BigDecimal value = money(request.entryValue());
        if (value.signum() < 0) {
            throw new BadRequestException("La valeur d'entree est positive");
        }
        LocalDate acquisition = request.acquisitionDate() == null ? LocalDate.now() : request.acquisitionDate();
        String entryMode = upper(request.entryMode(), Boolean.TRUE.equals(category.getInProgress()) ? "IN_PROGRESS" : "PURCHASE");
        FixedAsset asset = FixedAsset.builder().code(nextCode(acquisition.getYear())).categoryCode(category.getCode()).designation(request.designation().trim())
                .quantity(request.quantity() == null || request.quantity() <= 0 ? 1 : request.quantity()).entryMode(entryMode)
                .trackedOffBooks(offBooks).acquisitionDate(acquisition).receivedDate(request.receivedDate() == null ? acquisition : request.receivedDate())
                .accountingStatus(Boolean.TRUE.equals(category.getInProgress()) ? "IN_PROGRESS" : "DRAFT").createdBy(actor()).build();
        apply(asset, request, true);
        asset = assets.save(asset);
        if (value.signum() > 0) {
            valueLine(asset, "CONTRIBUTION".equals(entryMode) || "REPLAY".equals(entryMode) ? "OPENING" : "PURCHASE", value, acquisition, "Saisie", null);
        }
        if (!offBooks && StringUtils.hasText(request.counterAccount())) {
            if (value.signum() <= 0) {
                throw new BadRequestException("Une entree ecrite a une valeur positive");
            }
            String counter = account(request.counterAccount(), null, "Compte de contrepartie");
            String label = "Entree immobilisation " + asset.getCode() + " " + asset.getDesignation();
            AccEntry entry = book(journal(), acquisition, label, "FIXED_ASSET_ENTRY", asset.getCode(), List.of(
                    line(category.getAssetAccount(), null, label, value, null, asset.getCode(), null),
                    line(counter, thirdPartyFor(counter, request.supplierCode()), label, null, value, asset.getCode(),
                            counter.startsWith("6") || counter.startsWith("7") ? analytic(asset) : null)));
            asset.setEntryNumber(entry.getEntryNumber());
            asset.setBooked(true);
            if (counter.startsWith("14")) {
                // Don ou subvention d'equipement : reprise au resultat au rythme de l'amortissement.
                asset.setSubsidyAccount(counter);
                asset.setSubsidyAmount(value);
            }
        } else if (!offBooks && StringUtils.hasText(request.entryNumber())) {
            AccEntry entry = entryRepository.findByEntryNumber(request.entryNumber().trim())
                    .orElseThrow(() -> new BadRequestException("Piece inconnue : " + request.entryNumber()));
            asset.setEntryNumber(entry.getEntryNumber());
            asset.setBooked(true);
        }
        asset.setEntryValue(value);
        asset = assets.save(asset);
        linkInventory(asset, request.inventoryAssetCodes());
        event(asset, "CREATED", acquisition, null, category.getCode(), "Fiche creee a la main (" + entryMode + ")", value, null, asset.getEntryNumber());
        return view(asset, List.of());
    }

    /** Complete une fiche : descriptif, lieu, garanties ; les parametres du plan avant la premiere dotation. */
    public AssetView update(String code, AssetRequest request) {
        require(MANAGE);
        FixedAsset asset = live(code);
        List<String> warnings = new ArrayList<>();
        if (StringUtils.hasText(request.categoryCode()) && !request.categoryCode().trim().equalsIgnoreCase(asset.getCategoryCode())) {
            if (!"DRAFT".equals(asset.getAccountingStatus())) {
                throw new BadRequestException("Apres la mise en service, le changement de categorie passe par un reclassement");
            }
            FaCategory target = category(request.categoryCode());
            reclassAccounts(asset, category(asset.getCategoryCode()), target, LocalDate.now(), "Categorie completee");
            asset.setCategoryCode(target.getCode());
        }
        boolean planChange = request.method() != null || request.durationMonths() != null || request.residualValue() != null
                || request.coefficient() != null || request.totalUnits() != null || request.taxMethod() != null || request.taxDurationMonths() != null;
        if (planChange && hasPosted(asset)) {
            throw new BadRequestException("Le plan a deja des dotations comptabilisees : passez par une revision du plan");
        }
        if (request.entryValue() != null && !money(request.entryValue()).equals(money(asset.getEntryValue()))) {
            if (Boolean.TRUE.equals(asset.getBooked())) {
                throw new BadRequestException("La valeur d'une fiche comptabilisee vient de ses pieces (facture, frais, avoir)");
            }
            BigDecimal delta = money(request.entryValue()).subtract(asset.getEntryValue());
            valueLine(asset, "PURCHASE", delta, LocalDate.now(), "Correction de la fiche", null);
            asset.setEntryValue(money(request.entryValue()));
        }
        apply(asset, request, false);
        asset.setUpdatedAt(Instant.now());
        assets.save(asset);
        linkInventory(asset, request.inventoryAssetCodes());
        if (planChange && asset.getPlanVersion() > 0) {
            regenerate(asset, "Fiche completee avant la premiere dotation");
            warnings.add("Plan d'amortissement recalcule");
        }
        return view(asset, warnings);
    }

    private void apply(FixedAsset asset, AssetRequest request, boolean creating) {
        if (StringUtils.hasText(request.designation())) asset.setDesignation(request.designation().trim());
        if (request.brand() != null) asset.setBrand(blankToNull(request.brand()));
        if (request.model() != null) asset.setModel(blankToNull(request.model()));
        if (request.serialNumber() != null) asset.setSerialNumber(blankToNull(request.serialNumber()));
        if (!creating && request.quantity() != null && request.quantity() > 0) asset.setQuantity(request.quantity());
        if (request.supplierCode() != null) asset.setSupplierCode(blankToNull(request.supplierCode()));
        if (request.supplierName() != null) asset.setSupplierName(blankToNull(request.supplierName()));
        if (request.supplierInvoiceNumber() != null) asset.setSupplierInvoiceNumber(blankToNull(request.supplierInvoiceNumber()));
        if (request.orderNumber() != null) asset.setOrderNumber(blankToNull(request.orderNumber()));
        if (request.fundRequestRef() != null) asset.setFundRequestRef(blankToNull(request.fundRequestRef()));
        if (request.justificationRef() != null) asset.setJustificationRef(blankToNull(request.justificationRef()));
        if (!creating && request.acquisitionDate() != null) asset.setAcquisitionDate(request.acquisitionDate());
        if (!creating && request.receivedDate() != null) asset.setReceivedDate(request.receivedDate());
        if (request.residualValue() != null) {
            if (request.residualValue().signum() < 0) {
                throw new BadRequestException("La valeur residuelle est positive ou nulle");
            }
            asset.setResidualValue(money(request.residualValue()));
        }
        if (request.deductedVat() != null) asset.setDeductedVat(money(request.deductedVat()));
        if (request.organisation() != null) asset.setOrganisation(blankToNull(request.organisation()));
        if ("DRAFT".equals(asset.getAccountingStatus()) || "IN_PROGRESS".equals(asset.getAccountingStatus()) || creating) {
            // Apres la mise en service, le lieu change par un transfert (historique).
            if (request.site() != null) asset.setSite(blankToNull(request.site()));
            if (request.resourceCode() != null) asset.setResourceCode(resourceCode(request.resourceCode()));
            if (request.locationLabel() != null) asset.setLocationLabel(blankToNull(request.locationLabel()));
            if (request.analytic() != null) asset.setAnalyticSections(encode(request.analytic()));
        }
        if (request.amenityLinkId() != null) asset.setAmenityLinkId(request.amenityLinkId());
        if (asset.getLocationLabel() == null && asset.getResourceCode() != null) {
            resources().flatMap(repository -> repository.findByCode(asset.getResourceCode())).ifPresent(resource -> asset.setLocationLabel(resource.getLocationLabel()));
        }
        if (request.warrantyEnd() != null) asset.setWarrantyEnd(request.warrantyEnd());
        if (request.maintenanceContract() != null) asset.setMaintenanceContract(blankToNull(request.maintenanceContract()));
        if (request.maintenanceEnd() != null) asset.setMaintenanceEnd(request.maintenanceEnd());
        if (request.insurer() != null) asset.setInsurer(blankToNull(request.insurer()));
        if (request.policyNumber() != null) asset.setPolicyNumber(blankToNull(request.policyNumber()));
        if (request.insuredValue() != null) asset.setInsuredValue(money(request.insuredValue()));
        if (request.documentRefs() != null) asset.setDocumentRefs(blankToNull(request.documentRefs()));
        if (request.photoRef() != null) asset.setPhotoRef(blankToNull(request.photoRef()));
        if (request.notes() != null) asset.setNotes(blankToNull(request.notes()));
        if (request.method() != null) {
            String method = request.method().trim().toUpperCase(Locale.ROOT);
            if (!List.of("LINEAR", "DECLINING", "UNITS", "NONE").contains(method)) {
                throw new BadRequestException("Mode d'amortissement inconnu : " + method);
            }
            asset.setMethod(method);
        }
        if (request.durationMonths() != null) {
            if (request.durationMonths() <= 0) {
                throw new BadRequestException("La duree d'amortissement est positive (en mois)");
            }
            asset.setDurationMonths(request.durationMonths());
        }
        if (request.coefficient() != null) asset.setCoefficient(request.coefficient());
        if (request.totalUnits() != null) asset.setTotalUnits(request.totalUnits());
        if (request.taxMethod() != null) asset.setTaxMethod(blankToNull(request.taxMethod()) == null ? null : request.taxMethod().trim().toUpperCase(Locale.ROOT));
        if (request.taxDurationMonths() != null) asset.setTaxDurationMonths(request.taxDurationMonths());
        if (request.targetCategoryCode() != null) asset.setTargetCategoryCode(blankToNull(request.targetCategoryCode()) == null ? null : category(request.targetCategoryCode()).getCode());
        if ("DECLINING".equals(method(asset)) && duration(asset) < 36) {
            throw new BadRequestException("Le degressif s'applique aux biens d'au moins 3 ans");
        }
    }

    // ================================================================== composants

    public record ComponentInput(String designation, BigDecimal value, Integer durationMonths, String categoryCode, String method) {
    }

    /** Decoupe une fiche (avant sa mise en service) en composants, chacun avec sa duree et son plan. */
    public AssetView components(String code, List<ComponentInput> inputs) {
        require(MANAGE);
        FixedAsset parent = live(code);
        if (!"DRAFT".equals(parent.getAccountingStatus()) && !"IN_PROGRESS".equals(parent.getAccountingStatus())) {
            throw new BadRequestException("Seule une fiche non encore mise en service se decoupe en composants ; ensuite, une depense ulterieure devient un composant");
        }
        if (parent.getParentId() != null) {
            throw new BadRequestException("Un composant ne se decoupe pas a son tour");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException("Indiquez au moins un composant");
        }
        BigDecimal total = inputs.stream().map(input -> money(input.value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (inputs.stream().anyMatch(input -> money(input.value()).signum() <= 0 || !StringUtils.hasText(input.designation()))) {
            throw new BadRequestException("Chaque composant a une designation et une valeur positive");
        }
        if (total.compareTo(parent.getEntryValue()) > 0) {
            throw new BadRequestException("Les composants (" + plain(total) + ") depassent la valeur de la fiche (" + plain(parent.getEntryValue()) + ")");
        }
        FaCategory parentCategory = category(parent.getCategoryCode());
        boolean inProgress = "IN_PROGRESS".equals(parent.getAccountingStatus());
        LocalDate today = LocalDate.now();
        for (ComponentInput input : inputs) {
            FaCategory target = StringUtils.hasText(input.categoryCode()) ? category(input.categoryCode()) : parentCategory;
            FixedAsset child = FixedAsset.builder().code(nextCode(today.getYear())).parentId(parent.getId())
                    .categoryCode(inProgress ? parentCategory.getCode() : target.getCode())
                    .targetCategoryCode(inProgress ? (StringUtils.hasText(input.categoryCode()) ? target.getCode() : parent.getTargetCategoryCode()) : null)
                    .designation(input.designation().trim()).entryMode(parent.getEntryMode()).supplierCode(parent.getSupplierCode())
                    .supplierName(parent.getSupplierName()).supplierInvoiceNumber(parent.getSupplierInvoiceNumber()).orderNumber(parent.getOrderNumber())
                    .entryNumber(parent.getEntryNumber()).booked(parent.getBooked()).acquisitionDate(parent.getAcquisitionDate())
                    .receivedDate(parent.getReceivedDate()).accountingStatus(parent.getAccountingStatus()).site(parent.getSite())
                    .resourceCode(parent.getResourceCode()).locationLabel(parent.getLocationLabel()).analyticSections(parent.getAnalyticSections())
                    .organisation(parent.getOrganisation()).durationMonths(input.durationMonths()).method(blankToNull(input.method()) == null ? null
                            : input.method().trim().toUpperCase(Locale.ROOT)).createdBy(actor()).build();
            BigDecimal share = money(input.value());
            if (parent.getDeductedVat().signum() > 0 && parent.getEntryValue().signum() > 0) {
                BigDecimal vat = parent.getDeductedVat().multiply(share).divide(parent.getEntryValue(), 0, RoundingMode.HALF_UP);
                child.setDeductedVat(vat);
                parent.setDeductedVat(parent.getDeductedVat().subtract(vat));
            }
            child.setEntryValue(share);
            child = assets.save(child);
            valueLine(child, "COMPONENT_SPLIT", share, today, "Composant de " + parent.getCode(), null);
            valueLine(parent, "COMPONENT_SPLIT", share.negate(), today, "Composant " + child.getCode(), null);
            parent.setEntryValue(parent.getEntryValue().subtract(share));
            if (!inProgress && Boolean.TRUE.equals(parent.getBooked()) && !target.getAssetAccount().equals(parentCategory.getAssetAccount())) {
                String label = "Reclassement du composant " + child.getCode() + " " + child.getDesignation();
                AccEntry entry = book(journal(), today, label, "FIXED_ASSET_RECLASS", child.getCode(), List.of(
                        line(target.getAssetAccount(), null, label, share, null, child.getCode(), null),
                        line(parentCategory.getAssetAccount(), null, label, null, share, parent.getCode(), null)));
                event(child, "RECLASS", today, parentCategory.getCode(), target.getCode(), label, share, null, entry.getEntryNumber());
            }
            event(child, "CREATED", today, null, child.getCategoryCode(), "Composant de " + parent.getCode(), share, null, null);
        }
        assets.save(parent);
        event(parent, "COMPONENTS", today, null, String.valueOf(inputs.size()), "Decoupage en composants", total, null, null);
        return view(parent, List.of());
    }

    // ================================================================== mise en service

    public record CommissionRequest(LocalDate date, String site, String resourceCode, String locationLabel, String holder, String organisation,
                                    String targetCategoryCode, Map<String, String> analytic, String notes) {
    }

    /**
     * Mise en service (PV {@code PMS}) : la fiche et ses composants passent en service et leur plan
     * est calcule ; une immobilisation en cours est transferee vers ses comptes definitifs.
     */
    public AssetView commission(String code, CommissionRequest request) {
        require(MANAGE);
        FixedAsset asset = live(code);
        CommissionRequest input = request == null ? new CommissionRequest(null, null, null, null, null, null, null, null, null) : request;
        if (!"DRAFT".equals(asset.getAccountingStatus()) && !"IN_PROGRESS".equals(asset.getAccountingStatus())) {
            throw new BadRequestException("La fiche " + code + " est deja en service (" + asset.getAccountingStatus() + ")");
        }
        if (asset.getParentId() != null) {
            throw new BadRequestException("Un composant se met en service avec sa fiche parente");
        }
        if (!Boolean.TRUE.equals(asset.getBooked()) && !Boolean.TRUE.equals(asset.getTrackedOffBooks())) {
            throw new BadRequestException("La fiche " + code + " n'est pas encore en comptabilite : rattachez sa facture ou sa piece d'entree");
        }
        LocalDate date = input.date() != null ? input.date() : asset.getReceivedDate() != null ? asset.getReceivedDate() : LocalDate.now();
        // Valeur de revente propre a la marque (profil) quand la fiche n'en porte pas.
        if (asset.getResidualValue().signum() == 0) {
            profile(asset).map(FaDepreciationProfile::getResidualRate).filter(rate -> rate.signum() > 0)
                    .ifPresent(rate -> asset.setResidualValue(asset.getEntryValue().multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)));
        }
        if (asset.getAcquisitionDate() != null && date.isBefore(asset.getAcquisitionDate())) {
            throw new BadRequestException("La mise en service suit l'acquisition (" + asset.getAcquisitionDate() + ")");
        }
        List<FixedAsset> nodes = new ArrayList<>();
        nodes.add(asset);
        nodes.addAll(assets.findAllByParentIdOrderByCodeAsc(asset.getId()).stream().filter(child -> LIVE.contains(child.getAccountingStatus())).toList());
        if ("IN_PROGRESS".equals(asset.getAccountingStatus())) {
            transferInProgress(asset, nodes, input.targetCategoryCode(), date);
        }
        String issuedNumber = vouchers.issueAndPrint("PMS", "FIXED_ASSET_COMMISSION", asset.getCode(), asset.getEntryValue(),
                "Mise en service " + asset.getCode() + " " + asset.getDesignation(), actor()).number();
        for (FixedAsset node : nodes) {
            node.setInServiceDate(date);
            if (input.site() != null) node.setSite(blankToNull(input.site()));
            if (input.resourceCode() != null) node.setResourceCode(resourceCode(input.resourceCode()));
            if (input.locationLabel() != null) node.setLocationLabel(blankToNull(input.locationLabel()));
            if (input.organisation() != null) node.setOrganisation(blankToNull(input.organisation()));
            if (input.analytic() != null) node.setAnalyticSections(encode(input.analytic()));
            if (node.getLocationLabel() == null && node.getResourceCode() != null) {
                resources().flatMap(repository -> repository.findByCode(node.getResourceCode())).ifPresent(resource -> node.setLocationLabel(resource.getLocationLabel()));
            }
            if (StringUtils.hasText(input.holder())) {
                node.setHolder(input.holder().trim());
                node.setHolderSince(date);
            }
            node.setAccountingStatus("IN_SERVICE");
            node.setOperationalStatus("IN_USE");
            node.setOperationalSince(date);
            node.setUpdatedAt(Instant.now());
            assets.save(node);
            if (!Boolean.TRUE.equals(node.getTrackedOffBooks()) && !"NONE".equals(method(node)) && node.getEntryValue().signum() > 0) {
                regenerate(node, "Mise en service");
            }
            event(node, "COMMISSIONED", date, null, location(node), input.notes(), node.getEntryValue(), issuedNumber, null);
        }
        return view(asset, List.of());
    }

    /** Mise en service d'une immobilisation en cours : D comptes definitifs par composant ; C compte des en-cours. */
    private void transferInProgress(FixedAsset asset, List<FixedAsset> nodes, String requestedTarget, LocalDate date) {
        FaCategory source = category(asset.getCategoryCode());
        String label = "Mise en service de l'en-cours " + asset.getCode() + " " + asset.getDesignation();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FixedAsset node : nodes) {
            String targetCode = node != asset && node.getTargetCategoryCode() != null ? node.getTargetCategoryCode()
                    : StringUtils.hasText(requestedTarget) ? requestedTarget : node.getTargetCategoryCode() != null ? node.getTargetCategoryCode()
                    : asset.getTargetCategoryCode();
            if (node.getEntryValue().signum() == 0 && node == asset) {
                // Fiche parente entierement decoupee : elle prend la categorie demandee sans ecriture.
                if (StringUtils.hasText(targetCode)) node.setCategoryCode(category(targetCode).getCode());
                continue;
            }
            if (!StringUtils.hasText(targetCode)) {
                throw new BadRequestException("Indiquez la categorie definitive de " + node.getCode() + " (en cours)");
            }
            FaCategory target = category(targetCode);
            if (Boolean.TRUE.equals(target.getInProgress())) {
                throw new BadRequestException("La categorie definitive n'est pas une categorie d'en-cours");
            }
            lines.add(line(target.getAssetAccount(), null, label + " (" + node.getDesignation() + ")", node.getEntryValue(), null, node.getCode(), null));
            total = total.add(node.getEntryValue());
            node.setCategoryCode(target.getCode());
            node.setTargetCategoryCode(null);
        }
        if (total.signum() > 0) {
            lines.add(line(source.getAssetAccount(), null, label, null, total, asset.getCode(), null));
            AccEntry entry = book(journal(), date, label, "FIXED_ASSET_COMMISSION", asset.getCode(), lines);
            event(asset, "IN_PROGRESS_TRANSFER", date, source.getCode(), asset.getCategoryCode(), label, total, null, entry.getEntryNumber());
        }
    }

    // ================================================================== plans

    /** Plan d'amortissement en cours et ses versions anterieures. */
    public record PlanView(String code, String kind, List<FixedAssetPlan> versions, List<FixedAssetSchedule> current, List<FixedAssetSchedule> history,
                           Map<Integer, BigDecimal> byYear) {
    }

    @Transactional(readOnly = true)
    public PlanView plan(String code, String kind) {
        FixedAsset asset = asset(code);
        String planKind = TAX.equalsIgnoreCase(kind) ? TAX : ACCOUNTING;
        List<FixedAssetSchedule> current = current(asset, planKind);
        Map<Integer, BigDecimal> byYear = new TreeMap<>();
        current.stream().filter(row -> !"CANCELLED".equals(row.getStatus()))
                .forEach(row -> byYear.merge(Integer.parseInt(row.getPeriodCode().substring(0, 4)), amount(row), BigDecimal::add));
        return new PlanView(asset.getCode(), planKind, plans.findAllByAssetIdOrderByVersionAscKindAsc(asset.getId()).stream()
                .filter(plan -> planKind.equals(plan.getKind())).toList(), current,
                schedules.findAllByAssetIdOrderByKindAscVersionAscPeriodCodeAsc(asset.getId()).stream().filter(row -> planKind.equals(row.getKind())).toList(),
                byYear);
    }

    public record RevisionRequest(String reason, String comment, Integer durationMonths, BigDecimal residualValue, String method, BigDecimal coefficient,
                                  BigDecimal totalUnits, String taxMethod, Integer taxDurationMonths) {
    }

    /** Revision du plan, pour l'avenir, a partir de la valeur nette ; les dotations passees ne changent jamais. */
    public AssetView revise(String code, RevisionRequest request) {
        require(REVISE);
        FixedAsset asset = live(code);
        if (!"IN_SERVICE".equals(asset.getAccountingStatus()) && !"FULLY_DEPRECIATED".equals(asset.getAccountingStatus())) {
            throw new BadRequestException("Seul le plan d'un bien en service se revise");
        }
        String reason = request == null ? null : upper(request.reason(), null);
        if (reason == null || !REVISION_REASONS.contains(reason)) {
            throw new BadRequestException("Motif de revision obligatoire : " + String.join(", ", REVISION_REASONS));
        }
        String before = describePlan(asset);
        apply(asset, new AssetRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                request.residualValue(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, request.method(), request.durationMonths(), request.coefficient(), request.totalUnits(), request.taxMethod(),
                request.taxDurationMonths(), null, null), false);
        assets.save(asset);
        regenerate(asset, reason + (StringUtils.hasText(request.comment()) ? " : " + request.comment().trim() : ""));
        event(asset, "REVISION", LocalDate.now(), before, describePlan(asset), reason + (request.comment() == null ? "" : " " + request.comment()), null, null, null);
        return view(asset, List.of());
    }

    /**
     * Nouvelle version des plans (comptable et, s'il y a lieu, fiscal) : les dotations deja
     * comptabilisees sont recopiees, la suite est recalculee a partir de la valeur nette.
     */
    void regenerate(FixedAsset asset, String reason) {
        int version = asset.getPlanVersion() + 1;
        FaCategory category = category(asset.getCategoryCode());
        List<String> kinds = new ArrayList<>();
        if (!"NONE".equals(method(asset))) {
            kinds.add(ACCOUNTING);
            if (taxMethod(asset, category) != null) {
                kinds.add(TAX);
            }
        }
        boolean days360 = days360();
        LocalDate inService = asset.getInServiceDate() == null ? LocalDate.now() : asset.getInServiceDate();
        for (String kind : kinds) {
            List<FixedAssetSchedule> previous = asset.getPlanVersion() == 0 ? List.of() : current(asset, kind);
            List<FixedAssetSchedule> done = previous.stream().filter(row -> DONE.contains(row.getStatus())).toList();
            BigDecimal doneSum = done.stream().map(FixedAssetSchedule::getPosted).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal impairment = ACCOUNTING.equals(kind) ? asset.getAccumulatedImpairment() : BigDecimal.ZERO;
            BigDecimal base = asset.getEntryValue().subtract(asset.getResidualValue()).subtract(doneSum).subtract(impairment).max(BigDecimal.ZERO);
            String method = ACCOUNTING.equals(kind) ? method(asset) : taxMethod(asset, category);
            int duration = ACCOUNTING.equals(kind) ? duration(asset) : taxDuration(asset, category);
            LocalDate end = inService.plusMonths(duration);
            LocalDate start;
            Optional<FixedAssetSchedule> firstOpen = previous.stream().filter(row -> OPEN.contains(row.getStatus())).findFirst();
            if (previous.isEmpty()) {
                start = inService;
            } else if (firstOpen.isPresent()) {
                start = YearMonth.parse(firstOpen.get().getPeriodCode()).atDay(1);
            } else {
                YearMonth after = done.isEmpty() ? YearMonth.from(inService) : YearMonth.parse(done.get(done.size() - 1).getPeriodCode()).plusMonths(1);
                YearMonth now = YearMonth.now();
                start = (after.isBefore(now) ? now : after).atDay(1);
            }
            BigDecimal coefficient = null;
            BigDecimal rate = null;
            List<DepreciationCalculator.Line> lines;
            if ("DECLINING".equals(method)) {
                coefficient = coefficient(asset, category, duration);
                rate = DepreciationCalculator.decliningRate(duration, coefficient);
                YearMonth from = YearMonth.from(start);
                int months = (int) Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(from, YearMonth.from(inService).plusMonths(duration)));
                lines = DepreciationCalculator.declining(base, from, months, rate);
            } else {
                rate = BigDecimal.valueOf(1200).divide(BigDecimal.valueOf(duration), 6, RoundingMode.HALF_UP);
                lines = DepreciationCalculator.linear(base, start, end, days360);
            }
            FixedAssetPlan plan = plans.save(FixedAssetPlan.builder().assetId(asset.getId()).version(version).kind(kind).method(method)
                    .durationMonths(duration).rate(rate).coefficient(coefficient).base(base).validFrom(start).reason(cut(reason, 500))
                    .createdBy(actor()).build());
            BigDecimal cumulative = BigDecimal.ZERO;
            for (FixedAssetSchedule row : done) {
                cumulative = cumulative.add(row.getPosted());
                schedules.save(FixedAssetSchedule.builder().planId(plan.getId()).assetId(asset.getId()).kind(kind).version(version)
                        .periodCode(row.getPeriodCode()).planned(row.getPlanned()).posted(row.getPosted()).cumulative(cumulative)
                        .netValue(asset.getEntryValue().subtract(cumulative).subtract(impairment)).status(row.getStatus()).entryNumber(row.getEntryNumber())
                        .postedInPeriod(row.getPostedInPeriod()).note(row.getNote()).postedAt(row.getPostedAt()).build());
            }
            Set<String> taken = new HashSet<>(done.stream().map(FixedAssetSchedule::getPeriodCode).toList());
            for (DepreciationCalculator.Line line : lines) {
                if (taken.contains(line.period())) {
                    continue;
                }
                cumulative = cumulative.add(line.amount());
                schedules.save(FixedAssetSchedule.builder().planId(plan.getId()).assetId(asset.getId()).kind(kind).version(version)
                        .periodCode(line.period()).planned(line.amount()).cumulative(cumulative)
                        .netValue(asset.getEntryValue().subtract(cumulative).subtract(impairment)).build());
            }
        }
        asset.setPlanVersion(version);
        if ("FULLY_DEPRECIATED".equals(asset.getAccountingStatus()) && current(asset, ACCOUNTING).stream().anyMatch(row -> OPEN.contains(row.getStatus()))) {
            asset.setAccountingStatus("IN_SERVICE");
        }
        assets.save(asset);
    }

    // ================================================================== vie du bien

    public record TransferRequest(LocalDate date, String site, String resourceCode, String locationLabel, String organisation,
                                  Map<String, String> analytic, String reason) {
    }

    /** Transfert de lieu : historique ; les dotations suivantes vont sur la nouvelle section. Aucune ecriture generale. */
    public AssetView transfer(String code, TransferRequest request) {
        require(MANAGE);
        FixedAsset asset = live(code);
        if (request == null) {
            throw new BadRequestException("Indiquez le nouveau lieu");
        }
        String before = location(asset) + " " + nz(asset.getAnalyticSections());
        if (request.site() != null) asset.setSite(blankToNull(request.site()));
        if (request.resourceCode() != null) {
            asset.setResourceCode(resourceCode(request.resourceCode()));
            if (request.locationLabel() == null) {
                asset.setLocationLabel(null);
                resources().flatMap(repository -> repository.findByCode(asset.getResourceCode())).ifPresent(resource -> asset.setLocationLabel(resource.getLocationLabel()));
            }
        }
        if (request.locationLabel() != null) asset.setLocationLabel(blankToNull(request.locationLabel()));
        if (request.organisation() != null) asset.setOrganisation(blankToNull(request.organisation()));
        if (request.analytic() != null) asset.setAnalyticSections(encode(request.analytic()));
        asset.setUpdatedAt(Instant.now());
        assets.save(asset);
        event(asset, "TRANSFER", request.date() == null ? LocalDate.now() : request.date(), before, location(asset) + " " + nz(asset.getAnalyticSections()),
                request.reason(), null, null, null);
        return view(asset, List.of("Les dotations a venir, dont celle du mois en cours, vont sur la nouvelle section"));
    }

    public record AssignmentRequest(LocalDate date, String holder, String condition, String accessories) {
    }

    /** Affectation a un employe : fiche de remise de materiel ({@code PRM}). */
    public AssetView assign(String code, AssignmentRequest request) {
        require(MANAGE);
        FixedAsset asset = live(code);
        if (request == null || !StringUtils.hasText(request.holder())) {
            throw new BadRequestException("Indiquez l'employe detenteur");
        }
        if (asset.getHolder() != null) {
            throw new BadRequestException("Le bien est detenu par " + asset.getHolder() + " : enregistrez d'abord sa restitution");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        String number = vouchers.issueAndPrint("PRM", "FIXED_ASSET_ASSIGNMENT", asset.getCode() + "@" + date + "@" + request.holder().trim(),
                null, "Remise " + asset.getCode() + " a " + request.holder().trim(), actor()).number();
        asset.setHolder(request.holder().trim());
        asset.setHolderSince(date);
        assets.save(asset);
        event(asset, "ASSIGNMENT", date, null, asset.getHolder(), "Etat : " + nz(request.condition()) + " ; accessoires : " + nz(request.accessories()),
                null, number, null);
        return view(asset, List.of());
    }

    /** Restitution par l'employe, sur la meme fiche de remise. */
    public AssetView returnAsset(String code, AssignmentRequest request) {
        require(MANAGE);
        FixedAsset asset = asset(code);
        if (asset.getHolder() == null) {
            throw new BadRequestException("Le bien n'est affecte a personne");
        }
        String voucher = lastEvent(asset, "ASSIGNMENT").map(FixedAssetEvent::getVoucherNumber).orElse(null);
        String before = asset.getHolder();
        asset.setHolder(null);
        asset.setHolderSince(null);
        assets.save(asset);
        event(asset, "RETURN", request == null || request.date() == null ? LocalDate.now() : request.date(), before, null,
                request == null ? null : "Etat : " + nz(request.condition()) + " ; accessoires : " + nz(request.accessories()), null, voucher, null);
        return view(asset, List.of());
    }

    public record MaintenanceRequest(LocalDate date, String kind, String provider, String contractRef, BigDecimal cost, String supplierInvoiceNumber,
                                     String description) {
    }

    /** Entretien, reparation ou prime d'assurance : charge de la periode rattachee a la fiche, sans effet sur la valeur. */
    public AssetView maintenance(String code, MaintenanceRequest request) {
        require(MANAGE);
        FixedAsset asset = asset(code);
        if (request == null) {
            throw new BadRequestException("Indiquez l'entretien");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        String kind = upper(request.kind(), "MAINTENANCE");
        if (!List.of("MAINTENANCE", "REPAIR", "INSURANCE").contains(kind)) {
            throw new BadRequestException("Nature inconnue : " + kind + " (MAINTENANCE, REPAIR ou INSURANCE)");
        }
        boolean warranty = asset.getWarrantyEnd() != null && !date.isAfter(asset.getWarrantyEnd());
        maintenances.save(FixedAssetMaintenance.builder().assetId(asset.getId()).maintenanceDate(date).kind(kind).provider(request.provider())
                .contractRef(request.contractRef()).cost(money(request.cost())).underWarranty(warranty).supplierInvoiceNumber(request.supplierInvoiceNumber())
                .description(cut(request.description(), 1000)).createdBy(actor()).build());
        event(asset, "MAINTENANCE", date, null, kind, request.description(), money(request.cost()), null, null);
        return view(asset, warranty && "REPAIR".equals(kind) ? List.of("Reparation sous garantie (fin le " + asset.getWarrantyEnd() + ") : faites jouer la garantie")
                : List.of());
    }

    public record OperationalRequest(String status, LocalDate date, String reason) {
    }

    /** Etat operationnel (en service, en panne, en reparation, inutilise), distinct de l'etat comptable. */
    public AssetView operational(String code, OperationalRequest request) {
        require(MANAGE);
        FixedAsset asset = live(code);
        String status = request == null ? null : upper(request.status(), null);
        if (status == null || !List.of("IN_USE", "BROKEN", "IN_REPAIR", "UNUSED").contains(status)) {
            throw new BadRequestException("Etat operationnel : IN_USE, BROKEN, IN_REPAIR ou UNUSED");
        }
        String before = asset.getOperationalStatus();
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        asset.setOperationalStatus(status);
        asset.setOperationalSince(date);
        assets.save(asset);
        event(asset, "OPERATIONAL", date, before, status, request.reason(), null, null, null);
        return view(asset, List.of());
    }

    /** Releve mensuel du compteur d'un bien amorti en unites d'oeuvre. */
    public AssetView meterReading(String code, String period, BigDecimal units) {
        require(MANAGE);
        FixedAsset asset = live(code);
        if (!"UNITS".equals(method(asset))) {
            throw new BadRequestException("Le bien n'est pas amorti en unites d'oeuvre");
        }
        YearMonth month = month(period);
        if (units == null || units.signum() < 0) {
            throw new BadRequestException("Le releve est positif");
        }
        FixedAssetMeterReading reading = readings.findByAssetIdAndPeriodCode(asset.getId(), month.toString())
                .orElseGet(() -> FixedAssetMeterReading.builder().assetId(asset.getId()).periodCode(month.toString()).createdBy(actor()).build());
        if (current(asset, ACCOUNTING).stream().anyMatch(row -> row.getPeriodCode().equals(month.toString()) && DONE.contains(row.getStatus()))) {
            throw new BadRequestException("La dotation de " + month + " est deja comptabilisee");
        }
        reading.setUnits(units);
        readings.save(reading);
        return view(asset, List.of());
    }

    /** Lien facultatif avec un ou plusieurs actifs de l'inventaire. */
    public AssetView linkInventory(String code, List<String> inventoryCodes, boolean unlink) {
        require(MANAGE);
        FixedAsset asset = asset(code);
        if (unlink) {
            links.findAllByFixedAssetId(asset.getId()).stream().filter(link -> inventoryCodes == null || inventoryCodes.contains(link.getInventoryAssetCode()))
                    .forEach(links::delete);
        } else {
            linkInventory(asset, inventoryCodes);
        }
        return view(asset, List.of());
    }

    private void linkInventory(FixedAsset asset, List<String> codes) {
        if (codes == null) {
            return;
        }
        Set<String> existing = new HashSet<>(links.findAllByFixedAssetId(asset.getId()).stream().map(FixedAssetInventoryLink::getInventoryAssetCode).toList());
        AssetRepository repository = inventoryAssets.getIfAvailable();
        for (String raw : codes) {
            if (!StringUtils.hasText(raw) || existing.contains(raw.trim())) {
                continue;
            }
            String code = raw.trim();
            if (repository != null && repository.findByAssetCode(code).isEmpty()) {
                throw new BadRequestException("Actif d'inventaire inconnu : " + code);
            }
            links.save(FixedAssetInventoryLink.builder().fixedAssetId(asset.getId()).inventoryAssetCode(code).createdBy(actor()).build());
            existing.add(code);
        }
    }

    // ================================================================== reclassement, TVA

    public record ReclassRequest(String categoryCode, LocalDate date, String reason) {
    }

    /** Changement de categorie ou de compte : D nouveau compte ; C ancien, pour la valeur brute, le cumul et la depreciation. */
    public AssetView reclassify(String code, ReclassRequest request) {
        require(REVISE);
        FixedAsset asset = live(code);
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Motif du reclassement obligatoire");
        }
        FaCategory source = category(asset.getCategoryCode());
        FaCategory target = category(request.categoryCode());
        if (source.getCode().equals(target.getCode())) {
            throw new BadRequestException("Le bien est deja dans la categorie " + target.getCode());
        }
        if ("NONE".equals(target.getMethod()) != "NONE".equals(source.getMethod())) {
            throw new BadRequestException("Un bien amortissable ne se reclasse pas en categorie non amortissable, ni l'inverse");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        reclassAccounts(asset, source, target, date, request.reason().trim());
        asset.setCategoryCode(target.getCode());
        assets.save(asset);
        return view(asset, List.of());
    }

    private void reclassAccounts(FixedAsset asset, FaCategory source, FaCategory target, LocalDate date, String reason) {
        if (!Boolean.TRUE.equals(asset.getBooked())) {
            event(asset, "RECLASS", date, source.getCode(), target.getCode(), reason, null, null, null);
            return;
        }
        String label = "Reclassement " + asset.getCode() + " " + source.getCode() + " vers " + target.getCode();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        pair(lines, target.getAssetAccount(), source.getAssetAccount(), asset.getEntryValue(), label, asset.getCode());
        pair(lines, source.getDepreciationAccount(), target.getDepreciationAccount(), asset.getAccumulatedDepreciation(), label, asset.getCode());
        pair(lines, source.getImpairmentAccount(), target.getImpairmentAccount(), asset.getAccumulatedImpairment(), label, asset.getCode());
        String entryNumber = null;
        if (!lines.isEmpty()) {
            entryNumber = book(journal(), date, label, "FIXED_ASSET_RECLASS", asset.getCode(), lines).getEntryNumber();
        }
        event(asset, "RECLASS", date, source.getCode(), target.getCode(), reason, asset.getEntryValue(), null, entryNumber);
    }

    /** Ajoute D debit ; C credit quand les comptes different et que le montant est positif. */
    private void pair(List<AccountingRequests.EntryLine> lines, String debit, String credit, BigDecimal amount, String label, String reference) {
        if (amount == null || amount.signum() <= 0 || debit == null || credit == null || debit.equals(credit)) {
            return;
        }
        lines.add(line(debit, null, label, amount, null, reference, null));
        lines.add(line(credit, null, label, null, amount, reference, null));
    }

    public record VatRegularizationRequest(LocalDate date, String reason, BigDecimal amount) {
    }

    /**
     * Regularisation de la TVA deduite sur un bien qui change d'affectation (usage exonere, usage
     * prive) avant la fin de la periode de regularisation : D charge ; C TVA recuperable sur
     * immobilisations. Une seule fois par bien ; la sortie la fait d'office.
     */
    public AssetView regularizeVat(String code, VatRegularizationRequest request) {
        require(REVISE);
        FixedAsset asset = live(code);
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Motif de la regularisation obligatoire (changement d'affectation)");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        BigDecimal amount = request.amount() != null ? money(request.amount()) : vatRegularization(asset, date);
        if (amount.signum() <= 0) {
            throw new BadRequestException("Aucune TVA a reverser sur ce bien (TVA deduite " + plain(asset.getDeductedVat()) + ", periode de "
                    + vatYears() + " ans)");
        }
        String entry = postVatRegularization(asset, date, amount, request.reason().trim());
        return view(asset, List.of("TVA reversee : " + plain(amount) + " (piece " + entry + ")"));
    }

    BigDecimal vatRegularization(FixedAsset asset, LocalDate date) {
        if (lastEvent(asset, "VAT_REGULARIZATION").isPresent()) {
            return BigDecimal.ZERO;
        }
        return DepreciationCalculator.vatRegularization(asset.getDeductedVat(), asset.getAcquisitionDate(), date, vatYears());
    }

    private String postVatRegularization(FixedAsset asset, LocalDate date, BigDecimal amount, String reason) {
        if (lastEvent(asset, "VAT_REGULARIZATION").isPresent()) {
            throw new BadRequestException("La TVA de ce bien est deja regularisee");
        }
        String label = "Regularisation TVA deduite " + asset.getCode() + " " + asset.getDesignation();
        String charge = settings.text("accounting.fixed-asset.vat-regularization-account", "658");
        String vat = settings.text("accounting.purchase.vat-account-fixed-assets", "4451");
        AccEntry entry = book(journal(), date, label, "FIXED_ASSET_VAT", asset.getCode(), List.of(
                line(charge, null, label, amount, null, asset.getCode(), charge.startsWith("6") ? analytic(asset) : null),
                new AccountingRequests.EntryLine(vat, null, label, null, amount, null, null, null, "TVA_REGUL", null, null, asset.getCode(), null)));
        event(asset, "VAT_REGULARIZATION", date, plain(asset.getDeductedVat()), plain(amount), reason, amount, null, entry.getEntryNumber());
        return entry.getEntryNumber();
    }

    // ================================================================== depreciations

    public record ImpairmentRequest(String kind, LocalDate date, BigDecimal currentValue, String indicator, String justificationRef) {
    }

    /**
     * Depreciation (valeur actuelle inferieure a la valeur nette) ou reprise (plafonnee a la valeur
     * nette qu'aurait eue le bien sans depreciation). Au-dela du seuil, une seconde personne valide.
     */
    public AssetView impair(String code, ImpairmentRequest request) {
        require(IMPAIR);
        FixedAsset asset = live(code);
        FaCategory category = category(asset.getCategoryCode());
        if (category.getImpairmentAccount() == null || category.getImpairmentExpenseAccount() == null || category.getImpairmentReversalAccount() == null) {
            throw new BadRequestException("La categorie " + category.getCode() + " n'a pas de comptes de depreciation");
        }
        if (!Boolean.TRUE.equals(asset.getBooked())) {
            throw new BadRequestException("Seul un bien en comptabilite se deprecie");
        }
        if (request == null || request.currentValue() == null || request.currentValue().signum() < 0) {
            throw new BadRequestException("Valeur actuelle du bien obligatoire");
        }
        String kind = upper(request.kind(), "IMPAIRMENT");
        BigDecimal net = netValue(asset);
        BigDecimal current = money(request.currentValue());
        BigDecimal amount;
        if ("IMPAIRMENT".equals(kind)) {
            if (!StringUtils.hasText(request.justificationRef())) {
                throw new BadRequestException("Joignez le justificatif de la valeur actuelle (estimation, rapport)");
            }
            amount = net.subtract(current);
            if (amount.signum() <= 0) {
                throw new BadRequestException("La valeur actuelle (" + plain(current) + ") n'est pas inferieure a la valeur nette (" + plain(net) + ")");
            }
        } else if ("REVERSAL".equals(kind)) {
            BigDecimal cap = theoreticalNet(asset).subtract(net).max(BigDecimal.ZERO);
            amount = current.subtract(net).min(asset.getAccumulatedImpairment()).min(cap);
            if (amount.signum() <= 0) {
                throw new BadRequestException("Aucune reprise possible (depreciation " + plain(asset.getAccumulatedImpairment()) + ", plafond " + plain(cap) + ")");
            }
        } else {
            throw new BadRequestException("Nature : IMPAIRMENT ou REVERSAL");
        }
        FixedAssetImpairment impairment = FixedAssetImpairment.builder().assetId(asset.getId()).kind(kind)
                .impairmentDate(request.date() == null ? LocalDate.now() : request.date()).indicator(request.indicator()).currentValue(current)
                .amount(amount).justificationRef(request.justificationRef()).status("PENDING_APPROVAL").requestedBy(actor()).build();
        BigDecimal threshold = BigDecimal.valueOf(number("accounting.fixed-asset.impairment-second-validation", 1_000_000));
        if (threshold.signum() > 0 && amount.compareTo(threshold) > 0) {
            impairments.save(impairment);
            notifier.notifyAccountants("Depreciation a valider", "La " + ("IMPAIRMENT".equals(kind) ? "depreciation" : "reprise") + " de " + plain(amount)
                    + " XAF du bien " + asset.getCode() + " depasse le seuil de seconde validation.", asset.getCode(), "/accounting/fixed-assets/" + asset.getCode(), null);
            return view(asset, List.of("Au-dela de " + plain(threshold) + " XAF : une seconde personne valide"));
        }
        postImpairment(asset, category, impairment);
        return view(asset, List.of());
    }

    /** Seconde validation d'une depreciation au-dela du seuil, par une autre personne. */
    public AssetView decideImpairment(Long id, boolean approve) {
        require(IMPAIR);
        FixedAssetImpairment impairment = impairments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Depreciation introuvable : " + id));
        if (!"PENDING_APPROVAL".equals(impairment.getStatus())) {
            throw new BadRequestException("Cette depreciation n'attend pas de validation");
        }
        if (CashActorResolver.samePerson(impairment.getRequestedBy(), actor())) {
            throw new ForbiddenException("La seconde validation revient a une autre personne que le demandeur");
        }
        FixedAsset asset = assets.findById(impairment.getAssetId()).orElseThrow();
        impairment.setApprovedBy(actor());
        if (!approve) {
            impairment.setStatus("REJECTED");
            impairments.save(impairment);
            return view(asset, List.of());
        }
        postImpairment(asset, category(asset.getCategoryCode()), impairment);
        return view(asset, List.of());
    }

    private void postImpairment(FixedAsset asset, FaCategory category, FixedAssetImpairment impairment) {
        boolean reversal = "REVERSAL".equals(impairment.getKind());
        String label = (reversal ? "Reprise de depreciation " : "Depreciation ") + asset.getCode() + " " + asset.getDesignation();
        BigDecimal amount = impairment.getAmount();
        List<AccountingRequests.EntryLine> lines = reversal
                ? List.of(line(category.getImpairmentAccount(), null, label, amount, null, asset.getCode(), null),
                line(category.getImpairmentReversalAccount(), null, label, null, amount, asset.getCode(), analytic(asset)))
                : List.of(line(category.getImpairmentExpenseAccount(), null, label, amount, null, asset.getCode(), analytic(asset)),
                line(category.getImpairmentAccount(), null, label, null, amount, asset.getCode(), null));
        AccEntry entry = book(journal(), impairment.getImpairmentDate(), label, "FIXED_ASSET_IMPAIRMENT", asset.getCode(), lines);
        impairment.setEntryNumber(entry.getEntryNumber());
        impairment.setStatus("POSTED");
        impairments.save(impairment);
        asset.setAccumulatedImpairment(asset.getAccumulatedImpairment().add(reversal ? amount.negate() : amount));
        assets.save(asset);
        event(asset, reversal ? "IMPAIRMENT_REVERSAL" : "IMPAIRMENT", impairment.getImpairmentDate(), null, plain(impairment.getCurrentValue()),
                impairment.getIndicator(), amount, null, entry.getEntryNumber());
        if (asset.getPlanVersion() > 0 && !"NONE".equals(method(asset))) {
            regenerate(asset, reversal ? "Reprise de depreciation" : "IMPAIRMENT");
        }
    }

    /** Valeur nette qu'aurait le bien sans depreciation, au dernier mois dote. */
    private BigDecimal theoreticalNet(FixedAsset asset) {
        if (asset.getInServiceDate() == null || "NONE".equals(method(asset))) {
            return asset.getEntryValue();
        }
        String last = current(asset, ACCOUNTING).stream().filter(row -> DONE.contains(row.getStatus())).map(FixedAssetSchedule::getPeriodCode)
                .reduce((a, b) -> b).orElse(null);
        if (last == null) {
            return asset.getEntryValue();
        }
        List<DepreciationCalculator.Line> lines = "DECLINING".equals(method(asset))
                ? DepreciationCalculator.declining(asset.getEntryValue().subtract(asset.getResidualValue()), YearMonth.from(asset.getInServiceDate()),
                duration(asset), DepreciationCalculator.decliningRate(duration(asset), coefficient(asset, category(asset.getCategoryCode()), duration(asset))))
                : DepreciationCalculator.linear(asset.getEntryValue().subtract(asset.getResidualValue()), asset.getInServiceDate(),
                asset.getInServiceDate().plusMonths(duration(asset)), days360());
        BigDecimal cumulative = lines.stream().filter(line -> line.period().compareTo(last) <= 0).map(DepreciationCalculator.Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return asset.getEntryValue().subtract(cumulative);
    }

    // ================================================================== sorties

    public record DisposalRequest(String reason, LocalDate date, BigDecimal price, BigDecimal vatRate, String buyerName, String justificationRef,
                                  Boolean removeAmenity) {
    }

    /** Prepare une sortie (PV {@code PSI} a valider par une seconde personne). */
    public FixedAssetDisposal prepareDisposal(String code, DisposalRequest request) {
        require(DISPOSE);
        FixedAsset asset = live(code);
        if (request == null) {
            throw new BadRequestException("Indiquez le motif et la date de sortie");
        }
        String reason = upper(request.reason(), null);
        if (reason == null || !DISPOSAL_REASONS.contains(reason)) {
            throw new BadRequestException("Motif de sortie : " + String.join(", ", DISPOSAL_REASONS));
        }
        if (!StringUtils.hasText(request.justificationRef())) {
            throw new BadRequestException("Joignez la piece justificative (facture de vente, constat de destruction, declaration de vol, expertise)");
        }
        if (assets.findAllByParentIdOrderByCodeAsc(asset.getId()).stream().anyMatch(child -> LIVE.contains(child.getAccountingStatus()))) {
            throw new BadRequestException("Sortez d'abord les composants de " + code);
        }
        if (disposals.findAllByAssetIdOrderByCreatedAtAsc(asset.getId()).stream().anyMatch(item -> !"CANCELLED".equals(item.getStatus()))) {
            throw new BadRequestException("Une sortie est deja preparee pour " + code);
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        if (asset.getInServiceDate() != null && date.isBefore(asset.getInServiceDate())) {
            throw new BadRequestException("La sortie suit la mise en service (" + asset.getInServiceDate() + ")");
        }
        BigDecimal price = money(request.price());
        if (price.signum() < 0) {
            throw new BadRequestException("Le prix de cession est positif ou nul");
        }
        BigDecimal vatRate = request.vatRate() == null ? BigDecimal.ZERO : request.vatRate();
        BigDecimal complement = complement(asset, date, false);
        BigDecimal cumulative = asset.getAccumulatedDepreciation().add(complement);
        BigDecimal net = asset.getEntryValue().subtract(cumulative).subtract(asset.getAccumulatedImpairment());
        FixedAssetDisposal disposal = FixedAssetDisposal.builder().assetId(asset.getId()).reason(reason).disposalDate(date).grossValue(asset.getEntryValue())
                .accumulatedDepreciation(cumulative).accumulatedImpairment(asset.getAccumulatedImpairment()).netValue(net).price(price).vatRate(vatRate)
                .vatAmount(money(price.multiply(vatRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .gainLoss(DepreciationCalculator.gainOrLoss(price, net)).buyerName(request.buyerName()).justificationRef(request.justificationRef())
                .vatRegularization(vatRate.signum() > 0 ? BigDecimal.ZERO : vatRegularization(asset, date))
                .removeAmenity(Boolean.TRUE.equals(request.removeAmenity())).preparedBy(actor()).build();
        disposal = disposals.save(disposal);
        event(asset, "DISPOSAL_PREPARED", date, null, reason, request.justificationRef(), price, null, null);
        return disposal;
    }

    /**
     * Validation de la sortie par une seconde personne : complement de dotation au jour pres, sortie
     * de l'actif (cumuls soldes, valeur nette en charge HAO), cession (D 485 ; C 82x ; C TVA),
     * reprise du derogatoire et de la subvention, regularisation de la TVA deduite, fiche sortie.
     */
    public FixedAssetDisposal validateDisposal(Long id) {
        require(DISPOSE);
        FixedAssetDisposal disposal = disposal(id);
        if (!"PREPARED".equals(disposal.getStatus())) {
            throw new BadRequestException("Cette sortie n'est pas a valider (" + disposal.getStatus() + ")");
        }
        if (CashActorResolver.samePerson(disposal.getPreparedBy(), actor())) {
            throw new ForbiddenException("Celui qui prepare une sortie ne la valide pas : une seconde personne avec le droit FIXED_ASSET:DISPOSE valide le PV");
        }
        FixedAsset asset = assets.findById(disposal.getAssetId()).orElseThrow();
        FaCategory category = category(asset.getCategoryCode());
        LocalDate date = disposal.getDisposalDate();
        String number = vouchers.issueAndPrint("PSI", "FIXED_ASSET_DISPOSAL", asset.getCode(), disposal.getPrice(),
                "Sortie " + asset.getCode() + " " + asset.getDesignation(), actor()).number();
        disposal.setVoucherNumber(number);
        // 1. Complement de dotation jusqu'a la date de sortie.
        if (!"NONE".equals(method(asset)) && asset.getPlanVersion() > 0) {
            BigDecimal complement = complement(asset, date, true);
            if (complement.signum() > 0) {
                String label = "Complement de dotation " + asset.getCode() + " au " + date;
                AccEntry entry = book(journal(), date, label, "FIXED_ASSET_DEPRECIATION", asset.getCode() + "@" + date, List.of(
                        line(category.getExpenseAccount(), null, label, complement, null, asset.getCode(), analytic(asset)),
                        line(category.getDepreciationAccount(), null, label, null, complement, asset.getCode(), null)));
                disposal.setComplementEntry(entry.getEntryNumber());
                markComplement(asset, date, entry.getEntryNumber());
                asset.setAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(complement));
            } else {
                markComplement(asset, date, null);
            }
        }
        // 2. Sortie de l'actif.
        BigDecimal gross = asset.getEntryValue();
        BigDecimal cumulative = asset.getAccumulatedDepreciation();
        BigDecimal impairment = asset.getAccumulatedImpairment();
        BigDecimal net = gross.subtract(cumulative).subtract(impairment);
        String label = "Sortie de l'actif " + asset.getCode() + " " + asset.getDesignation() + " (" + disposal.getReason() + ")";
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        if (Boolean.TRUE.equals(asset.getBooked()) && gross.signum() > 0) {
            if (cumulative.signum() > 0) lines.add(line(category.getDepreciationAccount(), null, label, cumulative, null, asset.getCode(), null));
            if (impairment.signum() > 0) lines.add(line(category.getImpairmentAccount(), null, label, impairment, null, asset.getCode(), null));
            if (net.signum() > 0) lines.add(line(category.getDisposalValueAccount(), null, label, net, null, asset.getCode(), null));
            lines.add(line(category.getAssetAccount(), null, label, null, gross, asset.getCode(), null));
            BigDecimal derogatory = asset.getAccumulatedDerogatory();
            if (derogatory.signum() > 0) {
                lines.add(line(settings.text("accounting.fixed-asset.derogatory-account", "151"), null, label + " (derogatoire)", derogatory, null, asset.getCode(), null));
                lines.add(line(settings.text("accounting.fixed-asset.derogatory-reversal-account", "861"), null, label + " (derogatoire)", null, derogatory, asset.getCode(), null));
                asset.setAccumulatedDerogatory(BigDecimal.ZERO);
            }
            BigDecimal subsidy = asset.getSubsidyAmount().subtract(asset.getSubsidyReleased());
            if (subsidy.signum() > 0 && asset.getSubsidyAccount() != null) {
                lines.add(line(asset.getSubsidyAccount(), null, label + " (subvention)", subsidy, null, asset.getCode(), null));
                lines.add(line(settings.text("accounting.fixed-asset.subsidy-income-account", "799"), null, label + " (subvention)", null, subsidy, asset.getCode(), analytic(asset)));
                asset.setSubsidyReleased(asset.getSubsidyAmount());
            }
            AccEntry exit = book(journal(), date, label, "FIXED_ASSET_DISPOSAL", number, lines);
            disposal.setExitEntry(exit.getEntryNumber());
        }
        // 3. Cession : D creance sur cession ; C produit de cession ; C TVA facturee.
        if (disposal.getPrice().signum() > 0) {
            String saleLabel = "Cession " + asset.getCode() + " " + asset.getDesignation() + (disposal.getBuyerName() == null ? "" : " a " + disposal.getBuyerName());
            List<AccountingRequests.EntryLine> sale = new ArrayList<>();
            sale.add(line(settings.text("accounting.fixed-asset.disposal-receivable-account", "485"), null, saleLabel,
                    disposal.getPrice().add(disposal.getVatAmount()), null, number, null));
            sale.add(line(category.getDisposalIncomeAccount(), null, saleLabel, null, disposal.getPrice(), number, null));
            if (disposal.getVatAmount().signum() > 0) {
                sale.add(new AccountingRequests.EntryLine(settings.text("accounting.sales.vat-account", "4431"), null, "TVA " + saleLabel, null,
                        disposal.getVatAmount(), null, null, null, "TVA" + disposal.getVatRate().stripTrailingZeros().toPlainString(), disposal.getPrice(),
                        null, number, null));
            }
            disposal.setSaleEntry(book(settings.text("accounting.fixed-asset.sale-journal", "VT"), date, saleLabel, "FIXED_ASSET_SALE", number, sale).getEntryNumber());
        }
        // 4. TVA deduite a reverser quand le bien sort sans TVA avant la fin de la periode de regularisation.
        BigDecimal regularization = disposal.getVatRate().signum() > 0 ? BigDecimal.ZERO : vatRegularization(asset, date);
        if (regularization.signum() > 0) {
            postVatRegularization(asset, date, regularization, "Sortie sans TVA (" + disposal.getReason() + ")");
        }
        disposal.setVatRegularization(regularization);
        disposal.setAccumulatedDepreciation(cumulative);
        disposal.setAccumulatedImpairment(impairment);
        disposal.setNetValue(net);
        disposal.setGainLoss(DepreciationCalculator.gainOrLoss(disposal.getPrice(), net));
        disposal.setStatus("VALIDATED");
        disposal.setValidatedBy(actor());
        disposal.setValidatedAt(Instant.now());
        disposals.save(disposal);
        // 5. Fiche sortie, etiquette desactivee, equipement de la ressource retire si confirme.
        asset.setAccountingStatus("DISPOSED");
        asset.setDisposedOn(date);
        asset.setQrActive(false);
        asset.setHolder(null);
        asset.setUpdatedAt(Instant.now());
        if (Boolean.TRUE.equals(disposal.getRemoveAmenity()) && asset.getAmenityLinkId() != null) {
            ResourceAmenityLinkRepository repository = amenityLinks.getIfAvailable();
            if (repository != null) {
                repository.findById(asset.getAmenityLinkId()).ifPresent(repository::delete);
            }
            asset.setAmenityLinkId(null);
        }
        assets.save(asset);
        event(asset, "DISPOSAL", date, plain(net), disposal.getReason(), "Plus ou moins-value " + plain(disposal.getGainLoss()), disposal.getPrice(), number,
                disposal.getExitEntry());
        return disposal;
    }

    public FixedAssetDisposal cancelDisposal(Long id) {
        require(DISPOSE);
        FixedAssetDisposal disposal = disposal(id);
        if (!"PREPARED".equals(disposal.getStatus())) {
            throw new BadRequestException("Seule une sortie preparee s'annule");
        }
        disposal.setStatus("CANCELLED");
        return disposals.save(disposal);
    }

    public record CollectRequest(LocalDate date, BigDecimal amount, String treasuryAccount, String journalCode) {
    }

    /** Encaissement du prix de cession : D tresorerie ; C 485, lettre par le numero du PV. */
    public FixedAssetDisposal collect(Long id, CollectRequest request) {
        require(DISPOSE);
        FixedAssetDisposal disposal = disposal(id);
        if (!"VALIDATED".equals(disposal.getStatus()) || disposal.getSaleEntry() == null) {
            throw new BadRequestException("Aucune cession a encaisser");
        }
        if (request == null || !StringUtils.hasText(request.treasuryAccount()) || !StringUtils.hasText(request.journalCode())) {
            throw new BadRequestException("Compte et journal de tresorerie obligatoires");
        }
        BigDecimal due = disposal.getPrice().add(disposal.getVatAmount()).subtract(disposal.getCollectedAmount());
        BigDecimal amount = request.amount() == null ? due : money(request.amount());
        if (amount.signum() <= 0 || amount.compareTo(due) > 0) {
            throw new BadRequestException("L'encaissement est positif et au plus le reste du (" + plain(due) + ")");
        }
        String receivable = settings.text("accounting.fixed-asset.disposal-receivable-account", "485");
        FixedAsset asset = assets.findById(disposal.getAssetId()).orElseThrow();
        String label = "Encaissement cession " + asset.getCode() + " " + disposal.getVoucherNumber();
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        book(request.journalCode().trim(), date, label, "FIXED_ASSET_SALE_COLLECTION", disposal.getVoucherNumber(), List.of(
                line(request.treasuryAccount().trim(), null, label, amount, null, disposal.getVoucherNumber(), null),
                line(receivable, null, label, null, amount, disposal.getVoucherNumber(), null)));
        disposal.setCollectedAmount(disposal.getCollectedAmount().add(amount));
        disposals.save(disposal);
        lettering.autoLetterByReference(receivable, null, disposal.getVoucherNumber());
        return disposal;
    }

    public record IndemnityRequest(LocalDate date, BigDecimal amount, String insurerAccount) {
    }

    /** Indemnite d'assurance notifiee apres un vol ou un sinistre : D compte de l'assureur ; C produit HAO. */
    public FixedAssetDisposal indemnity(Long id, IndemnityRequest request) {
        require(DISPOSE);
        FixedAssetDisposal disposal = disposal(id);
        if (!"VALIDATED".equals(disposal.getStatus()) || !List.of("THEFT", "LOSS", "DISASTER").contains(disposal.getReason())) {
            throw new BadRequestException("L'indemnite d'assurance suit une sortie validee pour vol, perte ou sinistre");
        }
        if (disposal.getIndemnityEntry() != null) {
            throw new BadRequestException("L'indemnite est deja enregistree");
        }
        BigDecimal amount = request == null ? BigDecimal.ZERO : money(request.amount());
        if (amount.signum() <= 0) {
            throw new BadRequestException("Montant de l'indemnite obligatoire");
        }
        FixedAsset asset = assets.findById(disposal.getAssetId()).orElseThrow();
        String label = "Indemnite d'assurance " + asset.getCode() + " " + disposal.getVoucherNumber();
        String insurer = StringUtils.hasText(request.insurerAccount()) ? request.insurerAccount().trim()
                : settings.text("accounting.fixed-asset.insurance-claim-account", "471");
        AccEntry entry = book(journal(), request.date() == null ? LocalDate.now() : request.date(), label, "FIXED_ASSET_INDEMNITY",
                disposal.getVoucherNumber(), List.of(line(insurer, null, label, amount, null, disposal.getVoucherNumber(), null),
                        line(settings.text("accounting.fixed-asset.insurance-income-account", "841"), null, label, null, amount, disposal.getVoucherNumber(), null)));
        disposal.setIndemnityAmount(amount);
        disposal.setIndemnityEntry(entry.getEntryNumber());
        return disposals.save(disposal);
    }

    /** Complement de dotation jusqu'a la date : dotations non comptabilisees des mois anterieurs, et le mois de sortie au jour pres. */
    private BigDecimal complement(FixedAsset asset, LocalDate date, boolean apply) {
        if ("NONE".equals(method(asset)) || asset.getPlanVersion() == 0) {
            return BigDecimal.ZERO;
        }
        String month = YearMonth.from(date).toString();
        BigDecimal total = BigDecimal.ZERO;
        for (FixedAssetSchedule row : current(asset, ACCOUNTING)) {
            if (!OPEN.contains(row.getStatus())) {
                continue;
            }
            int order = row.getPeriodCode().compareTo(month);
            if (order < 0) {
                total = total.add(row.getPlanned());
            } else if (order == 0) {
                total = total.add(DepreciationCalculator.prorata(row.getPlanned(), date, days360()));
            }
        }
        return total;
    }

    private void markComplement(FixedAsset asset, LocalDate date, String entryNumber) {
        String month = YearMonth.from(date).toString();
        for (String kind : List.of(ACCOUNTING, TAX)) {
            for (FixedAssetSchedule row : current(asset, kind)) {
                if (!OPEN.contains(row.getStatus())) {
                    continue;
                }
                int order = row.getPeriodCode().compareTo(month);
                if (ACCOUNTING.equals(kind) && order <= 0) {
                    BigDecimal amount = order < 0 ? row.getPlanned() : DepreciationCalculator.prorata(row.getPlanned(), date, days360());
                    row.setPosted(amount);
                    row.setStatus("POSTED");
                    row.setEntryNumber(entryNumber);
                    row.setPostedInPeriod(month);
                    row.setNote("Complement de dotation a la sortie du " + date);
                    row.setPostedAt(Instant.now());
                } else {
                    row.setStatus("CANCELLED");
                    row.setNote("Bien sorti le " + date);
                }
                schedules.save(row);
            }
        }
    }

    // ================================================================== entree depuis les achats (FixedAssetIntake)

    @Override
    public void checkInvoiceLine(String account, BigDecimal unitPrice, String fixedAssetCode) {
        if (StringUtils.hasText(fixedAssetCode)) {
            FixedAsset asset = assets.findByCode(fixedAssetCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BadRequestException("Fiche d'immobilisation inconnue : " + fixedAssetCode));
            if (!LIVE.contains(asset.getAccountingStatus())) {
                throw new BadRequestException("La fiche " + asset.getCode() + " est sortie ou annulee");
            }
            return;
        }
        FaCategory category = categoryForAccount(account);
        if (category == null) {
            throw new BadRequestException("Aucune categorie d'immobilisation pour le compte " + account + " : creez-la ou choisissez le compte d'une categorie");
        }
        if (!Boolean.TRUE.equals(category.getInProgress()) && !"NONE".equals(category.getMethod()) && unitPrice != null
                && unitPrice.compareTo(category.getThreshold()) < 0) {
            throw new BadRequestException("Montant unitaire (" + plain(unitPrice) + ") sous le seuil d'immobilisation de la categorie " + category.getLabel()
                    + " (" + plain(category.getThreshold()) + " XAF HT) : imputez la ligne en charge");
        }
    }

    @Override
    public String vatAccountFor(String account) {
        FaCategory category = account != null && account.startsWith("2") ? categoryForAccount(account) : null;
        return category != null && !Boolean.TRUE.equals(category.getVatRecoverable()) ? account : null;
    }

    @Override
    public String payableAccount(List<AccSupplierInvoiceLine> lines, String supplierAccount) {
        String investment = settings.text("accounting.fixed-asset.supplier-account", "481");
        if (!StringUtils.hasText(investment) || lines == null || lines.isEmpty()) {
            return supplierAccount;
        }
        boolean allAssets = lines.stream().allMatch(line -> line.getAccountNumber() != null && line.getAccountNumber().startsWith("2"));
        return allAssets ? investment : supplierAccount;
    }

    @Override
    public void onInvoicePosted(AccSupplierInvoice invoice, List<AccSupplierInvoiceLine> lines, String supplierName) {
        boolean credit = "CREDIT_NOTE".equals(invoice.getKind());
        for (AccSupplierInvoiceLine line : lines) {
            boolean assetLine = line.getAccountNumber() != null && line.getAccountNumber().startsWith("2");
            if (!assetLine && line.getFixedAssetCode() == null) {
                continue;
            }
            BigDecimal nonRecoverable = line.getVatAccount() != null && line.getVatAccount().equals(line.getAccountNumber()) ? line.getVatAmount() : BigDecimal.ZERO;
            BigDecimal deducted = nonRecoverable.signum() == 0 ? line.getVatAmount() : BigDecimal.ZERO;
            String origin = invoice.getNumber() + "#" + line.getLineNo();
            if (credit) {
                FixedAsset target = line.getFixedAssetCode() != null ? assets.findByCode(line.getFixedAssetCode()).orElse(null)
                        : invoice.getOriginalNumber() == null ? null : assets.findAllBySupplierInvoiceNumber(invoice.getOriginalNumber()).stream()
                        .filter(asset -> LIVE.contains(asset.getAccountingStatus()) && asset.getParentId() == null).findFirst().orElse(null);
                if (target == null) {
                    notifier.notifyAccountants("Avoir sur immobilisation sans fiche", "L'avoir " + invoice.getNumber() + " porte sur le compte "
                            + line.getAccountNumber() + " sans fiche identifiee : rattachez-le a la fiche du bien.", invoice.getNumber(), "/accounting/fixed-assets", null);
                    continue;
                }
                addValue(target, "CREDIT_NOTE", line.getAmountHt().add(nonRecoverable).negate(), invoice.getInvoiceDate(), origin, invoice.getEntryNumber(),
                        deducted.negate(), "CREDIT_NOTE");
                continue;
            }
            if (line.getFixedAssetCode() != null) {
                FixedAsset target = assets.findByCode(line.getFixedAssetCode()).orElseThrow();
                String type = "IN_PROGRESS".equals(target.getAccountingStatus()) ? "PURCHASE"
                        : "DRAFT".equals(target.getAccountingStatus()) ? "ACCESSORY_COST" : "SUBSEQUENT_EXPENSE";
                addValue(target, type, line.getAmountHt(), invoice.getInvoiceDate(), origin, invoice.getEntryNumber(), deducted, "SUBSEQUENT_EXPENSE");
                if (nonRecoverable.signum() > 0) {
                    addValue(target, "NON_RECOVERABLE_VAT", nonRecoverable, invoice.getInvoiceDate(), origin, invoice.getEntryNumber(), BigDecimal.ZERO,
                            "SUBSEQUENT_EXPENSE");
                }
                continue;
            }
            FaCategory category = categoryForAccount(line.getAccountNumber());
            if (category == null) {
                continue;
            }
            BigDecimal quantity = line.getQuantity() == null || line.getQuantity().signum() <= 0 ? BigDecimal.ONE : line.getQuantity();
            boolean perUnit = "UNIT".equals(category.getTracking()) && quantity.compareTo(BigDecimal.ONE) > 0 && quantity.stripTrailingZeros().scale() <= 0
                    && quantity.intValue() <= 500;
            int count = perUnit ? quantity.intValue() : 1;
            BigDecimal leftValue = line.getAmountHt();
            BigDecimal leftVat = nonRecoverable;
            BigDecimal leftDeducted = deducted;
            for (int index = 1; index <= count; index++) {
                boolean last = index == count;
                BigDecimal value = last ? leftValue : line.getAmountHt().divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
                BigDecimal vat = last ? leftVat : nonRecoverable.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
                BigDecimal ded = last ? leftDeducted : deducted.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
                leftValue = leftValue.subtract(value);
                leftVat = leftVat.subtract(vat);
                leftDeducted = leftDeducted.subtract(ded);
                boolean inProgress = Boolean.TRUE.equals(category.getInProgress());
                FixedAsset asset = assets.save(FixedAsset.builder().code(nextCode(invoice.getInvoiceDate().getYear())).categoryCode(category.getCode())
                        .designation(cut(line.getDesignation(), 300)).quantity(perUnit ? 1 : Math.max(1, quantity.intValue()))
                        .entryMode(inProgress ? "IN_PROGRESS" : "PURCHASE").supplierCode(invoice.getSupplierCode()).supplierName(supplierName)
                        .supplierInvoiceNumber(invoice.getNumber()).supplierInvoiceLineNo(line.getLineNo()).orderNumber(invoice.getOrderNumber())
                        .entryNumber(invoice.getEntryNumber()).booked(true).acquisitionDate(invoice.getInvoiceDate()).receivedDate(invoice.getInvoiceDate())
                        .entryValue(value.add(vat)).deductedVat(ded).accountingStatus(inProgress ? "IN_PROGRESS" : "DRAFT")
                        .documentRefs(invoice.getDocumentRef()).createdBy(actor()).build());
                valueLine(asset, "PURCHASE", value, invoice.getInvoiceDate(), origin, invoice.getEntryNumber());
                if (vat.signum() > 0) {
                    valueLine(asset, "NON_RECOVERABLE_VAT", vat, invoice.getInvoiceDate(), origin, invoice.getEntryNumber());
                }
                event(asset, "CREATED", invoice.getInvoiceDate(), null, category.getCode(), "Fiche pre-remplie depuis la facture " + invoice.getNumber()
                        + " (" + invoice.getSupplierReference() + ")", asset.getEntryValue(), null, invoice.getEntryNumber());
            }
        }
    }

    /** Valeur rattachee a une fiche existante ; apres la mise en service, le plan est recalcule pour l'avenir. */
    private void addValue(FixedAsset asset, String type, BigDecimal amount, LocalDate date, String origin, String entryNumber, BigDecimal deducted,
                          String revisionReason) {
        valueLine(asset, type, amount, date, origin, entryNumber);
        asset.setEntryValue(asset.getEntryValue().add(amount).max(BigDecimal.ZERO));
        asset.setDeductedVat(asset.getDeductedVat().add(deducted).max(BigDecimal.ZERO));
        asset.setBooked(true);
        if (asset.getEntryNumber() == null) {
            asset.setEntryNumber(entryNumber);
        }
        asset.setUpdatedAt(Instant.now());
        assets.save(asset);
        event(asset, "VALUE_" + type, date, null, plain(asset.getEntryValue()), origin, amount, null, entryNumber);
        if (asset.getPlanVersion() > 0 && !"NONE".equals(method(asset))) {
            regenerate(asset, revisionReason);
        }
    }

    @Override
    public void checkManualEntry(List<AccountingRequests.EntryLine> lines, String label) {
        if ("false".equalsIgnoreCase(settings.text("accounting.fixed-asset.manual-entry-guard", "true"))) {
            return;
        }
        Set<String> tracked = trackedAccounts();
        for (AccountingRequests.EntryLine line : lines) {
            String account = line.accountNumber() == null ? "" : line.accountNumber().trim();
            boolean hit = tracked.stream().anyMatch(account::startsWith);
            if (!hit) {
                continue;
            }
            if (!actors.hasAuthority(REVISE)) {
                throw new ForbiddenException("Le compte " + account + " est tenu par le fichier des immobilisations : passez par la fiche du bien "
                        + "(une saisie directe exige le droit FIXED_ASSET:REVISE et un motif)");
            }
            if (label == null || label.trim().length() < 10) {
                throw new BadRequestException("Une saisie directe sur le compte " + account + " exige un motif explicite dans le libelle");
            }
        }
    }

    /** Comptes tenus par le fichier : biens, amortissements, depreciations des categories. */
    public Set<String> trackedAccounts() {
        Set<String> tracked = new TreeSet<>();
        for (FaCategory category : categories.findAll()) {
            tracked.add(category.getAssetAccount());
            if (category.getDepreciationAccount() != null) tracked.add(category.getDepreciationAccount());
            if (category.getImpairmentAccount() != null) tracked.add(category.getImpairmentAccount());
        }
        return tracked;
    }

    /** Categorie d'un compte de classe 2 : meme compte, sinon le prefixe le plus long. */
    public FaCategory categoryForAccount(String account) {
        if (account == null) {
            return null;
        }
        return categories.findAllByOrderByCodeAsc().stream().filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category -> account.startsWith(category.getAssetAccount()))
                .max(Comparator.comparingInt((FaCategory category) -> category.getAssetAccount().length())
                        .thenComparing(FaCategory::getCode, Comparator.reverseOrder()))
                .orElse(null);
    }

    // ================================================================== editions

    @Transactional
    public byte[] commissionPdf(String code) {
        FixedAsset asset = asset(code);
        FixedAssetEvent event = lastEvent(asset, "COMMISSIONED").orElseThrow(() -> new BadRequestException("Le bien n'est pas mis en service"));
        VoucherService.Issued issued = vouchers.issueAndPrint("PMS", "FIXED_ASSET_COMMISSION", asset.getCode(), asset.getEntryValue(),
                "Mise en service " + asset.getCode(), actor());
        List<VoucherService.Section> sections = new ArrayList<>(identification(asset));
        VoucherService.Section components = new VoucherService.Section("Composants").columns(List.of("Code", "Designation", "Valeur", "Duree (mois)"),
                List.of(false, false, true, true));
        assets.findAllByParentIdOrderByCodeAsc(asset.getId()).forEach(child -> components.row(child.getCode(), child.getDesignation(),
                VoucherService.money(child.getEntryValue()), duration(child)));
        sections.add(new VoucherService.Section("Mise en service").pair("Date", asset.getInServiceDate()).pair("Lieu", location(asset))
                .pair("Detenteur", asset.getHolder()).pair("Observations", event.getReason()));
        sections.add(components);
        return vouchers.render("PV de mise en service", issued, sections, asset.getEntryValue(), null, List.of(
                new VoucherService.Signature("Le receptionnaire", event.getCreatedBy(), VoucherService.date(event.getCreatedAt())),
                new VoucherService.Signature("Le responsable des moyens generaux", null, null)));
    }

    @Transactional
    public byte[] disposalPdf(Long id) {
        FixedAssetDisposal disposal = disposal(id);
        if (disposal.getVoucherNumber() == null) {
            throw new BadRequestException("Le PV de sortie s'imprime apres sa validation");
        }
        FixedAsset asset = assets.findById(disposal.getAssetId()).orElseThrow();
        VoucherService.Issued issued = vouchers.issueAndPrint("PSI", "FIXED_ASSET_DISPOSAL", asset.getCode(), disposal.getPrice(), "Sortie " + asset.getCode(), actor());
        List<VoucherService.Section> sections = new ArrayList<>(identification(asset));
        sections.add(new VoucherService.Section("Sortie").pair("Motif", disposal.getReason()).pair("Date", disposal.getDisposalDate())
                .pair("Valeur brute", VoucherService.money(disposal.getGrossValue())).pair("Cumul des amortissements", VoucherService.money(disposal.getAccumulatedDepreciation()))
                .pair("Depreciations", VoucherService.money(disposal.getAccumulatedImpairment())).pair("Valeur nette", VoucherService.money(disposal.getNetValue()))
                .pair("Prix de cession", VoucherService.money(disposal.getPrice())).pair("Plus ou moins-value", VoucherService.money(disposal.getGainLoss()))
                .pair("Acquereur", disposal.getBuyerName()).pair("Justificatif", disposal.getJustificationRef())
                .pair("TVA reversee", VoucherService.money(disposal.getVatRegularization())));
        return vouchers.render("PV de sortie d'immobilisation", issued, sections, disposal.getPrice(), null, List.of(
                new VoucherService.Signature("Prepare par", disposal.getPreparedBy(), VoucherService.date(disposal.getCreatedAt())),
                new VoucherService.Signature("Valide par", disposal.getValidatedBy(), VoucherService.date(disposal.getValidatedAt()))));
    }

    @Transactional
    public byte[] handoverPdf(String code) {
        FixedAsset asset = asset(code);
        FixedAssetEvent assignment = lastEvent(asset, "ASSIGNMENT").orElseThrow(() -> new BadRequestException("Le bien n'a jamais ete affecte"));
        VoucherService.Issued issued = vouchers.find(assignment.getVoucherNumber()).orElseThrow();
        Optional<FixedAssetEvent> returned = events.findAllByAssetIdOrderByCreatedAtAscIdAsc(asset.getId()).stream()
                .filter(item -> "RETURN".equals(item.getKind()) && Objects.equals(item.getVoucherNumber(), assignment.getVoucherNumber())).findFirst();
        List<VoucherService.Section> sections = new ArrayList<>(identification(asset));
        sections.add(new VoucherService.Section("Remise").pair("Employe", assignment.getAfterValue()).pair("Date", assignment.getEventDate())
                .pair("Etat et accessoires", assignment.getReason())
                .pair("Engagement", "L'employe s'engage a restituer le bien et ses accessoires a la demande et a son depart."));
        sections.add(new VoucherService.Section("Restitution").pair("Date", returned.map(FixedAssetEvent::getEventDate).orElse(null))
                .pair("Etat et accessoires", returned.map(FixedAssetEvent::getReason).orElse(null)));
        return vouchers.render("Fiche de remise de materiel", issued, sections, null, null, List.of(
                new VoucherService.Signature("L'employe", assignment.getAfterValue(), null),
                new VoucherService.Signature("Le remettant", assignment.getCreatedBy(), VoucherService.date(assignment.getCreatedAt()))));
    }

    List<VoucherService.Section> identification(FixedAsset asset) {
        FaCategory category = categories.findById(asset.getCategoryCode()).orElse(null);
        return List.of(new VoucherService.Section("Bien").pair("Code", asset.getCode()).pair("Designation", asset.getDesignation())
                .pair("Categorie", category == null ? asset.getCategoryCode() : category.getLabel()).pair("Marque et modele", nz(asset.getBrand()) + " " + nz(asset.getModel()))
                .pair("Numero de serie", asset.getSerialNumber()).pair("Quantite", asset.getQuantity())
                .pair("Fournisseur", asset.getSupplierName()).pair("Facture", asset.getSupplierInvoiceNumber())
                .pair("Valeur d'entree", VoucherService.money(asset.getEntryValue())).pair("Acquisition", asset.getAcquisitionDate()));
    }

    // ================================================================== outils

    public FixedAsset asset(String code) {
        return assets.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Immobilisation introuvable : " + code));
    }

    private FixedAsset live(String code) {
        FixedAsset asset = asset(code);
        if (!LIVE.contains(asset.getAccountingStatus())) {
            throw new BadRequestException("La fiche " + code + " est " + ("DISPOSED".equals(asset.getAccountingStatus()) ? "sortie" : "annulee"));
        }
        return asset;
    }

    public FaCategory category(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Categorie obligatoire");
        }
        return categories.findById(code.trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new BadRequestException("Categorie inconnue : " + code));
    }

    private FixedAssetDisposal disposal(Long id) {
        return disposals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sortie introuvable : " + id));
    }

    public List<FixedAssetSchedule> current(FixedAsset asset, String kind) {
        if (asset.getPlanVersion() == null || asset.getPlanVersion() == 0) {
            return List.of();
        }
        return schedules.findAllByAssetIdAndKindAndVersionOrderByPeriodCodeAsc(asset.getId(), kind, asset.getPlanVersion());
    }

    private boolean hasPosted(FixedAsset asset) {
        return current(asset, ACCOUNTING).stream().anyMatch(row -> DONE.contains(row.getStatus()));
    }

    public BigDecimal netValue(FixedAsset asset) {
        return asset.getEntryValue().subtract(asset.getAccumulatedDepreciation()).subtract(asset.getAccumulatedImpairment());
    }

    private static BigDecimal amount(FixedAssetSchedule row) {
        return DONE.contains(row.getStatus()) ? row.getPosted() : row.getPlanned();
    }

    public String method(FixedAsset asset) {
        if (asset.getMethod() != null) {
            return asset.getMethod();
        }
        Optional<FaDepreciationProfile> profile = profile(asset);
        if (profile.isPresent() && profile.get().getMethod() != null) {
            return profile.get().getMethod();
        }
        return categories.findById(asset.getCategoryCode()).map(FaCategory::getMethod).orElse("NONE");
    }

    /**
     * Profil de marque applicable a un bien : meme marque, categorie et modele compatibles ; le plus
     * precis gagne (categorie et modele renseignes), puis la priorite la plus haute.
     */
    public Optional<FaDepreciationProfile> profile(FixedAsset asset) {
        if (asset == null || !StringUtils.hasText(asset.getBrand())) {
            return Optional.empty();
        }
        String model = asset.getModel() == null ? "" : asset.getModel().toUpperCase(Locale.ROOT);
        return profiles.findAllByActiveTrueAndBrandIgnoreCase(asset.getBrand().trim()).stream()
                .filter(profile -> profile.getCategoryCode() == null || profile.getCategoryCode().equalsIgnoreCase(asset.getCategoryCode()))
                .filter(profile -> !StringUtils.hasText(profile.getModelPattern()) || model.contains(profile.getModelPattern().trim().toUpperCase(Locale.ROOT)))
                .max(Comparator.<FaDepreciationProfile>comparingInt(profile -> (profile.getCategoryCode() != null ? 2 : 0)
                                + (StringUtils.hasText(profile.getModelPattern()) ? 1 : 0))
                        .thenComparing(profile -> profile.getPriority() == null ? 0 : profile.getPriority())
                        .thenComparing(FaDepreciationProfile::getId, Comparator.reverseOrder()));
    }

    public int duration(FixedAsset asset) {
        if (asset.getDurationMonths() != null) {
            return asset.getDurationMonths();
        }
        Optional<FaDepreciationProfile> profile = profile(asset);
        if (profile.isPresent() && profile.get().getDurationMonths() != null) {
            return profile.get().getDurationMonths();
        }
        if (profile.isPresent() && profile.get().getAnnualRate() != null) {
            return DepreciationCalculator.monthsForRate(profile.get().getAnnualRate());
        }
        FaCategory category = categories.findById(asset.getCategoryCode()).orElse(null);
        if (category != null && category.getDurationMonths() != null) {
            return category.getDurationMonths();
        }
        if (category != null && category.getAnnualRate() != null) {
            return DepreciationCalculator.monthsForRate(category.getAnnualRate());
        }
        return 12;
    }

    private String taxMethod(FixedAsset asset, FaCategory category) {
        return asset.getTaxMethod() != null ? asset.getTaxMethod() : category.getTaxMethod();
    }

    private int taxDuration(FixedAsset asset, FaCategory category) {
        if (asset.getTaxDurationMonths() != null) return asset.getTaxDurationMonths();
        if (category.getTaxDurationMonths() != null) return category.getTaxDurationMonths();
        return duration(asset);
    }

    private BigDecimal coefficient(FixedAsset asset, FaCategory category, int duration) {
        BigDecimal value = asset.getCoefficient();
        if (value == null) {
            value = profile(asset).map(FaDepreciationProfile::getCoefficient).orElse(null);
        }
        if (value == null) {
            value = category.getCoefficient();
        }
        if (value == null) {
            value = DepreciationCalculator.coefficient(duration, settings.text("accounting.fixed-asset.declining-coefficients", "3-4:1.5,5-6:2,7+:2.5"));
        }
        if (value == null) {
            throw new BadRequestException("Aucun coefficient degressif pour une duree de " + duration + " mois");
        }
        return value;
    }

    private String describePlan(FixedAsset asset) {
        return method(asset) + " " + duration(asset) + " mois, residuelle " + plain(asset.getResidualValue())
                + (asset.getTaxMethod() == null ? "" : ", fiscal " + asset.getTaxMethod() + " " + asset.getTaxDurationMonths());
    }

    Optional<FixedAssetEvent> lastEvent(FixedAsset asset, String kind) {
        List<FixedAssetEvent> list = events.findAllByAssetIdOrderByCreatedAtAscIdAsc(asset.getId()).stream().filter(item -> kind.equals(item.getKind())).toList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    void event(FixedAsset asset, String kind, LocalDate date, String before, String after, String reason, BigDecimal amount, String voucher, String entry) {
        events.save(FixedAssetEvent.builder().assetId(asset.getId()).kind(kind).eventDate(date == null ? LocalDate.now() : date).beforeValue(cut(before, 500))
                .afterValue(cut(after, 500)).reason(cut(reason, 500)).amount(amount).voucherNumber(voucher).entryNumber(entry).createdBy(actor()).build());
    }

    private void valueLine(FixedAsset asset, String type, BigDecimal amount, LocalDate date, String origin, String entryNumber) {
        valueLines.save(FixedAssetValueLine.builder().assetId(asset.getId()).lineType(type).amount(amount).valueDate(date == null ? LocalDate.now() : date)
                .origin(cut(origin, 120)).entryNumber(entryNumber).createdBy(actor()).build());
    }

    /** Code {@code IMM-AAAA-00001} : sequence propre, sans exigence de continuite, jamais reutilisee. */
    String nextCode(int year) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO fixed_asset_counter (year, last_number) VALUES (?, 0) ON CONFLICT DO NOTHING", year);
        Integer last = jdbc.queryForObject("SELECT last_number FROM fixed_asset_counter WHERE year = ? FOR UPDATE", Integer.class, year);
        int next = (last == null ? 0 : last) + 1;
        jdbc.update("UPDATE fixed_asset_counter SET last_number = ? WHERE year = ?", next, year);
        return String.format("IMM-%d-%05d", year, next);
    }

    /**
     * Piece des immobilisations ; datee d'une periode verrouillee ou close, elle passe dans la
     * premiere periode ouverte suivante, avec mention de la date du fait.
     */
    AccEntry book(String journal, LocalDate date, String label, String sourceType, String sourceCode, List<AccountingRequests.EntryLine> lines) {
        LocalDate effective = date;
        LocalDate original = null;
        Optional<AccPeriod> period = periodRepository.findCovering(date);
        if (period.isPresent() && period.get().getStatus() != PeriodStatus.OPEN) {
            effective = periods.firstOpenOnOrAfter(date).getStartDate();
            original = date;
        }
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(journal, effective, original, label, EntryOrigin.PLATFORM, sourceType,
                cut(sourceCode, 120), lines, actor(), true, null));
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        return entry;
    }

    AccountingRequests.EntryLine line(String account, String thirdParty, String label, BigDecimal debit, BigDecimal credit, String reference,
                                      Map<String, String> analytic) {
        return new AccountingRequests.EntryLine(account, thirdParty, cut(label, 250), debit, credit, null, null, null, null, null, null, reference,
                analytic == null || analytic.isEmpty() ? null : analytic);
    }

    /** Tiers d'une contrepartie : un compte de tiers (40, 41, 42, 46, 47, 48) porte le fournisseur de la fiche. */
    private String thirdPartyFor(String counter, String supplierCode) {
        boolean thirdPartyAccount = accounts.findByNumber(counter).map(account -> account.getThirdPartyType() != null).orElse(false);
        if (!thirdPartyAccount) {
            return null;
        }
        if (!StringUtils.hasText(supplierCode)) {
            throw new BadRequestException("Le compte " + counter + " est un compte de tiers : indiquez le fournisseur (supplierCode)");
        }
        return supplierCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Sections analytiques d'un bien : celles de la fiche, sinon la section de sa ressource sur l'axe des ressources. */
    public Map<String, String> analytic(FixedAsset asset) {
        Map<String, String> result = new LinkedHashMap<>(decode(asset.getAnalyticSections()));
        String axis = settings.text("accounting.fixed-asset.resource-axis", "RESSOURCE");
        if (StringUtils.hasText(axis) && asset.getResourceCode() != null && !result.containsKey(axis)
                && sections.findByAxisCodeAndCode(axis, asset.getResourceCode()).isPresent()) {
            result.put(axis, asset.getResourceCode());
        }
        return result;
    }

    public static Map<String, String> decode(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(value)) {
            return result;
        }
        for (String pair : value.split(";")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1])) {
                result.put(parts[0].trim(), parts[1].trim());
            }
        }
        return result;
    }

    static String encode(Map<String, String> analytic) {
        if (analytic == null || analytic.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(";");
        new TreeMap<>(analytic).forEach((axis, section) -> {
            if (StringUtils.hasText(axis) && StringUtils.hasText(section)) {
                joiner.add(axis.trim().toUpperCase(Locale.ROOT) + "=" + section.trim());
            }
        });
        return cut(joiner.toString(), 500);
    }

    static String location(FixedAsset asset) {
        StringJoiner joiner = new StringJoiner(" / ");
        if (asset.getSite() != null) joiner.add(asset.getSite());
        if (asset.getResourceCode() != null) joiner.add(asset.getResourceCode());
        if (asset.getLocationLabel() != null) joiner.add(asset.getLocationLabel());
        return joiner.toString();
    }

    private String resourceCode(String code) {
        String value = blankToNull(code);
        if (value == null) {
            return null;
        }
        Optional<ResourceRepository> repository = resources();
        if (repository.isPresent() && repository.get().findByCode(value).isEmpty()) {
            throw new BadRequestException("Ressource inconnue : " + value);
        }
        return value;
    }

    private Optional<ResourceRepository> resources() {
        return Optional.ofNullable(resources.getIfAvailable());
    }

    private String account(String number, String prefix, String what) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException(what + " obligatoire");
        }
        String value = number.trim();
        if (prefix != null && !value.startsWith(prefix)) {
            throw new BadRequestException(what + " : le compte " + value + " ne commence pas par " + prefix);
        }
        accounts.findByNumber(value).orElseThrow(() -> new BadRequestException(what + " : compte inconnu " + value));
        return value;
    }

    private String optionalAccount(String number, String prefix, String what) {
        return StringUtils.hasText(number) ? account(number, prefix, what) : null;
    }

    String journal() {
        return settings.text("accounting.fixed-asset.journal", "OD");
    }

    boolean days360() {
        return !"365".equals(settings.text("accounting.fixed-asset.day-basis", "360").trim());
    }

    private int vatYears() {
        return (int) number("accounting.fixed-asset.vat-regularization-years", 5);
    }

    long number(String key, long fallback) {
        try {
            return Long.parseLong(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    static YearMonth month(String period) {
        try {
            return YearMonth.parse(period.trim());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Periode au format AAAA-MM attendue : " + period);
        }
    }

    void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Action reservee : " + authority);
        }
    }

    String actor() {
        return actors.resolve("SYSTEM");
    }

    static String upper(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String nz(String value) {
        return value == null ? "" : value;
    }

    static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
