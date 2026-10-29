package com.sni.bokaticowork.features.company.membership.repository;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerMemberLinkRepository extends JpaRepository<CustomerMemberLink, Long> {

    Optional<CustomerMemberLink> findByShortId(Long shortId);

    List<CustomerMemberLink> findAllByCustomerAndStatusOrderByStartedAtDesc(Customer customer, MemberLinkStatus status);

    List<CustomerMemberLink> findAllByCustomerOrderByStartedAtDesc(Customer customer);

    List<CustomerMemberLink> findAllByMemberOrderByStartedAtDesc(Member member);

    List<CustomerMemberLink> findAllByMemberAndStatusOrderByStartedAtDesc(Member member, MemberLinkStatus status);

    Optional<CustomerMemberLink> findByCustomerAndMemberAndStatus(Customer customer, Member member, MemberLinkStatus status);

    boolean existsByCustomerAndMember(Customer customer, Member member);

    /** Le lien actif en mode automatique du membre · il n'y en a qu'un au plus. */
    @Query("""
            SELECT l FROM CustomerMemberLink l
            WHERE l.member = :member AND l.status = com.sni.bokaticowork.features.company.enums.MemberLinkStatus.ACTIVE
              AND l.invoiceLinkMode = :mode
            """)
    List<CustomerMemberLink> findActiveWithMode(@Param("member") Member member, @Param("mode") InvoiceLinkMode mode);

    /** Les codes des organisations dont le membre est rattache et actif · pour la couverture d'abonnement. */
    @Query("""
            SELECT l.customer.customerId FROM CustomerMemberLink l
            WHERE l.member.memberId = :memberId
              AND l.status = com.sni.bokaticowork.features.company.enums.MemberLinkStatus.ACTIVE
            """)
    List<String> findActiveCompanyCodes(@Param("memberId") String memberId);

    long countByCustomerAndStatus(Customer customer, MemberLinkStatus status);
}
