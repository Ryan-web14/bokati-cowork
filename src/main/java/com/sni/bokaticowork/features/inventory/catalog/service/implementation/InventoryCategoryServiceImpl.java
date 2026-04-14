package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryCategoryRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryCategoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryCategoryMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCategoryServiceImpl implements InventoryCategoryService {

    private final InventoryCategoryRepository repository;
    private final InventoryCategoryMapper mapper;

    @Override
    public InventoryCategoryResponse create(InventoryCategoryRequest request) {
        String code = normalizeCode(StringUtils.hasText(request.getCode()) ? request.getCode() : request.getName());
        if (repository.existsByCode(code)) {
            throw new ResourceAlreadyExistException("Inventory category already exists");
        }
        InventoryCategory entity = mapper.toEntity(request);
        entity.setCode(code);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryCategoryResponse update(String code, InventoryCategoryRequest request) {
        InventoryCategory entity = findByCodeOrThrow(code);
        apply(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCategoryResponse get(String code) {
        return mapper.toResponse(findByCodeOrThrow(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryCategoryResponse> list(Boolean active) {
        List<InventoryCategory> categories = active == null
                ? repository.findAllByOrderByNameAsc()
                : Boolean.TRUE.equals(active)
                ? repository.findAllByActiveTrueOrderByNameAsc()
                : repository.findAllByOrderByNameAsc().stream().filter(item -> Boolean.FALSE.equals(item.getActive())).toList();
        return categories.stream().map(mapper::toResponse).toList();
    }

    @Override
    public InventoryCategoryResponse activate(String code) {
        InventoryCategory entity = findByCodeOrThrow(code);
        entity.setActive(Boolean.TRUE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public InventoryCategoryResponse deactivate(String code) {
        InventoryCategory entity = findByCodeOrThrow(code);
        entity.setActive(Boolean.FALSE);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCategory findByCodeOrThrow(String code) {
        return repository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category not found"));
    }

    private void apply(InventoryCategory entity, InventoryCategoryRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Inventory category name is required");
        }
        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity.setParentCategory(StringUtils.hasText(request.getParentCategoryCode())
                ? findByCodeOrThrow(request.getParentCategoryCode())
                : null);
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Inventory category code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
