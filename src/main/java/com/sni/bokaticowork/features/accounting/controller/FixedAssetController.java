package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.FaCategory;
import com.sni.bokaticowork.features.accounting.model.FixedAsset;
import com.sni.bokaticowork.features.accounting.model.FixedAssetDisposal;
import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryCampaign;
import com.sni.bokaticowork.features.accounting.service.implementation.DepreciationRunService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetInventoryService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Immobilisations et amortissements (lot F18). Les droits sont controles par les services. */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService assets;
    private final DepreciationRunService runs;
    private final FixedAssetReportService reports;
    private final FixedAssetInventoryService inventories;
    private final com.sni.bokaticowork.features.accounting.service.implementation.DepreciationProfileService depreciationProfiles;

    public record CodesRequest(List<String> codes) {
    }

    public record InventoryLinkRequest(List<String> inventoryCodes, Boolean unlink) {
    }

    public record MeterRequest(String period, BigDecimal units) {
    }

    public record DecisionRequest(Boolean approve) {
    }

    // ------------------------------------------------------------------ categories

    @GetMapping("/categories")
    public ResponseEntity<List<FaCategory>> categories() {
        return ResponseEntity.ok(assets.categories());
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_CATEGORY_SAVE", ressource = "fa_category")
    @PostMapping("/categories")
    public ResponseEntity<FaCategory> saveCategory(@RequestBody FixedAssetService.CategoryRequest request) {
        return ResponseEntity.ok(assets.saveCategory(request));
    }

    // ------------------------------------------------------------------ profils par marque

    @GetMapping("/depreciation-profiles")
    public ResponseEntity<List<com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile>> profiles() {
        return ResponseEntity.ok(depreciationProfiles.list());
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_PROFILE_CREATE", ressource = "fa_depreciation_profile")
    @PostMapping("/depreciation-profiles")
    public ResponseEntity<com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile> createProfile(
            @RequestBody com.sni.bokaticowork.features.accounting.service.implementation.DepreciationProfileService.ProfileRequest request) {
        return ResponseEntity.ok(depreciationProfiles.create(request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_PROFILE_UPDATE", ressource = "fa_depreciation_profile")
    @PatchMapping("/depreciation-profiles/{id}")
    public ResponseEntity<com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile> updateProfile(@PathVariable Long id,
            @RequestBody com.sni.bokaticowork.features.accounting.service.implementation.DepreciationProfileService.ProfileRequest request) {
        return ResponseEntity.ok(depreciationProfiles.update(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_PROFILE_DEACTIVATE", ressource = "fa_depreciation_profile")
    @PostMapping("/depreciation-profiles/{id}/deactivate")
    public ResponseEntity<com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile> deactivateProfile(@PathVariable Long id) {
        return ResponseEntity.ok(depreciationProfiles.deactivate(id));
    }

    @GetMapping("/{code}/depreciation-profile")
    public ResponseEntity<com.sni.bokaticowork.features.accounting.service.implementation.DepreciationProfileService.Resolution> resolvedProfile(
            @PathVariable String code) {
        return ResponseEntity.ok(depreciationProfiles.resolve(code));
    }

    // ------------------------------------------------------------------ fichier et fiches

    @GetMapping
    public ResponseEntity<List<FixedAsset>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String site, @RequestParam(required = false) String resource,
                                                 @RequestParam(required = false) String holder) {
        return ResponseEntity.ok(reports.list(status, category, site, resource, holder));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_CREATE", ressource = "fixed_asset")
    @PostMapping
    public ResponseEntity<FixedAssetService.AssetView> create(@RequestBody FixedAssetService.AssetRequest request) {
        return ResponseEntity.ok(assets.create(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<FixedAssetService.AssetView> get(@PathVariable String code) {
        return ResponseEntity.ok(assets.view(code));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_UPDATE", ressource = "fixed_asset")
    @PatchMapping("/{code}")
    public ResponseEntity<FixedAssetService.AssetView> update(@PathVariable String code, @RequestBody FixedAssetService.AssetRequest request) {
        return ResponseEntity.ok(assets.update(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_COMPONENTS", ressource = "fixed_asset")
    @PostMapping("/{code}/components")
    public ResponseEntity<FixedAssetService.AssetView> components(@PathVariable String code,
                                                                  @RequestBody List<FixedAssetService.ComponentInput> components) {
        return ResponseEntity.ok(assets.components(code, components));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_COMMISSION", ressource = "fixed_asset")
    @PostMapping("/{code}/commission")
    public ResponseEntity<FixedAssetService.AssetView> commission(@PathVariable String code, @RequestBody FixedAssetService.CommissionRequest request) {
        return ResponseEntity.ok(assets.commission(code, request));
    }

    @GetMapping("/{code}/commission.pdf")
    public ResponseEntity<byte[]> commissionPdf(@PathVariable String code) {
        return pdf(assets.commissionPdf(code), "pv-mise-en-service-" + code);
    }

    @GetMapping("/{code}/plan")
    public ResponseEntity<FixedAssetService.PlanView> plan(@PathVariable String code, @RequestParam(required = false) String kind) {
        return ResponseEntity.ok(assets.plan(code, kind));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_REVISE", ressource = "fixed_asset")
    @PostMapping("/{code}/revisions")
    public ResponseEntity<FixedAssetService.AssetView> revise(@PathVariable String code, @RequestBody FixedAssetService.RevisionRequest request) {
        return ResponseEntity.ok(assets.revise(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_RECLASS", ressource = "fixed_asset")
    @PostMapping("/{code}/reclassifications")
    public ResponseEntity<FixedAssetService.AssetView> reclassify(@PathVariable String code, @RequestBody FixedAssetService.ReclassRequest request) {
        return ResponseEntity.ok(assets.reclassify(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_TRANSFER", ressource = "fixed_asset")
    @PostMapping("/{code}/transfers")
    public ResponseEntity<FixedAssetService.AssetView> transfer(@PathVariable String code, @RequestBody FixedAssetService.TransferRequest request) {
        return ResponseEntity.ok(assets.transfer(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_ASSIGN", ressource = "fixed_asset")
    @PostMapping("/{code}/assignments")
    public ResponseEntity<FixedAssetService.AssetView> assign(@PathVariable String code, @RequestBody FixedAssetService.AssignmentRequest request) {
        return ResponseEntity.ok(assets.assign(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_RETURN", ressource = "fixed_asset")
    @PostMapping("/{code}/returns")
    public ResponseEntity<FixedAssetService.AssetView> returnAsset(@PathVariable String code, @RequestBody FixedAssetService.AssignmentRequest request) {
        return ResponseEntity.ok(assets.returnAsset(code, request));
    }

    @GetMapping("/{code}/handover.pdf")
    public ResponseEntity<byte[]> handoverPdf(@PathVariable String code) {
        return pdf(assets.handoverPdf(code), "fiche-remise-" + code);
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_MAINTENANCE", ressource = "fixed_asset")
    @PostMapping("/{code}/maintenance")
    public ResponseEntity<FixedAssetService.AssetView> maintenance(@PathVariable String code, @RequestBody FixedAssetService.MaintenanceRequest request) {
        return ResponseEntity.ok(assets.maintenance(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_OPERATIONAL", ressource = "fixed_asset")
    @PostMapping("/{code}/operational-status")
    public ResponseEntity<FixedAssetService.AssetView> operational(@PathVariable String code, @RequestBody FixedAssetService.OperationalRequest request) {
        return ResponseEntity.ok(assets.operational(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_METER", ressource = "fixed_asset")
    @PostMapping("/{code}/meter-readings")
    public ResponseEntity<FixedAssetService.AssetView> meterReading(@PathVariable String code, @RequestBody MeterRequest request) {
        return ResponseEntity.ok(assets.meterReading(code, request.period(), request.units()));
    }

    public record LeaseRequest(String loanCode) {
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_LEASE_LINK", ressource = "fixed_asset")
    @PostMapping("/{code}/lease")
    public ResponseEntity<FixedAssetService.AssetView> lease(@PathVariable String code, @RequestBody LeaseRequest request) {
        return ResponseEntity.ok(assets.linkLease(code, request.loanCode()));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_INVENTORY_LINK", ressource = "fixed_asset")
    @PostMapping("/{code}/inventory-links")
    public ResponseEntity<FixedAssetService.AssetView> linkInventory(@PathVariable String code, @RequestBody InventoryLinkRequest request) {
        return ResponseEntity.ok(assets.linkInventory(code, request.inventoryCodes(), Boolean.TRUE.equals(request.unlink())));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_VAT_REGULARIZATION", ressource = "fixed_asset")
    @PostMapping("/{code}/vat-regularizations")
    public ResponseEntity<FixedAssetService.AssetView> regularizeVat(@PathVariable String code,
                                                                     @RequestBody FixedAssetService.VatRegularizationRequest request) {
        return ResponseEntity.ok(assets.regularizeVat(code, request));
    }

    // ------------------------------------------------------------------ depreciations

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_IMPAIR", ressource = "fixed_asset")
    @PostMapping("/{code}/impairments")
    public ResponseEntity<FixedAssetService.AssetView> impair(@PathVariable String code, @RequestBody FixedAssetService.ImpairmentRequest request) {
        return ResponseEntity.ok(assets.impair(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_IMPAIR_DECIDE", ressource = "fixed_asset_impairment")
    @PostMapping("/impairments/{id}/decision")
    public ResponseEntity<FixedAssetService.AssetView> decideImpairment(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(assets.decideImpairment(id, Boolean.TRUE.equals(request.approve())));
    }

    // ------------------------------------------------------------------ sorties

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DISPOSAL_PREPARE", ressource = "fixed_asset_disposal")
    @PostMapping("/{code}/disposals")
    public ResponseEntity<FixedAssetDisposal> prepareDisposal(@PathVariable String code, @RequestBody FixedAssetService.DisposalRequest request) {
        return ResponseEntity.ok(assets.prepareDisposal(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DISPOSAL_VALIDATE", ressource = "fixed_asset_disposal")
    @PostMapping("/disposals/{id}/validate")
    public ResponseEntity<FixedAssetDisposal> validateDisposal(@PathVariable Long id) {
        return ResponseEntity.ok(assets.validateDisposal(id));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DISPOSAL_CANCEL", ressource = "fixed_asset_disposal")
    @PostMapping("/disposals/{id}/cancel")
    public ResponseEntity<FixedAssetDisposal> cancelDisposal(@PathVariable Long id) {
        return ResponseEntity.ok(assets.cancelDisposal(id));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DISPOSAL_COLLECT", ressource = "fixed_asset_disposal")
    @PostMapping("/disposals/{id}/collections")
    public ResponseEntity<FixedAssetDisposal> collect(@PathVariable Long id, @RequestBody FixedAssetService.CollectRequest request) {
        return ResponseEntity.ok(assets.collect(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DISPOSAL_INDEMNITY", ressource = "fixed_asset_disposal")
    @PostMapping("/disposals/{id}/indemnities")
    public ResponseEntity<FixedAssetDisposal> indemnity(@PathVariable Long id, @RequestBody FixedAssetService.IndemnityRequest request) {
        return ResponseEntity.ok(assets.indemnity(id, request));
    }

    @GetMapping("/disposals/{id}/pdf")
    public ResponseEntity<byte[]> disposalPdf(@PathVariable Long id) {
        return pdf(assets.disposalPdf(id), "pv-sortie-" + id);
    }

    // ------------------------------------------------------------------ impressions

    @GetMapping("/{code}/sheet.pdf")
    public ResponseEntity<byte[]> sheet(@PathVariable String code) {
        return pdf(reports.sheetPdf(code), "fiche-" + code);
    }

    @GetMapping("/{code}/label.pdf")
    public ResponseEntity<byte[]> label(@PathVariable String code) {
        return pdf(inventories.labels(List.of(code)), "etiquette-" + code);
    }

    @PostMapping("/labels")
    public ResponseEntity<byte[]> labels(@RequestBody CodesRequest request) {
        return pdf(inventories.labels(request.codes()), "etiquettes");
    }

    // ------------------------------------------------------------------ dotations

    @GetMapping("/depreciation-runs/preview")
    public ResponseEntity<DepreciationRunService.RunResult> preview(@RequestParam String period) {
        return ResponseEntity.ok(runs.preview(period));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_DEPRECIATION_RUN", ressource = "fixed_asset_schedule")
    @PostMapping("/depreciation-runs")
    public ResponseEntity<DepreciationRunService.RunResult> run(@RequestParam String period) {
        return ResponseEntity.ok(runs.run(period));
    }

    // ------------------------------------------------------------------ inventaire physique

    @GetMapping("/inventories")
    public ResponseEntity<List<FixedAssetInventoryCampaign>> inventories() {
        return ResponseEntity.ok(inventories.list());
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_INVENTORY_OPEN", ressource = "fixed_asset_inventory_campaign")
    @PostMapping("/inventories")
    public ResponseEntity<FixedAssetInventoryService.CampaignView> openInventory(@RequestBody FixedAssetInventoryService.CampaignRequest request) {
        return ResponseEntity.ok(inventories.open(request));
    }

    @GetMapping("/inventories/{code}")
    public ResponseEntity<FixedAssetInventoryService.CampaignView> inventory(@PathVariable String code) {
        return ResponseEntity.ok(inventories.get(code));
    }

    @PostMapping("/inventories/{code}/scans")
    public ResponseEntity<FixedAssetInventoryService.CampaignView> scan(@PathVariable String code, @RequestBody FixedAssetInventoryService.ScanRequest request) {
        return ResponseEntity.ok(inventories.scan(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "FIXED_ASSET_INVENTORY_CLOSE", ressource = "fixed_asset_inventory_campaign")
    @PostMapping("/inventories/{code}/close")
    public ResponseEntity<FixedAssetInventoryService.CampaignView> closeInventory(@PathVariable String code,
                                                                                  @RequestBody(required = false) FixedAssetInventoryService.CloseRequest request) {
        return ResponseEntity.ok(inventories.close(code, request));
    }

    @GetMapping("/inventories/{code}/pdf")
    public ResponseEntity<byte[]> inventoryPdf(@PathVariable String code) {
        return pdf(inventories.reportPdf(code), "pv-inventaire-" + code);
    }

    // ------------------------------------------------------------------ etats et controles

    /** Etats de la section 11 : {@code format} json (defaut), csv ou pdf. */
    @GetMapping("/reports/{report}")
    public ResponseEntity<?> report(@PathVariable String report, @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) String category, @RequestParam(required = false) String site,
                                    @RequestParam(required = false) String resource, @RequestParam(required = false) String holder,
                                    @RequestParam(required = false, defaultValue = "json") String format) {
        FixedAssetReportService.Table table = reports.report(report, year, date, category, site, resource, holder);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "csv" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"immobilisations-" + table.code() + ".csv\"")
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(reports.csv(table));
            case "pdf" -> pdf(reports.pdf(table), "immobilisations-" + table.code());
            default -> ResponseEntity.ok(table);
        };
    }

    @GetMapping("/controls")
    public ResponseEntity<List<FixedAssetReportService.Finding>> controls(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reports.controls(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/closing-check")
    public ResponseEntity<List<FixedAssetReportService.Finding>> closingCheck(@RequestParam int year) {
        return ResponseEntity.ok(reports.closingCheck(year));
    }

    private static ResponseEntity<byte[]> pdf(byte[] content, String name) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(content);
    }
}
