package com.sni.bokaticowork.features.company.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * L'etat de compte d'une organisation ou d'un membre · ce qui a ete facture, regle, ce qui reste du
 * et depuis combien de temps.
 */
public final class StatementDtos {

    private StatementDtos() {
    }

    /** De qui l'etat parle · une organisation ({@code COMPANY}) ou un membre ({@code MEMBER}). */
    public enum Subject { COMPANY, MEMBER }

    /** Le compte qui porte une ligne · l'organisation elle-meme, un de ses membres, ou le membre seul. */
    public enum AccountType { COMPANY, MEMBER, PERSONAL }

    /** L'anciennete de ce qui reste du, selon l'echeance. */
    public record Aging(BigDecimal notDue, BigDecimal days1To30, BigDecimal days31To60,
                        BigDecimal days61To90, BigDecimal over90) {
    }

    /**
     * Les totaux d'une devise.
     *
     * <p>{@code invoiced} et {@code paid} portent sur les factures emises dans la periode ;
     * {@code balanceDue}, {@code overdue} et l'anciennete portent sur tout ce qui reste du a la
     * date de l'etat, periode ou non : une facture ancienne impayee ne disparait pas du compte.</p>
     */
    public record Totals(String currency,
                         BigDecimal invoiced,
                         BigDecimal paid,
                         BigDecimal balanceDue,
                         BigDecimal overdue,
                         /** Avoirs scelles pas encore consommes · ce que nous devons. */
                         BigDecimal creditAvailable,
                         /** Le du une fois les avoirs deduits · jamais negatif. */
                         BigDecimal netBalanceDue,
                         int openInvoices,
                         int overdueInvoices,
                         Aging aging) {
    }

    /** Ce que porte chaque compte · pour une organisation, chacun de ses membres. */
    public record AccountBreakdown(AccountType accountType,
                                   String accountCode,
                                   String accountName,
                                   String currency,
                                   BigDecimal invoiced,
                                   BigDecimal paid,
                                   BigDecimal balanceDue,
                                   BigDecimal overdue,
                                   int documentCount) {
    }

    public record Line(String documentNumber,
                       String documentType,
                       String status,
                       LocalDate issueDate,
                       LocalDate dueDate,
                       String currency,
                       AccountType accountType,
                       String accountCode,
                       String accountName,
                       /** AT_ISSUE, ATTACHED, ou null pour un document de l'organisation elle-meme. */
                       String companyLinkSource,
                       String companyCustomerCode,
                       String companyName,
                       BigDecimal totalAmount,
                       BigDecimal paidAmount,
                       BigDecimal balanceDue,
                       /** Effet signe sur le solde · avoir disponible en negatif, document clos a zero. */
                       BigDecimal impact,
                       /** Jours de retard a la date de l'etat · 0 si rien n'est echu. */
                       long daysOverdue,
                       /** Faux pour une facture d'avant la periode, reprise parce qu'elle reste due. */
                       boolean inPeriod) {
    }

    public record Statement(Subject subject,
                            String code,
                            String name,
                            String billingEmail,
                            LocalDate from,
                            LocalDate to,
                            LocalDate asOf,
                            BigDecimal creditLimit,
                            Integer paymentTermsDays,
                            boolean blocked,
                            String blockedReason,
                            List<Totals> totals,
                            List<AccountBreakdown> accounts,
                            List<Line> lines) {
    }
}
