package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemSubstituteRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTranslationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemSubstituteResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTranslationResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemSubstitute;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTranslation;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemSubstituteRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemTranslationRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemRelationServiceImpl implements InventoryItemRelationService {

    private static final int MAX_LANGUAGE_CODE_LENGTH = 8;

    private final InventoryItemLookupService itemLookupService;
    private final InventoryItemSubstituteRepository substituteRepository;
    private final InventoryItemTranslationRepository translationRepository;

    @Override
    public InventoryItemSubstituteResponse addSubstitute(String itemCode, InventoryItemSubstituteRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        InventoryItem substitute = itemLookupService.findByItemCodeOrThrow(request.getSubstituteItemCode());

        if (item.getId().equals(substitute.getId())) {
            throw new BadRequestException("An item cannot substitute itself");
        }
        substituteRepository.findByItemAndSubstituteItem(item, substitute).ifPresent(existing -> {
            throw new ResourceAlreadyExistException("Substitute already declared for this item");
        });

        InventoryItemSubstitute link = InventoryItemSubstitute.builder()
                .item(item)
                .substituteItem(substitute)
                .priority(request.getPriority() == null ? nextPriority(item) : request.getPriority())
                .conversionFactor(request.getConversionFactor() == null ? BigDecimal.ONE : request.getConversionFactor())
                .bidirectional(Boolean.TRUE.equals(request.getBidirectional()))
                .notes(trimToNull(request.getNotes()))
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return toResponse(substituteRepository.save(link), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemSubstituteResponse> listSubstitutes(String itemCode) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);

        List<InventoryItemSubstituteResponse> result = new ArrayList<>(
                substituteRepository.findAllByItemAndActiveTrueOrderByPriorityAsc(item).stream()
                        .map(link -> toResponse(link, false))
                        .toList());

        // Un lien declare reciproque sur l'autre article vaut aussi pour celui-ci, sans ligne en base.
        substituteRepository.findReciprocal(item).stream()
                .map(this::toReciprocalResponse)
                .forEach(result::add);

        result.sort(Comparator.comparing(InventoryItemSubstituteResponse::getPriority,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Override
    public void deleteSubstitute(Long substituteId) {
        InventoryItemSubstitute link = substituteRepository.findById(substituteId)
                .orElseThrow(() -> new ResourceNotFoundException("Substitute link not found"));
        substituteRepository.delete(link);
    }

    @Override
    public InventoryItemTranslationResponse upsertTranslation(String itemCode, InventoryItemTranslationRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        String language = normalizeLanguage(request.getLanguageCode());

        InventoryItemTranslation translation = translationRepository.findByItemAndLanguageCode(item, language)
                .orElseGet(() -> InventoryItemTranslation.builder()
                        .item(item)
                        .languageCode(language)
                        .build());

        translation.setName(request.getName().trim());
        translation.setDescription(trimToNull(request.getDescription()));

        return toResponse(translationRepository.save(translation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemTranslationResponse> listTranslations(String itemCode) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        return translationRepository.findAllByItemOrderByLanguageCodeAsc(item).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteTranslation(Long translationId) {
        InventoryItemTranslation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation not found"));
        translationRepository.delete(translation);
    }

    private int nextPriority(InventoryItem item) {
        return substituteRepository.findAllByItemAndActiveTrueOrderByPriorityAsc(item).stream()
                .map(InventoryItemSubstitute::getPriority)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(1);
    }

    private String normalizeLanguage(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Language code is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LANGUAGE_CODE_LENGTH) {
            throw new BadRequestException("Language code must not exceed " + MAX_LANGUAGE_CODE_LENGTH + " characters");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private InventoryItemSubstituteResponse toResponse(InventoryItemSubstitute link, boolean inherited) {
        return InventoryItemSubstituteResponse.builder()
                .id(link.getId())
                .itemCode(link.getItem().getItemCode())
                .substituteItemCode(link.getSubstituteItem().getItemCode())
                .substituteItemName(link.getSubstituteItem().getName())
                .priority(link.getPriority())
                .conversionFactor(link.getConversionFactor())
                .bidirectional(link.getBidirectional())
                .inherited(inherited)
                .notes(link.getNotes())
                .active(link.getActive())
                .build();
    }

    /**
     * Presente un lien reciproque du point de vue de l'article courant : les deux articles sont
     * echanges, et le facteur de conversion est inverse.
     */
    private InventoryItemSubstituteResponse toReciprocalResponse(InventoryItemSubstitute link) {
        BigDecimal factor = link.getConversionFactor();
        BigDecimal inverted = factor == null || factor.signum() == 0
                ? BigDecimal.ONE
                : BigDecimal.ONE.divide(factor, 6, java.math.RoundingMode.HALF_UP);

        return InventoryItemSubstituteResponse.builder()
                .id(link.getId())
                .itemCode(link.getSubstituteItem().getItemCode())
                .substituteItemCode(link.getItem().getItemCode())
                .substituteItemName(link.getItem().getName())
                .priority(link.getPriority())
                .conversionFactor(inverted)
                .bidirectional(Boolean.TRUE)
                .inherited(true)
                .notes(link.getNotes())
                .active(link.getActive())
                .build();
    }

    private InventoryItemTranslationResponse toResponse(InventoryItemTranslation entity) {
        return InventoryItemTranslationResponse.builder()
                .id(entity.getId())
                .itemCode(entity.getItem().getItemCode())
                .languageCode(entity.getLanguageCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}
