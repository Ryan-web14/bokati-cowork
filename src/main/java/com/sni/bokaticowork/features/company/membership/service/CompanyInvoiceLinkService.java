package com.sni.bokaticowork.features.company.membership.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relier d'anciennes factures de membres a leur organisation · lien seulement.
 *
 * <p>Le document emis ne change pas : ni nom, ni PDF scelle. La facture rejoint seulement le
 * compte de l'organisation (encours, relances, etat de compte). C'est la decision prise : une
 * facture emise ne se modifie pas, et la reemettre au nom de l'organisation aurait demande un avoir.</p>
 *
 * <p>Toujours en deux temps : une previsualisation dit ce qui serait relie et ce qui ne le sera
 * pas, avec la raison ; le rattachement n'ecrit que les factures admissibles.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyInvoiceLinkService {

    public static final String SOURCE_AT_ISSUE = "AT_ISSUE";
    public static final String SOURCE_ATTACHED = "ATTACHED";

    private final BillingDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final CustomerMemberLinkRepository linkRepository;

    /** Les factures a relier · soit par numeros, soit toutes celles d'un membre sur une periode. */
    public record LinkRequest(@NotBlank String companyCustomerCode,
                              List<String> documentNumbers,
                              String memberId,
                              LocalDate from,
                              LocalDate to) {
    }

    public record Line(String documentNumber, String memberId, String memberName, LocalDate issueDate,
                       BigDecimal totalAmount, BigDecimal balanceDue, String status, boolean eligible, String reason) {
    }

    public record Result(String companyCustomerCode, String companyName, int eligible, int linked,
                         BigDecimal eligibleBalance, List<Line> lines) {
    }

    @Transactional(readOnly = true)
    public Result preview(LinkRequest request) {
        Customer company = company(request.companyCustomerCode());
        List<Line> lines = candidates(request).stream().map(document -> evaluate(document, company)).toList();
        return result(company, lines, 0);
    }

    public Result link(LinkRequest request, String actor) {
        Customer company = company(request.companyCustomerCode());
        List<Line> lines = new ArrayList<>();
        int linked = 0;
        for (BillingDocument document : candidates(request)) {
            Line line = evaluate(document, company);
            lines.add(line);
            if (!line.eligible()) {
                continue;
            }
            document.setCompanyCustomerId(company.getId());
            document.setCompanyCustomerCode(company.getCustomerId());
            document.setCompanyLinkSource(SOURCE_ATTACHED);
            document.setCompanyLinkedAt(Instant.now());
            document.setCompanyLinkedBy(actor);
            documentRepository.save(document);
            linked++;
        }
        return result(company, lines, linked);
    }

    /** Retire un lien pose apres coup · un lien de l'emission est imprime, il ne se retire pas. */
    public void unlink(String documentNumber) {
        BillingDocument document = documentRepository.findByDocumentNumber(documentNumber == null ? "" : documentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + documentNumber));
        if (!StringUtils.hasText(document.getCompanyCustomerCode())) {
            throw new BadRequestException("Cette facture n'est reliée à aucune organisation.");
        }
        if (SOURCE_AT_ISSUE.equals(document.getCompanyLinkSource())) {
            throw new BadRequestException("Cette facture a été émise pour le compte de " + document.getCompanyName()
                    + " et le porte imprimé : le lien ne se retire pas. Corrigez par un avoir.");
        }
        document.setCompanyCustomerId(null);
        document.setCompanyCustomerCode(null);
        document.setCompanyLinkSource(null);
        document.setCompanyLinkedAt(null);
        document.setCompanyLinkedBy(null);
        documentRepository.save(document);
    }

    // ── Admissibilite ────────────────────────────────────────────────────────

    private Line evaluate(BillingDocument document, Customer company) {
        String reason = null;
        Member member = "MEMBER".equalsIgnoreCase(document.getCustomerType())
                ? memberRepository.findByMemberIdIncludingArchived(document.getCustomerCode()).orElse(null)
                : null;
        if (!"MEMBER".equalsIgnoreCase(document.getCustomerType())) {
            reason = "La facture n'est pas au nom d'un membre.";
        } else if (!BillingReceivables.RECEIVABLE_TYPES.contains(document.getDocumentType())) {
            reason = "Seules les factures se relient, pas les devis ni les avoirs.";
        } else if (document.getStatus() == BillingDocumentStatus.DRAFT) {
            reason = "Brouillon : il sera relié à son émission selon le mode du membre.";
        } else if (member == null || !linkRepository.existsByCustomerAndMember(company, member)) {
            reason = "Le membre n'a jamais été rattaché à cette organisation.";
        } else if (StringUtils.hasText(document.getCompanyCustomerCode())) {
            reason = company.getCustomerId().equals(document.getCompanyCustomerCode())
                    ? "Déjà reliée à cette organisation."
                    : "Déjà reliée à une autre organisation (" + document.getCompanyCustomerCode() + ").";
        }
        return new Line(document.getDocumentNumber(), document.getCustomerCode(), document.getCustomerName(),
                document.getIssueDate(), document.getTotalAmount(), document.getBalanceDue(),
                document.getStatus() == null ? null : document.getStatus().name(), reason == null, reason);
    }

    private List<BillingDocument> candidates(LinkRequest request) {
        if (request.documentNumbers() != null && !request.documentNumbers().isEmpty()) {
            Map<String, BillingDocument> found = new HashMap<>();
            for (String number : request.documentNumbers()) {
                if (StringUtils.hasText(number)) {
                    documentRepository.findByDocumentNumber(number.trim()).ifPresent(d -> found.put(d.getDocumentNumber(), d));
                }
            }
            List<String> missing = request.documentNumbers().stream()
                    .filter(StringUtils::hasText).map(String::trim).filter(n -> !found.containsKey(n)).toList();
            if (!missing.isEmpty()) {
                throw new ResourceNotFoundException("Factures introuvables : " + String.join(", ", missing));
            }
            return request.documentNumbers().stream().filter(StringUtils::hasText).map(String::trim)
                    .distinct().map(found::get).toList();
        }
        if (!StringUtils.hasText(request.memberId()) || request.from() == null || request.to() == null) {
            throw new BadRequestException("Donnez les numéros de factures, ou un membre et une période (memberId, from, to).");
        }
        if (request.to().isBefore(request.from())) {
            throw new BadRequestException("La période se termine avant de commencer.");
        }
        return documentRepository.findMemberInvoicesBetween(request.memberId().trim(), request.from(), request.to(),
                BillingReceivables.RECEIVABLE_TYPES);
    }

    private Result result(Customer company, List<Line> lines, int linked) {
        BigDecimal balance = lines.stream().filter(Line::eligible)
                .map(line -> line.balanceDue() == null ? BigDecimal.ZERO : line.balanceDue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(company.getCustomerId(), company.getCompanyName(),
                (int) lines.stream().filter(Line::eligible).count(), linked, balance, lines);
    }

    private Customer company(String customerId) {
        Customer company = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + customerId));
        if (company.getType() != CustomerType.COMPANY) {
            throw new BadRequestException("Le client " + customerId + " n'est pas une organisation.");
        }
        return company;
    }
}
