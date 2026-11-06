package com.sni.bokaticowork.features.company.contracts;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.service.support.CompanyLegalCard;
import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import com.sni.bokaticowork.features.contract.model.ContractParty;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentRepository;
import com.sni.bokaticowork.features.contract.repository.ContractPartyRepository;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Tous les contrats d'une organisation, sur sa fiche · abonnements, passes, contrat-cadre,
 * avenants, domiciliation. Et les collaborateurs beneficiaires du contrat d'un abonnement partage.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyContractsService {

    private final ContractRepository contractRepository;
    private final ContractPartyRepository partyRepository;
    private final ContractAmendmentRepository amendmentRepository;
    private final DomiciliationContractRepository domiciliationRepository;
    private final DocumentService documentService;
    private final CompanyLegalCard legalCard;

    public record AmendmentView(String code, String status, String description, LocalDate effectiveDate, Instant signedAt,
                                boolean hasDocument) {
    }

    public record PartyView(String role, String partyType, String partyCode, String displayName, boolean mustSign) {
    }

    public record ContractView(String contractCode, String title, String templateCode, String status, LocalDate startDate,
                               LocalDate endDate, Instant signedAt, boolean hasDocument, List<PartyView> parties,
                               List<AmendmentView> amendments) {
    }

    public record SignatoryView(String name, String title, String email, String memberId, boolean legalRepresentative) {
    }

    public record CompanyContracts(String companyCustomerCode, List<SignatoryView> signatories, List<ContractView> contracts,
                                   List<DomiciliationDtos.ContractView> domiciliations) {
    }

    @Transactional(readOnly = true)
    public CompanyContracts list(Customer company) {
        List<ContractView> contracts = contractRepository
                .findAllByOwnerTypeAndOwnerCode(DocumentOwnerType.CUSTOMER, company.getCustomerId(), Pageable.unpaged())
                .stream().map(this::view).toList();
        List<DomiciliationDtos.ContractView> domiciliations = domiciliationRepository
                .findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.CUSTOMER,
                        company.getCustomerId())
                .stream().map(DomiciliationDtos.ContractView::of).toList();
        List<SignatoryView> signatories = legalCard.signatories(company).stream()
                .map(s -> new SignatoryView(s.name(), s.title(), s.email(), s.memberId(), s.legalRepresentative())).toList();
        return new CompanyContracts(company.getCustomerId(), signatories, contracts, domiciliations);
    }

    /** Le document du contrat, signe ou a defaut genere · 404 s'il n'est pas a l'organisation. */
    @Transactional(readOnly = true)
    public byte[] document(Customer company, String contractCode) {
        Contract contract = owned(company, contractCode);
        String code = StringUtils.hasText(contract.getSignedDocumentCode()) ? contract.getSignedDocumentCode()
                : contract.getDraftDocumentCode();
        if (!StringUtils.hasText(code)) {
            throw new ResourceNotFoundException("Document pas encore disponible");
        }
        return documentService.downloadFile(code);
    }

    // ── Beneficiaires du contrat d'un abonnement partage ─────────────────────

    /**
     * Le collaborateur qui recoit une place devient partie {@code BENEFICIARY} du contrat de
     * l'abonnement · sans signer. Sans contrat (pas encore genere), rien.
     */
    public void addBeneficiary(String contractCode, Member member) {
        if (!StringUtils.hasText(contractCode) || member == null) {
            return;
        }
        contractRepository.findByContractCode(contractCode).ifPresent(contract -> {
            boolean present = partyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract).stream()
                    .anyMatch(p -> p.getRole() == ContractPartyRole.BENEFICIARY && member.getMemberId().equalsIgnoreCase(p.getPartyCode()));
            if (present) {
                return;
            }
            partyRepository.save(ContractParty.builder()
                    .contract(contract)
                    .partyType(DocumentOwnerType.MEMBER)
                    .partyId(member.getId())
                    .partyCode(member.getMemberId())
                    .displayName(member.getDisplayName())
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .role(ContractPartyRole.BENEFICIARY)
                    .mustSign(Boolean.FALSE)
                    .build());
        });
    }

    /** La place liberee · le collaborateur n'est plus beneficiaire du contrat. */
    public void removeBeneficiary(String contractCode, String memberCode) {
        if (!StringUtils.hasText(contractCode) || !StringUtils.hasText(memberCode)) {
            return;
        }
        contractRepository.findByContractCode(contractCode).ifPresent(contract ->
                partyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract).stream()
                        .filter(p -> p.getRole() == ContractPartyRole.BENEFICIARY && memberCode.equalsIgnoreCase(p.getPartyCode()))
                        .forEach(partyRepository::delete));
    }

    private ContractView view(Contract contract) {
        List<PartyView> parties = partyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract).stream()
                .map(p -> new PartyView(p.getRole() == null ? null : p.getRole().name(),
                        p.getPartyType() == null ? null : p.getPartyType().name(), p.getPartyCode(), p.getDisplayName(),
                        Boolean.TRUE.equals(p.getMustSign())))
                .toList();
        List<AmendmentView> amendments = amendmentRepository.findAllByOriginalContractCodeOrderByCreatedAtDesc(contract.getContractCode())
                .stream().map(CompanyContractsService::amendment).toList();
        return new ContractView(contract.getContractCode(), contract.getTitle(), contract.getTemplateCode(),
                contract.getStatus() == null ? null : contract.getStatus().name(), contract.getStartDate(), contract.getEndDate(),
                contract.getSignedAt(), StringUtils.hasText(contract.getSignedDocumentCode()) || StringUtils.hasText(contract.getDraftDocumentCode()),
                parties, amendments);
    }

    private static AmendmentView amendment(ContractAmendment amendment) {
        return new AmendmentView(amendment.getCode(), amendment.getStatus() == null ? null : amendment.getStatus().name(),
                amendment.getDescription(), amendment.getEffectiveDate(), amendment.getSignedAt(),
                StringUtils.hasText(amendment.getSignedDocumentCode()) || StringUtils.hasText(amendment.getDraftDocumentCode()));
    }

    private Contract owned(Customer company, String contractCode) {
        return contractRepository.findByContractCode(contractCode == null ? "" : contractCode.trim())
                .filter(c -> c.getOwnerType() == DocumentOwnerType.CUSTOMER && company.getCustomerId().equalsIgnoreCase(c.getOwnerCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable"));
    }
}
