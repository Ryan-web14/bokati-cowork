package com.sni.bokaticowork.features.client.member.repository.repo;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    Boolean existsByPhoneAndDeletedFalse(String phone);

    Boolean existsByMemberIdAndDeletedFalse(String memberId);

    Optional<Member> findByMemberIdAndDeletedFalse(String memberId);

    Optional<Member> findByEmailIgnoreCaseAndDeletedFalse(String email);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                      AND m.member_id = :memberId
                    LIMIT 1
                    """
    )
    Optional<Member> findVisibleByMemberId(@Param("memberId") String memberId);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                      AND lower(m.email) = lower(:email)
                    LIMIT 1
                    """
    )
    Optional<Member> findVisibleByEmail(@Param("email") String email);

    @Query(nativeQuery = true, value = "SELECT * FROM member WHERE deleted = false AND phone = :phone LIMIT 1")
    Optional<Member> findByPhoneAndDeletedFalse(@Param("phone") String phone);

    Optional<Member> findByUser_IdAndDeletedFalse(Long userId);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                      AND m.user_id = :userId
                    LIMIT 1
                    """
    )
    Optional<Member> findVisibleByUserId(@Param("userId") Long userId);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                    ORDER BY m.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                    """
    )
    Page<Member> findAllVisible(Pageable pageable);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM search_member(:query) sm
                    JOIN member m ON m.id = sm.id
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                    ORDER BY sm.score DESC
                    """
    )
    List<Member> basicSearch(@Param("query") String query);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT sm.member_id
                    FROM search_member_advanced(:query) sm
                    JOIN member m ON m.member_id = sm.member_id
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    LEFT JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND (m.customer_id IS NULL OR c.id IS NOT NULL)
                    """
    )
    List<String> fuzzySearchMemberIds(@Param("query") String query);

    Page<Member> findAllByCustomerAndDeletedFalse(Customer customer, Pageable pageable);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND c.customer_id = :customerId
                    ORDER BY m.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM member m
                    JOIN users u ON u.id = m.user_id AND u.deleted = false
                    JOIN customer c ON c.id = m.customer_id AND c.deleted = false
                    WHERE m.deleted = false
                      AND c.customer_id = :customerId
                    """
    )
    Page<Member> findAllVisibleByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE member
            SET deleted = true,
                member_status = 'ARCHIVED',
                portal_access = false,
                updated_at = NOW()
            WHERE deleted = false
              AND customer_id = (SELECT id FROM customer WHERE customer_id = :customerId)
            """)
    void suppressAllByCustomerId(@Param("customerId") String customerId);
}
