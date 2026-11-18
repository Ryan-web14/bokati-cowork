package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import com.sni.bokaticowork.features.ressource.repository.specification.specification.ResourceSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R4 des ressources : filtres par code et graphe d'entites. */
class ResourceR4Test {

    @Test
    @SuppressWarnings("unchecked")
    void searchFiltersByTypeCode() {
        Root<Resource> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        Path<Object> typeCode = mock(Path.class);
        when(root.get("resourceType").get("code")).thenReturn(typeCode);

        ResourceSpecification.search(ResourceSearchCriteria.builder().typeCode(" sal ").build()).toPredicate(root, query, cb);

        verify(cb).upper(any());
        verify(cb).equal(any(), org.mockito.ArgumentMatchers.eq("SAL"));
    }

    @Test
    void listsLoadTypeGroupAndPolicyWithThePage() throws Exception {
        EntityGraph graph = ResourceRepository.class.getMethod("findAll", Pageable.class).getAnnotation(EntityGraph.class);
        assertThat(graph).isNotNull();
        assertThat(graph.attributePaths()).containsExactlyInAnyOrder("resourceType", "resourceGroup", "resourcePolicy");
    }
}
