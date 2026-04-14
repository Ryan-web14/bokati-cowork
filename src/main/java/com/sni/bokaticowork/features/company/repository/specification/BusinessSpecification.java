package com.sni.bokaticowork.features.company.repository.specification;

import com.sni.bokaticowork.features.company.dto.request.BusinessSearchCriteria;
import com.sni.bokaticowork.features.company.enums.Status;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BusinessSpecification {

    private BusinessSpecification() {
    }

    public static Specification<BusinessEntity> search(BusinessSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            likeIfPresent(predicates, cb, root.get("code"), criteria.getCode());
            likeIfPresent(predicates, cb, root.get("name"), criteria.getName());
            likeIfPresent(predicates, cb, root.get("legalForm"), criteria.getLegalForm());
            likeIfPresent(predicates, cb, root.get("niuNumber"), criteria.getNiuNumber());
            likeIfPresent(predicates, cb, root.get("rccmNumber"), criteria.getRccmNumber());
            likeIfPresent(predicates, cb, root.get("taxId"), criteria.getTaxId());
            likeIfPresent(predicates, cb, root.get("activity"), criteria.getActivity());
            likeIfPresent(predicates, cb, root.get("phone"), criteria.getPhone());
            likeIfPresent(predicates, cb, root.get("email"), criteria.getEmail());

            if (StringUtils.hasText(criteria.getBaseCurrencyCode())) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("baseCurrency").get("currencyCode")),
                                criteria.getBaseCurrencyCode().trim().toLowerCase(Locale.ROOT)
                        )
                );
            }

            if (StringUtils.hasText(criteria.getStatus())) {
                predicates.add(cb.equal(root.get("status"), Status.valueOf(criteria.getStatus().trim().toUpperCase(Locale.ROOT))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void likeIfPresent(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                      jakarta.persistence.criteria.Path<String> path, String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(cb.like(cb.lower(path), "%" + value.trim().toLowerCase(Locale.ROOT) + "%"));
        }
    }
}
