package com.sni.bokaticowork.features.client.member.repository.repo;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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

    Optional<Member> findByUser_IdAndDeletedFalse(Long userId);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT m.*
                    FROM search_member(:query) sm
                    JOIN member m ON m.id = sm.id
                    WHERE m.deleted = false
                    ORDER BY sm.score DESC
                    """
    )
    List<Member> basicSearch(@Param("query") String query);

    Page<Member> findAllByCustomerAndDeletedFalse(Customer customer, Pageable pageable);
}
