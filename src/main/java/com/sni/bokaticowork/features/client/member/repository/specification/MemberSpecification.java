package com.sni.bokaticowork.features.client.member.repository.specification;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class MemberSpecification {

    private static final double SIMILARITY_THRESHOLD = 0.20d;

    public static Specification<Member> search(MemberSearchCriteria criteria, Collection<String> fuzzyMatchedMemberIds) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> textPredicates = new ArrayList<>();

            if (criteria == null) {
                if (fuzzyMatchedMemberIds == null) {
                    return cb.conjunction();
                }
                return fuzzyMatchedMemberIds.isEmpty()
                        ? cb.disjunction()
                        : root.get("memberId").in(fuzzyMatchedMemberIds);
            }

            if (fuzzyMatchedMemberIds != null) {
                predicates.add(
                        fuzzyMatchedMemberIds.isEmpty()
                                ? cb.disjunction()
                                : root.get("memberId").in(fuzzyMatchedMemberIds)
                );
            }

            if (StringUtils.hasText(criteria.getCustomerId())) {
                predicates.add(
                        fuzzyTextMatch(cb, root.get("customer").get("customerId").as(String.class), criteria.getCustomerId().trim())
                );
            }

            if (StringUtils.hasText(criteria.getMemberId())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("memberId").as(String.class), criteria.getMemberId().trim())
                );
            }

            if (StringUtils.hasText(criteria.getCode())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("memberId").as(String.class), criteria.getCode().trim())
                );
            }

            if (StringUtils.hasText(criteria.getFirstname())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("firstname").as(String.class), criteria.getFirstname().trim())
                );
            }

            if (StringUtils.hasText(criteria.getLastname())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("lastname").as(String.class), criteria.getLastname().trim())
                );
            }

            if (StringUtils.hasText(criteria.getEmail())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("email").as(String.class), criteria.getEmail().trim())
                );
            }

            if (StringUtils.hasText(criteria.getPhone())) {
                textPredicates.add(
                        fuzzyTextMatch(cb, root.get("phone").as(String.class), criteria.getPhone().trim())
                );
            }

            if (!textPredicates.isEmpty()) {
                predicates.add(cb.or(textPredicates.toArray(new Predicate[0])));
            }

            if (criteria.getStatus() != null) {
                predicates.add(
                        cb.equal(root.get("status"), criteria.getStatus())
                );
            }

            if (criteria.getPortalAccess() != null) {
                predicates.add(
                        cb.equal(root.get("portalAccess"), criteria.getPortalAccess())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));

        };

    }

    private static Predicate fuzzyTextMatch(jakarta.persistence.criteria.CriteriaBuilder cb,
                                            Expression<String> field,
                                            String value) {
        String normalizedValue = normalizeSearchValue(value);
        if (!StringUtils.hasText(normalizedValue)) {
            return cb.conjunction();
        }

        Expression<String> normalizedField = cb.function("normalize_text", String.class, field);
        Predicate contains = cb.like(normalizedField, "%" + normalizedValue + "%", '\\');
        Predicate similar = cb.greaterThanOrEqualTo(
                cb.function("similarity", Double.class, normalizedField, cb.literal(normalizedValue)),
                similarityThreshold(normalizedValue)
        );

        return cb.or(contains, similar);
    }

    private static Double similarityThreshold(String value) {
        return value.length() <= 4 ? 0.15d : SIMILARITY_THRESHOLD;
    }

    private static String normalizeSearchValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replace("%", "\\%").replace("_", "\\_");
    }

}
