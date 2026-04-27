package com.sni.bokaticowork.features.client.customer.repository;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @Query(nativeQuery = true, value = "SELECT * FROM customer WHERE customer_id = :customerId")
    Optional<Customer> findByCustomerId(@Param("customerId") String customerId);

    @Query(nativeQuery = true, value = "SELECT * FROM customer WHERE email = :email")
    Optional<Customer> findByEmail(String email);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.status = :status"
    )
    List<Customer> findDistinctByStatus(@Param("status") String status);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.type = :type"
    )
    List<Customer> findDistinctByType(@Param("type") String type);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.status = :status AND c.type = :type"
    )
    List<Customer> findDistinctByStatusAndType(@Param("status") String status, @Param("type") String type);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c " +
                    "WHERE (:status IS NULL OR c.status = :status) " +
                    "AND (:type IS NULL OR c.type = :type)"
    )
    List<Customer> findDistinctByStatusAndTypeOptional(@Param("status") String status, @Param("type") String type);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = true"
    )
    List<Customer> findAllDeletedCustomers();

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.email = :email"
    )
    Optional<Customer> findByEmailAndDeletedFalse(@Param("email") String email);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.phone = :phone LIMIT 1"
    )
    Optional<Customer> findByPhoneAndDeletedFalse(@Param("phone") String phone);

    @Query(
            nativeQuery = true,
            value = "SELECT DISTINCT c.* FROM customer c WHERE c.deleted = false AND c.email = :email"
    )
    Optional<Customer> findByEmailAndDeletedTrue(@Param("email") String email);

    @Query(
            nativeQuery = true,
            value = "SELECT EXISTS(SELECT 1 FROM customer WHERE email = :email)"
    )
    Boolean existsByEmail(String email);

    @Query(
            nativeQuery = true,
            value = "SELECT EXISTS(SELECT 1 FROM customer WHERE lower(email) = lower(:email))"
    )
    Boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query(
            nativeQuery = true,
            value = "SELECT EXISTS(SELECT 1 FROM customer WHERE email = :email AND deleted = false)"
    )
    Boolean existsByEmailAndDeletedFalse(String email);

    @Query(
            nativeQuery = true,
            value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_id = :customerId)"
    )
    Boolean existsByCustomerId(String customerId);

    @Query(
            nativeQuery = true,
            value = "SELECT EXISTS(SELECT 1 FROM customer WHERE customer_id = :customerId AND deleted = false)"
    )
    Boolean existsByCustomerIdAndDeletedFalse(String customerId);

    @Query("""
            SELECT c
            FROM Customer c
            WHERE c.deleted = false
              AND (
                   LOWER(c.customerId) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.companyName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.firstname, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.lastname, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CONCAT(COALESCE(c.firstname, ''), ' ', COALESCE(c.lastname, ''))) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR COALESCE(c.phone, '') LIKE CONCAT('%', :query, '%')
              )
              AND (:type IS NULL OR c.type = :type)
            ORDER BY c.companyName ASC, c.firstname ASC, c.lastname ASC
            """)
    List<Customer> basicSearch(@Param("query") String query, @Param("type") com.sni.bokaticowork.features.client.customer.enums.CustomerType type);

    @Query(nativeQuery = true, value = "UPDATE customer SET deleted = true WHERE customerId = :custoemrId ")
    void deleteByCustomerId(@Param("customerId") String customerId);

}
