package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.ressource.dto.request.AudienceRef;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAudience;
import com.sni.bokaticowork.features.ressource.model.ResourceVisibility;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAudienceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.pricing.PriceContext;
import com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lot B10 : visibilite d'une ressource par client (decision D6).
 *
 * <ul>
 *   <li>{@code PUBLIC} : visible et reservable par tous ;</li>
 *   <li>{@code RESTRICTED} : seulement l'audience designee, invisible des autres (introuvable) ;</li>
 *   <li>{@code ON_REQUEST} : visible de tous ; l'audience reserve directement, les autres font une
 *   demande, que l'equipe approuve.</li>
 * </ul>
 *
 * <p>Audience : membres, clients ou organisations, entreprises, segments, plans. Un membre est aussi
 * reconnu par les organisations auxquelles il est rattache.</p>
 */
@Component
@RequiredArgsConstructor
public class ResourceVisibilityService {

    /** Acces d'un client a une ressource. */
    public enum Access { DIRECT, ON_REQUEST, HIDDEN }

    public record VisibilityRequest(String mode, List<AudienceRef> audiences) {
    }

    public record VisibilityResponse(String resourceCode, String mode, List<AudienceRef> audiences) {
    }

    private final ResourceRepository resourceRepository;
    private final ResourceAudienceRepository audienceRepository;
    private PriceContextResolver contextResolver;

    @Autowired(required = false)
    void setContextResolver(PriceContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    @Transactional(readOnly = true)
    public VisibilityResponse get(String resourceCode) {
        Resource resource = resource(resourceCode);
        return view(resource);
    }

    /** Remplace le mode et l'audience de la ressource. */
    @Transactional
    public VisibilityResponse set(String resourceCode, VisibilityRequest request) {
        Resource resource = resource(resourceCode);
        ResourceVisibility mode = parse(request == null ? null : request.mode());
        List<String> keys = request == null || request.audiences() == null ? List.of()
                : request.audiences().stream().map(AudienceRef::key).distinct().toList();
        if (mode == ResourceVisibility.RESTRICTED && keys.isEmpty()) {
            throw new BadRequestException("Une ressource réservée demande au moins un client ou groupe désigné");
        }
        resource.setVisibilityMode(mode);
        resourceRepository.save(resource);
        audienceRepository.deleteAllForResource(resource);
        for (String key : keys) {
            AudienceRef ref = AudienceRef.of(key);
            audienceRepository.save(ResourceAudience.builder().resource(resource)
                    .audienceType(ref.type()).audienceCode(ref.code()).build());
        }
        return view(resource);
    }

    private VisibilityResponse view(Resource resource) {
        return new VisibilityResponse(resource.getCode(), mode(resource).name(),
                audienceRepository.findAllByResourceOrderByIdAsc(resource).stream()
                        .map(a -> new AudienceRef(a.getAudienceType(), a.getAudienceCode())).toList());
    }

    private static ResourceVisibility parse(String value) {
        if (!StringUtils.hasText(value)) {
            return ResourceVisibility.PUBLIC;
        }
        try {
            return ResourceVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Visibilité invalide : PUBLIC, RESTRICTED ou ON_REQUEST");
        }
    }

    public static ResourceVisibility mode(Resource resource) {
        return resource.getVisibilityMode() == null ? ResourceVisibility.PUBLIC : resource.getVisibilityMode();
    }

    // ------------------------------------------------------------------ acces

    /** Contexte d'un client (titulaire) ; un visiteur sans compte n'a que le segment PUBLIC. */
    public PriceContext context(String ownerType, String ownerCode) {
        return contextResolver == null ? PriceContext.anonymous() : contextResolver.forOwner(ownerType, ownerCode);
    }

    /** Acces d'un client a la ressource. */
    @Transactional(readOnly = true)
    public Access access(Resource resource, PriceContext context) {
        ResourceVisibility mode = mode(resource);
        if (mode == ResourceVisibility.PUBLIC) {
            return Access.DIRECT;
        }
        Set<String> keys = context == null ? Set.of() : context.audienceKeys();
        boolean designated = audienceRepository.findAllByResourceOrderByIdAsc(resource).stream()
                .anyMatch(audience -> keys.contains(audience.key()));
        if (designated) {
            return Access.DIRECT;
        }
        return mode == ResourceVisibility.RESTRICTED ? Access.HIDDEN : Access.ON_REQUEST;
    }

    @Transactional(readOnly = true)
    public Access access(Resource resource, String ownerType, String ownerCode) {
        return mode(resource) == ResourceVisibility.PUBLIC ? Access.DIRECT : access(resource, context(ownerType, ownerCode));
    }

    /** Ressources reservees visibles par ce client (pour filtrer les listes du portail). */
    @Transactional(readOnly = true)
    public List<Long> restrictedVisibleIds(PriceContext context) {
        Set<String> keys = context == null ? Set.of() : context.audienceKeys();
        return keys.isEmpty() ? List.of() : audienceRepository.resourceIdsFor(keys);
    }

    private Resource resource(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de la ressource est requis");
        }
        return resourceRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code.trim()));
    }
}
