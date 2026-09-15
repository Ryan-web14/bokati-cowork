package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTemplateRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryVariantGenerationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTemplateResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryVariantGenerationResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTemplate;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryVariantAxis;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryVariantValue;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemTemplateRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemTemplateService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemTemplateServiceImpl implements InventoryItemTemplateService {

    private static final String TEMPLATE_SEQUENCE = "inventory_item_template";

    /**
     * Garde-fou : le produit cartesien croit tres vite. Quatre axes de cinq valeurs font deja
     * six cent vingt-cinq articles, ce qui est presque toujours une erreur de saisie.
     */
    private static final int MAX_COMBINATIONS = 500;

    private final InventoryItemTemplateRepository templateRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryItemService itemService;
    private final InventoryCategoryService categoryService;
    private final InventoryUnitService unitService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public InventoryItemTemplateResponse create(InventoryItemTemplateRequest request) {
        InventoryItemTemplate template = new InventoryItemTemplate();
        template.setTemplateCode(nextTemplateCode());
        applyTemplate(template, request);
        return toResponse(templateRepository.save(template));
    }

    @Override
    public InventoryItemTemplateResponse update(String templateCode, InventoryItemTemplateRequest request) {
        InventoryItemTemplate template = findOrThrow(templateCode);
        applyTemplate(template, request);
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemTemplateResponse get(String templateCode) {
        return toResponse(findOrThrow(templateCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemTemplateResponse> list(Boolean active) {
        List<InventoryItemTemplate> templates = Boolean.TRUE.equals(active)
                ? templateRepository.findAllByActiveTrueOrderByNameAsc()
                : templateRepository.findAllByOrderByNameAsc();
        return templates.stream().map(this::toResponse).toList();
    }

    @Override
    public InventoryVariantGenerationResponse generateVariants(String templateCode,
                                                               InventoryVariantGenerationRequest request) {
        InventoryItemTemplate template = findOrThrow(templateCode);
        if (template.getAxes().isEmpty()) {
            throw new BadRequestException("Template has no variation axis");
        }

        List<List<InventoryVariantValue>> selection = resolveSelection(template, request);
        List<List<InventoryVariantValue>> combinations = cartesianProduct(selection);

        if (combinations.size() > MAX_COMBINATIONS) {
            throw new BadRequestException("Requested generation produces " + combinations.size()
                    + " variants, above the limit of " + MAX_COMBINATIONS
                    + ". Narrow the selection with selectedValues.");
        }

        boolean dryRun = Boolean.TRUE.equals(request == null ? null : request.getDryRun());
        List<InventoryVariantGenerationResponse.Variant> variants = new ArrayList<>();
        int created = 0;
        int skipped = 0;

        for (List<InventoryVariantValue> combination : combinations) {
            String signature = signatureOf(combination);
            InventoryItem existing = itemRepository.findByTemplateAndVariantSignature(template, signature).orElse(null);

            if (existing != null) {
                skipped++;
                variants.add(InventoryVariantGenerationResponse.Variant.builder()
                        .itemCode(existing.getItemCode())
                        .name(existing.getName())
                        .variantSignature(signature)
                        .alreadyExisted(true)
                        .build());
                continue;
            }

            String name = variantName(template, combination);
            if (dryRun) {
                variants.add(InventoryVariantGenerationResponse.Variant.builder()
                        .name(name)
                        .variantSignature(signature)
                        .alreadyExisted(false)
                        .build());
                continue;
            }

            InventoryItemResponse response = itemService.createVariant(
                    variantRequest(template, name), template.getTemplateCode(), signature);
            created++;
            variants.add(InventoryVariantGenerationResponse.Variant.builder()
                    .itemCode(response.getItemCode())
                    .name(response.getName())
                    .variantSignature(signature)
                    .alreadyExisted(false)
                    .build());
        }

        return InventoryVariantGenerationResponse.builder()
                .templateCode(template.getTemplateCode())
                .dryRun(dryRun)
                .requestedCombinations(combinations.size())
                .createdCount(created)
                .skippedCount(skipped)
                .variants(variants)
                .build();
    }

    private void applyTemplate(InventoryItemTemplate template, InventoryItemTemplateRequest request) {
        template.setName(request.getName().trim());
        template.setDescription(trimToNull(request.getDescription()));
        template.setCategory(StringUtils.hasText(request.getCategoryCode())
                ? categoryService.findByCodeOrThrow(request.getCategoryCode()) : null);
        template.setUnit(StringUtils.hasText(request.getUnitCode())
                ? unitService.findByCodeOrThrow(request.getUnitCode()) : null);
        template.setItemType(request.getItemType());
        template.setTrackingType(request.getTrackingType() == null
                ? defaultTrackingType(request.getItemType()) : request.getTrackingType());
        template.setDefaultCost(request.getDefaultCost());
        template.setSalePrice(request.getSalePrice());
        template.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

        replaceAxes(template, request.getAxes());
    }

    /**
     * Remplace integralement les axes du modele.
     *
     * <p>Une modification d'axes ne touche pas aux variantes deja generees : elles restent des
     * articles autonomes. C'est volontaire, supprimer des articles porteurs de stock parce qu'une
     * valeur d'axe disparait serait destructeur.</p>
     */
    private void replaceAxes(InventoryItemTemplate template, List<InventoryItemTemplateRequest.Axis> axes) {
        Set<String> axisCodes = axes.stream()
                .map(axis -> normalizeCode(axis.getAxisCode()))
                .collect(Collectors.toSet());
        if (axisCodes.size() != axes.size()) {
            throw new BadRequestException("Duplicate axis code in template");
        }

        template.getAxes().clear();
        int axisPosition = 1;
        for (InventoryItemTemplateRequest.Axis axisRequest : axes) {
            InventoryVariantAxis axis = InventoryVariantAxis.builder()
                    .template(template)
                    .axisCode(normalizeCode(axisRequest.getAxisCode()))
                    .name(axisRequest.getName().trim())
                    .position(axisRequest.getPosition() == null ? axisPosition : axisRequest.getPosition())
                    .values(new ArrayList<>())
                    .build();

            Set<String> valueCodes = axisRequest.getValues().stream()
                    .map(value -> normalizeCode(value.getValueCode()))
                    .collect(Collectors.toSet());
            if (valueCodes.size() != axisRequest.getValues().size()) {
                throw new BadRequestException("Duplicate value code on axis " + axis.getAxisCode());
            }

            int valuePosition = 1;
            for (InventoryItemTemplateRequest.Value valueRequest : axisRequest.getValues()) {
                axis.getValues().add(InventoryVariantValue.builder()
                        .axis(axis)
                        .valueCode(normalizeCode(valueRequest.getValueCode()))
                        .label(valueRequest.getLabel().trim())
                        .position(valueRequest.getPosition() == null ? valuePosition : valueRequest.getPosition())
                        .active(valueRequest.getActive() == null ? Boolean.TRUE : valueRequest.getActive())
                        .build());
                valuePosition++;
            }

            template.getAxes().add(axis);
            axisPosition++;
        }
    }

    /**
     * Retient, pour chaque axe, les valeurs demandees, ou toutes ses valeurs actives par defaut.
     */
    private List<List<InventoryVariantValue>> resolveSelection(InventoryItemTemplate template,
                                                                InventoryVariantGenerationRequest request) {
        Map<String, List<String>> selected = request == null || request.getSelectedValues() == null
                ? Map.of()
                : request.getSelectedValues();

        Map<String, List<String>> normalizedSelection = new LinkedHashMap<>();
        selected.forEach((axisCode, values) -> normalizedSelection.put(normalizeCode(axisCode),
                values == null ? List.of() : values.stream().map(this::normalizeCode).toList()));

        Set<String> knownAxes = template.getAxes().stream()
                .map(InventoryVariantAxis::getAxisCode)
                .collect(Collectors.toSet());
        normalizedSelection.keySet().stream()
                .filter(axisCode -> !knownAxes.contains(axisCode))
                .findFirst()
                .ifPresent(unknown -> {
                    throw new BadRequestException("Unknown axis in selection: " + unknown);
                });

        List<List<InventoryVariantValue>> result = new ArrayList<>();
        for (InventoryVariantAxis axis : template.getAxes()) {
            List<String> wanted = normalizedSelection.get(axis.getAxisCode());
            List<InventoryVariantValue> values = axis.getValues().stream()
                    .filter(value -> Boolean.TRUE.equals(value.getActive()))
                    .filter(value -> wanted == null || wanted.isEmpty() || wanted.contains(value.getValueCode()))
                    .toList();
            if (values.isEmpty()) {
                throw new BadRequestException("No selectable value left on axis " + axis.getAxisCode());
            }
            result.add(values);
        }
        return result;
    }

    private List<List<InventoryVariantValue>> cartesianProduct(List<List<InventoryVariantValue>> axes) {
        List<List<InventoryVariantValue>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<InventoryVariantValue> axisValues : axes) {
            List<List<InventoryVariantValue>> expanded = new ArrayList<>();
            for (List<InventoryVariantValue> partial : result) {
                for (InventoryVariantValue value : axisValues) {
                    List<InventoryVariantValue> combination = new ArrayList<>(partial);
                    combination.add(value);
                    expanded.add(combination);
                }
            }
            result = expanded;
        }
        return result;
    }

    private String signatureOf(List<InventoryVariantValue> combination) {
        return combination.stream()
                .map(value -> value.getAxis().getAxisCode() + "=" + value.getValueCode())
                .collect(Collectors.joining(";"));
    }

    private String variantName(InventoryItemTemplate template, List<InventoryVariantValue> combination) {
        String suffix = combination.stream()
                .map(InventoryVariantValue::getLabel)
                .collect(Collectors.joining(" "));
        return (template.getName() + " " + suffix).trim();
    }

    private InventoryItemRequest variantRequest(InventoryItemTemplate template, String name) {
        InventoryItemRequest request = new InventoryItemRequest();
        request.setName(name);
        request.setDescription(template.getDescription());
        request.setCategoryCode(template.getCategory() == null ? null : template.getCategory().getCode());
        request.setUnitCode(template.getUnit() == null ? null : template.getUnit().getCode());
        request.setItemType(template.getItemType());
        request.setTrackingType(template.getTrackingType());
        request.setDefaultCost(template.getDefaultCost());
        request.setSalePrice(template.getSalePrice());
        return request;
    }

    private InventoryItemTemplate findOrThrow(String templateCode) {
        return templateRepository.findByTemplateCode(normalizeCode(templateCode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item template not found"));
    }

    private String nextTemplateCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = normalizeCode(sequenceGenerator.next(TEMPLATE_SEQUENCE, LocalDate.now()));
            if (!templateRepository.existsByTemplateCode(code)) {
                return code;
            }
        }
        throw new ResourceAlreadyExistException("Unable to generate unique template code");
    }

    private InventoryTrackingType defaultTrackingType(InventoryItemType itemType) {
        return itemType == InventoryItemType.ASSET ? InventoryTrackingType.SERIAL : InventoryTrackingType.QUANTITY;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private InventoryItemTemplateResponse toResponse(InventoryItemTemplate template) {
        List<InventoryItemTemplateResponse.Axis> axes = template.getAxes().stream()
                .map(axis -> InventoryItemTemplateResponse.Axis.builder()
                        .axisCode(axis.getAxisCode())
                        .name(axis.getName())
                        .position(axis.getPosition())
                        .values(axis.getValues().stream()
                                .map(value -> InventoryItemTemplateResponse.Value.builder()
                                        .valueCode(value.getValueCode())
                                        .label(value.getLabel())
                                        .position(value.getPosition())
                                        .active(value.getActive())
                                        .build())
                                .toList())
                        .build())
                .toList();

        int combinations = template.getAxes().isEmpty() ? 0 : template.getAxes().stream()
                .mapToInt(axis -> (int) axis.getValues().stream()
                        .filter(value -> Boolean.TRUE.equals(value.getActive()))
                        .count())
                .reduce(1, (left, right) -> left * right);

        return InventoryItemTemplateResponse.builder()
                .templateCode(template.getTemplateCode())
                .name(template.getName())
                .description(template.getDescription())
                .categoryCode(template.getCategory() == null ? null : template.getCategory().getCode())
                .categoryName(template.getCategory() == null ? null : template.getCategory().getName())
                .unitCode(template.getUnit() == null ? null : template.getUnit().getCode())
                .itemType(template.getItemType())
                .trackingType(template.getTrackingType())
                .defaultCost(template.getDefaultCost())
                .salePrice(template.getSalePrice())
                .active(template.getActive())
                .possibleCombinations(combinations)
                .generatedVariants(itemRepository.countByTemplate(template))
                .axes(axes)
                .build();
    }
}
