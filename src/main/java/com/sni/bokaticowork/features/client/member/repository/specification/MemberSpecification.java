package com.sni.bokaticowork.features.client.member.repository.specification;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.specification.criteria.MemberSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MemberSpecification {

    public static Specification<Member> search(MemberSearchCriteria criteria) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (StringUtils.hasText(criteria.getCustomerId())) {
                predicates.add(
                        cb.equal(root.get("customer").get("customerId"), criteria.getCustomerId().trim())
                );
            }

            if (StringUtils.hasText(criteria.getMemberId())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("memberId")),
                                "%" + criteria.getMemberId().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getCode())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("memberId")),
                                "%" + criteria.getCode().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getFirstname())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("firstname")),
                                "%" + criteria.getFirstname().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getLastname())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("lastname")),
                                "%" + criteria.getLastname().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getEmail())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + criteria.getEmail().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getPhone())) {
                predicates.add(
                        cb.like(
                                root.get("phone"),
                                "%" + criteria.getPhone().trim() + "%"
                        )
                );
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

}
