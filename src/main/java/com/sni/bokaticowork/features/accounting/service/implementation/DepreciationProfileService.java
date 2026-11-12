package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile;
import com.sni.bokaticowork.features.accounting.model.FixedAsset;
import com.sni.bokaticowork.features.accounting.repository.FaCategoryRepository;
import com.sni.bokaticowork.features.accounting.repository.FaDepreciationProfileRepository;
import com.sni.bokaticowork.features.accounting.service.support.DepreciationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Profils d'amortissement par marque ou modele (lot F18, complement) : un Mac, par exemple, garde
 * plus de valeur qu'un autre ordinateur. Un profil s'applique aux biens mis en service apres sa
 * creation ; un bien deja en service change de plan par une revision.
 */
@Service
@RequiredArgsConstructor
public class DepreciationProfileService {

    private static final List<String> METHODS = List.of("LINEAR", "DECLINING", "UNITS", "NONE");

    private final FaDepreciationProfileRepository profiles;
    private final FaCategoryRepository categories;
    private final FixedAssetService fixedAssets;

    public record ProfileRequest(String label, String categoryCode, String brand, String modelPattern, String method,
                                 BigDecimal annualRate, Integer durationMonths, BigDecimal coefficient, BigDecimal residualRate,
                                 Integer priority, Boolean active, String notes) {
    }

    /** Ce qu'un bien recevrait : mode, duree, profil retenu. */
    public record Resolution(String assetCode, String method, int durationMonths, BigDecimal residualRate, FaDepreciationProfile profile) {
    }

    @Transactional(readOnly = true)
    public List<FaDepreciationProfile> list() {
        fixedAssets.require(FixedAssetService.READ);
        return profiles.findAllByOrderByBrandAscPriorityDescIdAsc();
    }

    @Transactional
    public FaDepreciationProfile create(ProfileRequest request) {
        fixedAssets.require(FixedAssetService.SETTINGS);
        FaDepreciationProfile profile = FaDepreciationProfile.builder().createdBy(fixedAssets.actor()).build();
        apply(profile, request);
        return profiles.save(profile);
    }

    @Transactional
    public FaDepreciationProfile update(Long id, ProfileRequest request) {
        fixedAssets.require(FixedAssetService.SETTINGS);
        FaDepreciationProfile profile = profile(id);
        apply(profile, request);
        profile.setUpdatedBy(fixedAssets.actor());
        profile.setUpdatedAt(Instant.now());
        return profiles.save(profile);
    }

    @Transactional
    public FaDepreciationProfile deactivate(Long id) {
        fixedAssets.require(FixedAssetService.SETTINGS);
        FaDepreciationProfile profile = profile(id);
        profile.setActive(false);
        profile.setUpdatedBy(fixedAssets.actor());
        profile.setUpdatedAt(Instant.now());
        return profiles.save(profile);
    }

    /** Ce que la fiche recoit : utile avant la mise en service pour verifier le profil retenu. */
    @Transactional(readOnly = true)
    public Resolution resolve(String assetCode) {
        fixedAssets.require(FixedAssetService.READ);
        FixedAsset asset = fixedAssets.asset(assetCode);
        FaDepreciationProfile profile = fixedAssets.profile(asset).orElse(null);
        return new Resolution(asset.getCode(), fixedAssets.method(asset), fixedAssets.duration(asset),
                profile == null ? null : profile.getResidualRate(), profile);
    }

    private void apply(FaDepreciationProfile profile, ProfileRequest request) {
        if (request == null || !StringUtils.hasText(request.brand())) {
            throw new BadRequestException("Un profil d'amortissement porte une marque");
        }
        String method = StringUtils.hasText(request.method()) ? request.method().trim().toUpperCase(Locale.ROOT) : null;
        if (method != null && !METHODS.contains(method)) {
            throw new BadRequestException("Mode d'amortissement inconnu : " + method + " (LINEAR, DECLINING, UNITS ou NONE)");
        }
        if (request.annualRate() != null && (request.annualRate().signum() <= 0 || request.annualRate().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("Le taux annuel est un pourcentage entre 0 et 100");
        }
        if (request.durationMonths() != null && request.durationMonths() <= 0) {
            throw new BadRequestException("La duree est un nombre de mois positif");
        }
        if (request.residualRate() != null && (request.residualRate().signum() < 0 || request.residualRate().compareTo(BigDecimal.valueOf(100)) >= 0)) {
            throw new BadRequestException("La valeur residuelle est un pourcentage du cout, de 0 a moins de 100");
        }
        Integer duration = request.durationMonths() != null ? request.durationMonths()
                : request.annualRate() != null ? DepreciationCalculator.monthsForRate(request.annualRate()) : null;
        if ("DECLINING".equals(method) && duration != null && duration < 36) {
            throw new BadRequestException("Le degressif s'applique aux biens d'au moins 3 ans");
        }
        if (method == null && request.annualRate() == null && request.durationMonths() == null && request.coefficient() == null
                && request.residualRate() == null) {
            throw new BadRequestException("Un profil change au moins le mode, le taux, la duree, le coefficient ou la valeur residuelle");
        }
        String category = StringUtils.hasText(request.categoryCode()) ? request.categoryCode().trim().toUpperCase(Locale.ROOT) : null;
        if (category != null && categories.findById(category).isEmpty()) {
            throw new BadRequestException("Categorie inconnue : " + category);
        }
        String brand = request.brand().trim();
        profile.setLabel(StringUtils.hasText(request.label()) ? request.label().trim()
                : brand + (StringUtils.hasText(request.modelPattern()) ? " " + request.modelPattern().trim() : ""));
        profile.setCategoryCode(category);
        profile.setBrand(brand);
        profile.setModelPattern(StringUtils.hasText(request.modelPattern()) ? request.modelPattern().trim() : null);
        profile.setMethod(method);
        profile.setAnnualRate(request.annualRate());
        profile.setDurationMonths(request.durationMonths());
        profile.setCoefficient(request.coefficient());
        profile.setResidualRate(request.residualRate());
        profile.setPriority(request.priority() == null ? 0 : request.priority());
        profile.setActive(request.active() == null || request.active());
        profile.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
    }

    private FaDepreciationProfile profile(Long id) {
        return profiles.findById(id).orElseThrow(() -> new ResourceNotFoundException("Profil d'amortissement introuvable : " + id));
    }
}
