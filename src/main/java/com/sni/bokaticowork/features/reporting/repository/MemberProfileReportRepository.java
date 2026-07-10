package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MemberProfileReportRepository {

    @PersistenceContext
    private EntityManager em;

    public Object[] memberProfile(String memberCode) {
        Query q = em.createNativeQuery("""
                SELECT
                    m.member_id, m.firstname, m.lastname, m.email, m.phone, m.whatsapp_phone,
                    m.member_status, m.portal_access, m.created_at,
                    c.customer_id, c.company_name,
                    mp.job_title, mp.company_role, mp.birth_date, mp.city, mp.country, mp.photo_url
                FROM member m
                JOIN customer c ON c.id = m.customer_id
                LEFT JOIN member_profile mp ON mp.member_id = m.id
                WHERE m.member_id = :memberCode AND m.deleted = false
                """);
        q.setParameter("memberCode", memberCode);
        Object result = q.getSingleResult();
        return result instanceof Object[] arr ? arr : new Object[]{result};
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberSubscriptions(String memberCode) {
        Query q = em.createNativeQuery("""
                SELECT
                    s.subscription_number, sp.name AS plan_name, s.status, s.billing_cycle,
                    s.total_amount, s.currency, s.start_date, s.current_period_end, s.auto_renew
                FROM subscription s
                JOIN subscription_plan_version spv ON spv.id = s.plan_version_id
                JOIN subscription_plan sp ON sp.id = spv.plan_id
                WHERE s.subscriber_type = 'MEMBER' AND s.subscriber_code = :memberCode
                ORDER BY s.created_at DESC
                """);
        q.setParameter("memberCode", memberCode);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberBookings(String memberCode, int limit) {
        Query q = em.createNativeQuery("""
                SELECT
                    b.booking_number, r.name AS resource_name, b.status,
                    DATE(b.started_at), b.duration_minutes, b.total_amount, b.currency
                FROM booking b
                JOIN resource r ON r.id = b.resource_id
                WHERE b.owner_type = 'MEMBER' AND b.owner_code = :memberCode AND b.deleted = false
                ORDER BY b.started_at DESC
                LIMIT :limit
                """);
        q.setParameter("memberCode", memberCode);
        q.setParameter("limit", limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberInvoices(String memberCode, int limit) {
        Query q = em.createNativeQuery("""
                SELECT
                    bd.document_number, bd.status, bd.total_amount, bd.paid_amount,
                    bd.balance_due, bd.currency, bd.issue_date, bd.due_date
                FROM billing_document bd
                WHERE bd.customer_type = 'MEMBER' AND bd.customer_code = :memberCode
                  AND bd.document_type = 'INVOICE'
                ORDER BY bd.created_at DESC
                LIMIT :limit
                """);
        q.setParameter("memberCode", memberCode);
        q.setParameter("limit", limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberPayments(String memberCode, int limit) {
        Query q = em.createNativeQuery("""
                SELECT
                    pt.transaction_number, pt.payment_method, pt.amount,
                    pt.currency, pt.status, DATE(pt.paid_at)
                FROM payment_transaction pt
                JOIN payment_intent pi ON pi.id = pt.payment_intent_id
                WHERE pi.customer_type = 'MEMBER' AND pi.customer_code = :memberCode
                ORDER BY pt.paid_at DESC NULLS LAST
                LIMIT :limit
                """);
        q.setParameter("memberCode", memberCode);
        q.setParameter("limit", limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberWallet(String memberCode) {
        Query q = em.createNativeQuery("""
                SELECT
                    wa.wallet_number, wa.available_balance, wa.ledger_balance,
                    wa.held_balance, wa.currency, wa.status
                FROM wallet_account wa
                WHERE wa.owner_type = 'MEMBER' AND wa.owner_code = :memberCode
                """);
        q.setParameter("memberCode", memberCode);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberContracts(String memberCode) {
        Query q = em.createNativeQuery("""
                SELECT
                    cr.contract_code, cr.title, cr.status,
                    cr.start_date, cr.end_date, cr.renewal_type, DATE(cr.signed_at)
                FROM contract_record cr
                WHERE cr.owner_type = 'MEMBER' AND cr.owner_code = :memberCode AND cr.deleted = false
                ORDER BY cr.created_at DESC
                """);
        q.setParameter("memberCode", memberCode);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> memberTickets(String memberCode) {
        Query q = em.createNativeQuery("""
                SELECT
                    st.ticket_number, st.title, st.status, st.priority,
                    st.category, DATE(st.created_at), DATE(st.resolved_at)
                FROM support_ticket st
                WHERE st.owner_type = 'MEMBER' AND st.owner_code = :memberCode
                ORDER BY st.created_at DESC
                """);
        q.setParameter("memberCode", memberCode);
        return q.getResultList();
    }
}
