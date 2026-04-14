package com.sni.bokaticowork.features.client.customer.repository.specification;

import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Customer> hasType(CustomerType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Customer> withFilters(CustomerStatus status, CustomerType type) {
        return Specification.where(distinct())
                .and(hasStatus(status))
                .and(hasType(type));
    }

    private static Specification<Customer> distinct() {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }
}
