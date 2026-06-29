package com.sni.bokaticowork.features.crm.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.crm.dto.ProposalDtos.*;
import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import com.sni.bokaticowork.features.crm.model.CommercialProposal;
import com.sni.bokaticowork.features.crm.model.ProposalLineItem;
import com.sni.bokaticowork.features.crm.repository.ProposalLineRepository;
import com.sni.bokaticowork.features.crm.repository.ProposalRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.ProposalService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProposalServiceImpl implements ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProposalLineRepository proposalLineRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    // ── CRUD ─────────────────────────────────────────────────────

    @Override
    public ProposalResponse create(CreateProposalRequest request) {
        CommercialProposal proposal = CommercialProposal.builder()
                .proposalNumber(sequenceGenerator.next("crm_proposal"))
                .title(request.title().trim())
                .recipientType(request.recipientType())
                .recipientCode(request.recipientCode())
                .recipientName(request.recipientName())
                .recipientEmail(request.recipientEmail())
                .status(ProposalStatus.DRAFT)
                .validUntil(request.validUntil())
                .currency(request.currency())
                .notes(request.notes())
                .internalNotes(request.internalNotes())
                .assignedTo(request.assignedTo())
                .build();

        if (request.opportunityId() != null) {
            proposal.setOpportunity(
                    new com.sni.bokaticowork.features.crm.model.Opportunity());
            proposal.getOpportunity().setId(request.opportunityId());
        }
        if (request.leadId() != null) {
            proposal.setLead(
                    new com.sni.bokaticowork.features.crm.model.Lead());
            proposal.getLead().setId(request.leadId());
        }

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ProposalResponse update(Long id, UpdateProposalRequest request) {
        CommercialProposal proposal = findOrThrow(id);

        if (StringUtils.hasText(request.title()))        proposal.setTitle(request.title().trim());
        if (request.recipientName() != null)             proposal.setRecipientName(request.recipientName());
        if (request.recipientEmail() != null)            proposal.setRecipientEmail(request.recipientEmail());
        if (request.validUntil() != null)                proposal.setValidUntil(request.validUntil());
        if (request.notes() != null)                     proposal.setNotes(request.notes());
        if (request.internalNotes() != null)             proposal.setInternalNotes(request.internalNotes());
        if (request.assignedTo() != null)                proposal.setAssignedTo(request.assignedTo());

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public ProposalResponse get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProposalResponse getByNumber(String proposalNumber) {
        CommercialProposal proposal = proposalRepository.findByProposalNumber(proposalNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalNumber));
        return toResponse(proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ProposalResponse> list(ProposalStatus status, Long opportunityId,
                                                    String searchText, Pageable pageable) {
        Specification<CommercialProposal> spec = buildSpecification(status, opportunityId, searchText);
        Page<ProposalResponse> page = proposalRepository.findAll(spec, pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    // ── Line management ──────────────────────────────────────────

    @Override
    public ProposalResponse addLine(Long proposalId, AddProposalLineRequest request) {
        CommercialProposal proposal = findOrThrow(proposalId);

        ProposalLineItem line = ProposalLineItem.builder()
                .proposal(proposal)
                .label(request.label().trim())
                .description(request.description())
                .serviceType(request.serviceType())
                .serviceRefCode(request.serviceRefCode())
                .quantity(request.quantity() != null ? request.quantity() : BigDecimal.ONE)
                .unitPrice(request.unitPrice())
                .discountPercent(request.discountPercent() != null ? request.discountPercent() : BigDecimal.ZERO)
                .taxRate(request.taxRate() != null ? request.taxRate() : BigDecimal.ZERO)
                .taxIncluded(request.taxIncluded() != null ? request.taxIncluded() : Boolean.FALSE)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .currency(request.currency())
                .build();

        calculateLineTotals(line);
        proposal.getLines().add(line);
        recalculateProposalTotals(proposal);

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ProposalResponse updateLine(Long proposalId, Long lineId, UpdateProposalLineRequest request) {
        CommercialProposal proposal = findOrThrow(proposalId);
        ProposalLineItem line = findLine(proposal, lineId);

        if (StringUtils.hasText(request.label()))     line.setLabel(request.label().trim());
        if (request.description() != null)            line.setDescription(request.description());
        if (request.quantity() != null)               line.setQuantity(request.quantity());
        if (request.unitPrice() != null)              line.setUnitPrice(request.unitPrice());
        if (request.discountPercent() != null)        line.setDiscountPercent(request.discountPercent());
        if (request.taxRate() != null)                line.setTaxRate(request.taxRate());
        if (request.taxIncluded() != null)            line.setTaxIncluded(request.taxIncluded());
        if (request.sortOrder() != null)              line.setSortOrder(request.sortOrder());

        calculateLineTotals(line);
        recalculateProposalTotals(proposal);

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ProposalResponse removeLine(Long proposalId, Long lineId) {
        CommercialProposal proposal = findOrThrow(proposalId);
        ProposalLineItem line = findLine(proposal, lineId);

        proposal.getLines().remove(line);
        recalculateProposalTotals(proposal);

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    // ── Status transitions ───────────────────────────────────────

    @Override
    public ProposalResponse send(Long id) {
        CommercialProposal proposal = findOrThrow(id);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT proposals can be sent");
        }
        proposal.setStatus(ProposalStatus.SENT);
        proposal.setSentAt(Instant.now());

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ProposalResponse accept(Long id) {
        CommercialProposal proposal = findOrThrow(id);
        if (proposal.getStatus() != ProposalStatus.SENT && proposal.getStatus() != ProposalStatus.VIEWED) {
            throw new BadRequestException("Only SENT or VIEWED proposals can be accepted");
        }
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(Instant.now());

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ProposalResponse reject(Long id, RejectProposalRequest request) {
        CommercialProposal proposal = findOrThrow(id);
        if (proposal.getStatus() != ProposalStatus.SENT && proposal.getStatus() != ProposalStatus.VIEWED) {
            throw new BadRequestException("Only SENT or VIEWED proposals can be rejected");
        }
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setRejectedAt(Instant.now());
        if (request != null && StringUtils.hasText(request.reason())) {
            proposal.setRejectionReason(request.reason());
        }

        proposal = proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    // ── Line calculation ─────────────────────────────────────────

    private void calculateLineTotals(ProposalLineItem line) {
        BigDecimal quantity = line.getQuantity();
        BigDecimal unitPrice = line.getUnitPrice();
        BigDecimal discountPercent = line.getDiscountPercent();
        BigDecimal taxRate = line.getTaxRate();
        boolean taxIncluded = Boolean.TRUE.equals(line.getTaxIncluded());

        BigDecimal rawSubtotal = quantity.multiply(unitPrice);
        BigDecimal discountAmount = rawSubtotal.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal afterDiscount = rawSubtotal.subtract(discountAmount);

        BigDecimal subtotal;
        BigDecimal tax;
        BigDecimal total;

        if (taxIncluded) {
            BigDecimal factor = BigDecimal.ONE.add(
                    taxRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            subtotal = afterDiscount.divide(factor, 4, RoundingMode.HALF_UP);
            tax = afterDiscount.subtract(subtotal);
            total = afterDiscount;
        } else {
            subtotal = afterDiscount;
            tax = afterDiscount.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            total = subtotal.add(tax);
        }

        line.setSubtotalAmount(subtotal);
        line.setTaxAmount(tax);
        line.setTotalAmount(total);
    }

    private void recalculateProposalTotals(CommercialProposal proposal) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (ProposalLineItem line : proposal.getLines()) {
            subtotal = subtotal.add(line.getSubtotalAmount());
            tax = tax.add(line.getTaxAmount());
            total = total.add(line.getTotalAmount());
        }

        proposal.setSubtotalAmount(subtotal);
        proposal.setTaxAmount(tax);
        proposal.setTotalAmount(total);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private CommercialProposal findOrThrow(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + id));
    }

    private ProposalLineItem findLine(CommercialProposal proposal, Long lineId) {
        return proposal.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Proposal line not found: " + lineId));
    }

    private Specification<CommercialProposal> buildSpecification(ProposalStatus status,
                                                                  Long opportunityId,
                                                                  String searchText) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (opportunityId != null) {
                predicates.add(cb.equal(root.get("opportunity").get("id"), opportunityId));
            }
            if (StringUtils.hasText(searchText)) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("proposalNumber")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("recipientName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── Mapping ──────────────────────────────────────────────────

    private ProposalResponse toResponse(CommercialProposal p) {
        List<ProposalLineResponse> lineResponses = p.getLines() != null
                ? p.getLines().stream().map(this::toLineResponse).collect(Collectors.toList())
                : List.of();

        return new ProposalResponse(
                p.getId(),
                p.getProposalNumber(),
                p.getTitle(),
                p.getOpportunity() != null ? p.getOpportunity().getId() : null,
                p.getLead() != null ? p.getLead().getId() : null,
                p.getRecipientType(),
                p.getRecipientCode(),
                p.getRecipientName(),
                p.getRecipientEmail(),
                p.getStatus(),
                p.getValidUntil(),
                p.getSubtotalAmount(),
                p.getTaxAmount(),
                p.getTotalAmount(),
                p.getCurrency(),
                p.getNotes(),
                p.getInternalNotes(),
                p.getConvertedInvoiceNumber(),
                p.getConvertedContractCode(),
                p.getConvertedSubscriptionNumber(),
                p.getConvertedPassNumber(),
                p.getSentAt(),
                p.getViewedAt(),
                p.getAcceptedAt(),
                p.getRejectedAt(),
                p.getRejectionReason(),
                p.getCreatedBy(),
                p.getAssignedTo(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                lineResponses
        );
    }

    private ProposalLineResponse toLineResponse(ProposalLineItem l) {
        return new ProposalLineResponse(
                l.getId(),
                l.getSortOrder(),
                l.getServiceType(),
                l.getServiceRefCode(),
                l.getLabel(),
                l.getDescription(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getDiscountPercent(),
                l.getTaxRate(),
                l.getTaxIncluded(),
                l.getSubtotalAmount(),
                l.getTaxAmount(),
                l.getTotalAmount(),
                l.getCurrency()
        );
    }
}
