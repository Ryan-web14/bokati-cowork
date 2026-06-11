package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SubscriptionSpecification {

    private SubscriptionSpecification() {
    }

    public static Specification<Subscription> search(SubscriptionSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getSubscriberType() != null) {
                predicates.add(cb.equal(root.get("subscriberType"), criteria.getSubscriberType()));
            }
            if (StringUtils.hasText(criteria.getSubscriberCode())) {
                predicates.add(cb.equal(root.get("subscriberCode"), criteria.getSubscriberCode().trim()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getNextBillingBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("nextBillingDate"), criteria.getNextBillingBefore()));
            }
            if (StringUtils.hasText(criteria.getPlanCode())) {
                var plan = root.join("planVersion", JoinType.INNER).join("plan", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(plan.get("code")), criteria.getPlanCode().trim().toLowerCase()));
            }

            predicates.add(ownerExists(root, query, cb));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate ownerExists(Root<Subscription> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                         jakarta.persistence.criteria.CriteriaBuilder cb) {
        return cb.or(
                cb.and(
                        cb.equal(root.get("subscriberType"), SubscriberType.MEMBER),
                        existsMember(root, query, cb)
                ),
                cb.and(
                        cb.equal(root.get("subscriberType"), SubscriberType.CUSTOMER),
                        existsCustomer(root, query, cb)
                ),
                cb.and(
                        cb.equal(root.get("subscriberType"), SubscriberType.BUSINESS_ENTITY),
                        existsBusiness(root, query, cb)
                )
        );
    }

    private static Predicate existsMember(Root<Subscription> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                          jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Member> member = subquery.from(Member.class);
        subquery.select(member.get("id"))
                .where(cb.equal(member.get("id"), relatedId(root.get("member"))));
        return cb.exists(subquery);
    }

    private static Predicate existsCustomer(Root<Subscription> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Customer> customer = subquery.from(Customer.class);
        subquery.select(customer.get("id"))
                .where(cb.equal(customer.get("id"), relatedId(root.get("customer"))));
        return cb.exists(subquery);
    }

    private static Predicate existsBusiness(Root<Subscription> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BusinessEntity> business = subquery.from(BusinessEntity.class);
        subquery.select(business.get("id"))
                .where(cb.equal(business.get("id"), relatedId(root.get("businessEntity"))));
        return cb.exists(subquery);
    }

    private static Expression<Long> relatedId(jakarta.persistence.criteria.Path<?> path) {
        return path.get("id");
    }
}
