package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.accounting.model.FixedAsset;
import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryCampaign;
import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryScan;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetInventoryCampaignRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetInventoryScanRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetRepository;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inventaire physique des immobilisations (lot F18, section 9) : campagne sur un perimetre, scan
 * des QR par l'equipe, ecarts a la cloture et PV d'inventaire ({@code PIP-}). Etiquettes QR.
 */
@Service
@RequiredArgsConstructor
public class FixedAssetInventoryService {

    public static final String INVENTORY = "FIXED_ASSET:INVENTORY";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final FixedAssetService fixedAssets;
    private final FixedAssetRepository assets;
    private final FixedAssetInventoryCampaignRepository campaigns;
    private final FixedAssetInventoryScanRepository scans;
    private final VoucherService vouchers;
    private final QrCodeRenderer qrCodes;

    public record CampaignRequest(String label, String site, List<String> resourceCodes, List<String> categoryCodes, List<String> team,
                                  LocalDate startDate, String witness) {
    }

    public record ScanRequest(String assetCode, String unregisteredLabel, String site, String resourceCode, String locationLabel,
                              String photoRef) {
    }

    public record CloseRequest(String decisions, String witness) {
    }

    public record CampaignView(FixedAssetInventoryCampaign campaign, List<FixedAssetInventoryScan> scans, List<String> expected,
                               List<String> seen, List<String> missing, List<String> mislocated, List<String> warnings) {
    }

    // ================================================================== campagnes

