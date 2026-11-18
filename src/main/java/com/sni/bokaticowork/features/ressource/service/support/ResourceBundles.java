package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceBundle;
import com.sni.bokaticowork.features.ressource.model.ResourceBundleItem;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceBundleRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lot R5c : lots de ressources reservees ensemble, avec remise. */
@Component
@RequiredArgsConstructor
public class ResourceBundles {

    /** Une ressource du lot (code et quantite). */
    public record Item(String resourceCode, Integer quantity) {
    }

    /** Lot ; en modification, une liste {@code items} remplace la precedente. */
    public record Request(String name, String description, BigDecimal discountPercent, Boolean active, List<Item> items) {
    }

    public record Response(Long id, String code, String name, String description, BigDecimal discountPercent, Boolean active,
                           List<ItemResponse> items) {
    }

    public record ItemResponse(String resourceCode, String resourceName, Integer quantity) {
    }

    private final ResourceBundleRepository repository;
    private final ResourceRepository resourceRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Transactional(readOnly = true)
    public List<Response> list() {
        return repository.findAllByOrderByNameAsc().stream().map(ResourceBundles::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response get(String code) {
        return toResponse(entity(code));
    }

    @Transactional
    public Response create(Request request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BadRequestException("Le nom du lot est requis");
        }
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Un lot porte déjà ce nom");
        }
        if (request.items() == null || request.items().size() < 2) {
            throw new BadRequestException("Un lot réunit au moins deux ressources");
        }
        long seq = CodeComposer.extractSeq(sequenceGenerator.next("resource_bundle", com.sni.bokaticowork.core.utils.time.AppTime.today()));
        ResourceBundle bundle = ResourceBundle.builder()
                .code(CodeComposer.refWithYear("LOT", com.sni.bokaticowork.core.utils.time.AppTime.today(), seq))
                .name(request.name().trim())
                .build();
        apply(bundle, request);
        return toResponse(repository.save(bundle));
    }

    @Transactional
    public Response update(String code, Request request) {
        ResourceBundle bundle = entity(code);
        if (request != null && StringUtils.hasText(request.name())) {
            bundle.setName(request.name().trim());
        }
        apply(bundle, request);
        return toResponse(repository.save(bundle));
    }

    @Transactional
    public void delete(String code) {
        repository.delete(entity(code));
    }

    private void apply(ResourceBundle bundle, Request request) {
        if (request == null) {
            return;
        }
        if (request.description() != null) {
            bundle.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        }
        if (request.discountPercent() != null) {
            if (request.discountPercent().signum() < 0 || request.discountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException("La remise du lot est un pourcentage, entre 0 et 100");
            }
            bundle.setDiscountPercent(request.discountPercent());
        }
        if (request.active() != null) {
            bundle.setActive(request.active());
        }
        if (request.items() != null) {
            if (request.items().size() < 2) {
                throw new BadRequestException("Un lot réunit au moins deux ressources");
            }
            bundle.getItems().clear();
            for (Item item : request.items()) {
                if (item == null || !StringUtils.hasText(item.resourceCode())) {
                    throw new BadRequestException("Chaque ressource du lot porte son code");
                }
                Resource resource = resourceRepository.findByCode(item.resourceCode().trim())
                        .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + item.resourceCode().trim()));
                bundle.getItems().add(ResourceBundleItem.builder().bundle(bundle).resource(resource)
                        .quantity(item.quantity() == null || item.quantity() < 1 ? 1 : item.quantity()).build());
            }
        }
    }

    public ResourceBundle entity(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code du lot est requis");
        }
        return repository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lot de ressources introuvable : " + code.trim()));
    }

    static Response toResponse(ResourceBundle bundle) {
        return new Response(bundle.getId(), bundle.getCode(), bundle.getName(), bundle.getDescription(), bundle.getDiscountPercent(),
                bundle.getActive(), bundle.getItems().stream()
                .map(item -> new ItemResponse(item.getResource().getCode(), item.getResource().getName(), item.getQuantity()))
                .toList());
    }
}