    @Transactional
    public CampaignView open(CampaignRequest request) {
        fixedAssets.require(INVENTORY);
        if (request == null || !StringUtils.hasText(request.label())) {
            throw new BadRequestException("Une campagne d'inventaire a un libelle");
        }
        Set<String> team = clean(request.team());
        if (team.isEmpty()) {
            throw new BadRequestException("Une campagne d'inventaire a une equipe (courriels des membres)");
        }
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now(ZONE);
        String code = "INV-" + start.getYear() + "-" + String.format("%03d", campaigns.count() + 1);
        FixedAssetInventoryCampaign campaign = FixedAssetInventoryCampaign.builder().code(code).label(request.label().trim())
                .site(blankToNull(request.site())).resourceCodes(join(clean(request.resourceCodes())))
                .categoryCodes(join(clean(request.categoryCodes()))).team(join(team)).startDate(start)
                .witness(blankToNull(request.witness())).openedBy(fixedAssets.actor()).build();
        campaign.setExpectedCount(expected(campaign).size());
        return view(campaigns.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<FixedAssetInventoryCampaign> list() {
        fixedAssets.require(FixedAssetService.READ);
        return campaigns.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public CampaignView get(String code) {
        fixedAssets.require(FixedAssetService.READ);
        return view(campaign(code));
    }

    /** Un QR scanne marque le bien vu ; un bien sans etiquette se declare avec un libelle. */
    @Transactional
    public CampaignView scan(String code, ScanRequest request) {
        FixedAssetInventoryCampaign campaign = campaign(code);
        if (!"OPEN".equals(campaign.getStatus())) {
            throw new BadRequestException("La campagne " + campaign.getCode() + " est close");
        }
        String actor = fixedAssets.actor();
        boolean member = members(campaign).contains(actor.toLowerCase(Locale.ROOT));
        if (!member && !hasInventoryRight()) {
            throw new ForbiddenException("Seuls les membres de l'equipe de la campagne peuvent scanner");
        }
        if (request == null || !StringUtils.hasText(request.assetCode()) && !StringUtils.hasText(request.unregisteredLabel())) {
            throw new BadRequestException("Un scan donne le code du bien, ou le libelle d'un bien non inscrit");
        }
        FixedAssetInventoryScan.FixedAssetInventoryScanBuilder scan = FixedAssetInventoryScan.builder().campaignId(campaign.getId())
                .site(blankToNull(request.site())).resourceCode(blankToNull(request.resourceCode()))
                .locationLabel(blankToNull(request.locationLabel())).photoRef(blankToNull(request.photoRef())).scannedBy(actor);
        if (StringUtils.hasText(request.assetCode())) {
            FixedAsset asset = fixedAssets.asset(request.assetCode());
            boolean mislocated = (StringUtils.hasText(request.site()) && !Objects.equals(upper(request.site()), upper(asset.getSite())))
                    || (StringUtils.hasText(request.resourceCode()) && !Objects.equals(upper(request.resourceCode()), upper(asset.getResourceCode())));
            // Separation des taches : le detenteur ne devrait pas scanner son propre bien (alerte, pas blocage).
            boolean holder = StringUtils.hasText(asset.getHolder()) && asset.getHolder().equalsIgnoreCase(actor);
            scan.assetId(asset.getId()).assetCode(asset.getCode()).mislocated(mislocated).holderAlert(holder);
        } else {
            scan.unregisteredLabel(request.unregisteredLabel().trim());
        }
        scans.save(scan.build());
        return view(refreshCounts(campaign));
    }

    /** Cloture : ecarts figes et PV d'inventaire physique. */
    @Transactional
    public CampaignView close(String code, CloseRequest request) {
        fixedAssets.require(INVENTORY);
        FixedAssetInventoryCampaign campaign = campaign(code);
        if (!"OPEN".equals(campaign.getStatus())) {
            throw new BadRequestException("La campagne " + campaign.getCode() + " est deja close");
        }
        CampaignView view = view(refreshCounts(campaign));
        campaign.setMissingCodes(join(new LinkedHashSet<>(view.missing())));
        campaign.setMislocatedCodes(join(new LinkedHashSet<>(view.mislocated())));
        campaign.setDecisions(request == null ? null : blankToNull(request.decisions()));
        if (request != null && StringUtils.hasText(request.witness())) {
            campaign.setWitness(request.witness().trim());
        }
        if (!view.missing().isEmpty() && !StringUtils.hasText(campaign.getDecisions())) {
            throw new BadRequestException(view.missing().size() + " bien(s) non vu(s) : indiquez les decisions (recherche, sortie pour perte...)");
        }
        campaign.setStatus("CLOSED");
        campaign.setEndDate(LocalDate.now(ZONE));
        campaign.setClosedBy(fixedAssets.actor());
        campaign.setClosedAt(Instant.now());
        VoucherService.Issued issued = vouchers.issueAndPrint("PIP", "FIXED_ASSET_INVENTORY", campaign.getCode(), null,
                "Inventaire physique " + campaign.getCode(), fixedAssets.actor());
        campaign.setVoucherNumber(issued.number());
        return view(campaigns.save(campaign));
    }

    @Transactional
    public byte[] reportPdf(String code) {
        fixedAssets.require(FixedAssetService.READ);
        FixedAssetInventoryCampaign campaign = campaign(code);
        if (!StringUtils.hasText(campaign.getVoucherNumber())) {
            throw new BadRequestException("Le PV s'imprime apres la cloture de la campagne");
        }
        CampaignView view = view(campaign);
        VoucherService.Issued issued = vouchers.issueAndPrint("PIP", "FIXED_ASSET_INVENTORY", campaign.getCode(), null,
                "Inventaire physique " + campaign.getCode(), fixedAssets.actor());
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Campagne").pair("Code", campaign.getCode()).pair("Libellé", campaign.getLabel())
                .pair("Site", campaign.getSite()).pair("Ressources", campaign.getResourceCodes()).pair("Catégories", campaign.getCategoryCodes())
                .pair("Équipe", campaign.getTeam()).pair("Du", campaign.getStartDate()).pair("Au", campaign.getEndDate()));
        sections.add(new VoucherService.Section("Résultat").pair("Biens attendus", campaign.getExpectedCount()).pair("Biens vus", campaign.getSeenCount())
                .pair("Biens manquants", campaign.getMissingCount()).pair("Biens trouvés non inscrits", campaign.getFoundCount())
                .pair("Biens mal localisés", campaign.getMislocatedCount()));
        VoucherService.Section gaps = new VoucherService.Section("Écarts").columns(List.of("Nature", "Bien"), List.of(false, false));
        view.missing().forEach(asset -> gaps.row("Non vu", asset));
        view.mislocated().forEach(asset -> gaps.row("Mal localisé", asset));
        view.scans().stream().filter(scan -> scan.getAssetId() == null).forEach(scan -> gaps.row("Non inscrit", scan.getUnregisteredLabel()));
        sections.add(gaps);
        sections.add(new VoucherService.Section("Décisions").pair("Décisions", campaign.getDecisions()));
        return vouchers.render("PV d'inventaire physique", issued, sections, null, null, List.of(
                new VoucherService.Signature("Le responsable", campaign.getClosedBy(), campaign.getClosedAt() == null ? null : VoucherService.date(campaign.getClosedAt())),
                new VoucherService.Signature("Le témoin", campaign.getWitness(), null)));
    }

    // ================================================================== etiquettes

    /** Planche d'etiquettes A4 (38 x 21 mm, 65 par planche) : code, designation courte, QR. */
    @Transactional
    public byte[] labels(List<String> codes) {
        fixedAssets.require(FixedAssetService.MANAGE);
        if (codes == null || codes.isEmpty()) {
            throw new BadRequestException("Choisissez au moins un bien");
        }
        StringBuilder cells = new StringBuilder();
        for (String code : new LinkedHashSet<>(codes)) {
            FixedAsset asset = fixedAssets.asset(code);
            asset.setLabelPrintCount((asset.getLabelPrintCount() == null ? 0 : asset.getLabelPrintCount()) + 1);
            assets.save(asset);
            String designation = asset.getDesignation() == null ? "" : asset.getDesignation();
            if (designation.length() > 28) {
                designation = designation.substring(0, 27) + ".";
            }
            cells.append("<div class=\"label\"><img src=\"").append(qrCodes.toDataUri(asset.getCode(), 120)).append("\"/>")
                    .append("<div class=\"text\"><div class=\"code\">").append(HtmlUtils.htmlEscape(asset.getCode())).append("</div>")
                    .append("<div class=\"name\">").append(HtmlUtils.htmlEscape(designation)).append("</div></div></div>");
        }
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>"
                + "@page { size: A4; margin: 10.7mm 4.7mm; }"
                + "body { margin: 0; font-family: Helvetica, Arial, sans-serif; }"
                + ".label { float: left; width: 38.1mm; height: 21.2mm; box-sizing: border-box; padding: 1.5mm; overflow: hidden; }"
                + ".label img { float: left; width: 17mm; height: 17mm; }"
                + ".text { margin-left: 18.5mm; }"
                + ".code { font-size: 7pt; font-weight: bold; }"
                + ".name { font-size: 6pt; margin-top: 1mm; }"
                + "</style></head><body>" + cells + "</body></html>";
        return PdfRenderer.render(html);
    }

    // ================================================================== outils

    private CampaignView view(FixedAssetInventoryCampaign campaign) {
        List<FixedAssetInventoryScan> all = campaign.getId() == null ? List.of() : scans.findAllByCampaignIdOrderByScannedAtAsc(campaign.getId());
        List<String> expected = expected(campaign).stream().map(FixedAsset::getCode).toList();
        Set<String> seen = all.stream().map(FixedAssetInventoryScan::getAssetCode).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missing = expected.stream().filter(code -> !seen.contains(code)).toList();
        List<String> mislocated = all.stream().filter(scan -> Boolean.TRUE.equals(scan.getMislocated())).map(FixedAssetInventoryScan::getAssetCode)
                .distinct().toList();
        List<String> warnings = new ArrayList<>();
        all.stream().filter(scan -> Boolean.TRUE.equals(scan.getHolderAlert()))
                .forEach(scan -> warnings.add(scan.getAssetCode() + " scanne par son propre detenteur (" + scan.getScannedBy() + ")"));
        seen.stream().filter(code -> !expected.contains(code))
                .forEach(code -> warnings.add(code + " vu hors du perimetre de la campagne"));
        return new CampaignView(campaign, all, expected, List.copyOf(seen), missing, mislocated, warnings);
    }

    private FixedAssetInventoryCampaign refreshCounts(FixedAssetInventoryCampaign campaign) {
        CampaignView view = view(campaign);
        campaign.setExpectedCount(view.expected().size());
        campaign.setSeenCount((int) view.seen().stream().filter(view.expected()::contains).count());
        campaign.setMissingCount(view.missing().size());
        campaign.setFoundCount((int) view.scans().stream().filter(scan -> scan.getAssetId() == null).count());
        campaign.setMislocatedCount(view.mislocated().size());
        return campaigns.save(campaign);
    }

    /** Biens attendus : au fichier (non sortis), dans le perimetre de la campagne. */
    private List<FixedAsset> expected(FixedAssetInventoryCampaign campaign) {
        Set<String> resources = split(campaign.getResourceCodes());
        Set<String> categories = split(campaign.getCategoryCodes());
        return assets.findAllByOrderByCodeAsc().stream()
                .filter(asset -> !"DISPOSED".equals(asset.getAccountingStatus()) && !"DRAFT".equals(asset.getAccountingStatus()))
                .filter(asset -> campaign.getSite() == null || campaign.getSite().equalsIgnoreCase(asset.getSite() == null ? "" : asset.getSite()))
                .filter(asset -> resources.isEmpty() || resources.contains(upper(asset.getResourceCode())))
                .filter(asset -> categories.isEmpty() || categories.contains(upper(asset.getCategoryCode())))
                .toList();
    }

    private FixedAssetInventoryCampaign campaign(String code) {
        return campaigns.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Campagne d'inventaire introuvable : " + code));
    }

    private Set<String> members(FixedAssetInventoryCampaign campaign) {
        return Arrays.stream(campaign.getTeam().split(",")).map(item -> item.trim().toLowerCase(Locale.ROOT)).filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private boolean hasInventoryRight() {
        try {
            fixedAssets.require(INVENTORY);
            return true;
        } catch (ForbiddenException ex) {
            return false;
        }
    }

    private static Set<String> clean(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).map(item -> item.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static String join(Set<String> values) {
        return values.isEmpty() ? null : String.join(",", values);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
