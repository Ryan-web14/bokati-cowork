package com.sni.bokaticowork.features.portal.document.contract.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractParty;
import com.sni.bokaticowork.features.contract.repository.ContractPartyRepository;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateDocumentSignatureRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SignDocumentRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentSignatureResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSignatureService;
import com.sni.bokaticowork.features.portal.document.contract.dto.request.ClientContractSignRequest;
import com.sni.bokaticowork.features.portal.document.contract.dto.response.ClientContractResponse;
import com.sni.bokaticowork.features.portal.document.contract.dto.response.ClientContractSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientContractService {

    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final DocumentService documentService;
    private final DocumentSignatureService documentSignatureService;

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientContractSummaryResponse> listContracts(Member member, Pageable pageable) {
        Page<Contract> page = contractRepository.findAllByOwnerTypeAndOwnerCode(
                DocumentOwnerType.MEMBER, member.getMemberId(), pageable);
        return new PaginatedResponse<>(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public ClientContractResponse getContract(Member member, String contractCode) {
        Contract contract = resolveOwnedContract(member, contractCode);
        return toDetailResponse(contract);
    }

    @Transactional(readOnly = true)
    public DocumentFileResult downloadPdf(Member member, String contractCode) {
        Contract contract = resolveOwnedContract(member, contractCode);
        String docCode = StringUtils.hasText(contract.getSignedDocumentCode())
                ? contract.getSignedDocumentCode()
                : contract.getDraftDocumentCode();
        if (!StringUtils.hasText(docCode)) {
            throw new BadRequestException("No PDF is available for this contract yet");
        }
        return documentService.getFileWithMeta(docCode);
    }

    @Transactional
    public ClientContractResponse signContract(Member member, String contractCode,
                                               ClientContractSignRequest request,
                                               String ipAddress, String userAgent) {
        Contract contract = resolveOwnedContract(member, contractCode);
        if (contract.getStatus() != ContractStatus.GENERATED
                && contract.getStatus() != ContractStatus.AWAITING_SIGNATURE) {
            throw new BadRequestException("Contract is not available for signing. Current status: " + contract.getStatus());
        }
        if (!StringUtils.hasText(contract.getDraftDocumentCode())) {
            throw new BadRequestException("Contract document has not been generated yet");
        }
        enforceSignOrder(contract, member);
        String documentCode = contract.getDraftDocumentCode();
        CreateDocumentSignatureRequest signatureRequest = new CreateDocumentSignatureRequest(
                "MEMBER",
                member.getId(),
                member.getDisplayName(),
                member.getEmail()
        );
        DocumentSignatureResponse sigResponse = documentSignatureService.requestSignature(documentCode, signatureRequest);
        documentSignatureService.sign(
                documentCode,
                sigResponse.id(),
                new SignDocumentRequest(request.isAccepted(), request.getSignatureData()),
                ipAddress,
                userAgent
        );
        contractService.markSigned(contractCode, documentCode);
        return toDetailResponse(contractService.serviceByCode(contractCode));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void enforceSignOrder(Contract contract, Member member) {
        List<ContractParty> parties = contractPartyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract);
        if (parties.isEmpty() || !StringUtils.hasText(contract.getDraftDocumentCode())) {
            return;
        }
        ContractParty currentParty = parties.stream()
                .filter(p -> p.getPartyType() == DocumentOwnerType.MEMBER
                        && member.getId().equals(p.getPartyId()))
                .findFirst()
                .orElse(null);
        if (currentParty == null || currentParty.getSignOrder() == null) {
            return;
        }
        List<DocumentSignatureResponse> signatures = documentSignatureService.list(contract.getDraftDocumentCode());
        boolean predecessorsAllSigned = parties.stream()
                .filter(p -> p.getSignOrder() != null && p.getSignOrder() < currentParty.getSignOrder())
                .filter(p -> Boolean.TRUE.equals(p.getMustSign()))
                .allMatch(p -> signatures.stream().anyMatch(
                        s -> p.getPartyType().name().equalsIgnoreCase(s.signerType())
                                && p.getPartyId().equals(s.signerId())
                                && s.signedAt() != null));
        if (!predecessorsAllSigned) {
            throw new BadRequestException("You cannot sign yet — previous signatories have not completed their signatures");
        }
    }

    private Contract resolveOwnedContract(Member member, String contractCode) {
        Contract contract = contractRepository.findByContractCode(contractCode)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        if (!DocumentOwnerType.MEMBER.equals(contract.getOwnerType())
                || !member.getMemberId().equals(contract.getOwnerCode())) {
            throw new ResourceNotFoundException("Contract not found");
        }
        return contract;
    }

    private ClientContractSummaryResponse toSummary(Contract contract) {
        return ClientContractSummaryResponse.builder()
                .contractCode(contract.getContractCode())
                .title(contract.getTitle())
                .status(contract.getStatus() != null ? contract.getStatus().name() : null)
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .signedAt(contract.getSignedAt())
                .hasPdf(StringUtils.hasText(contract.getDraftDocumentCode())
                        || StringUtils.hasText(contract.getSignedDocumentCode()))
                .build();
    }

    private ClientContractResponse toDetailResponse(Contract contract) {
        boolean canSign = (contract.getStatus() == ContractStatus.GENERATED
                || contract.getStatus() == ContractStatus.AWAITING_SIGNATURE)
                && StringUtils.hasText(contract.getDraftDocumentCode())
                && !StringUtils.hasText(contract.getSignedDocumentCode());
        boolean canDownload = StringUtils.hasText(contract.getDraftDocumentCode())
                || StringUtils.hasText(contract.getSignedDocumentCode());
        return ClientContractResponse.builder()
                .contractCode(contract.getContractCode())
                .title(contract.getTitle())
                .description(contract.getDescription())
                .status(contract.getStatus() != null ? contract.getStatus().name() : null)
                .renewalType(contract.getRenewalType() != null ? contract.getRenewalType().name() : null)
                .effectiveDate(contract.getEffectiveDate())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .signedAt(contract.getSignedAt())
                .activatedAt(contract.getActivatedAt())
                .terminatedAt(contract.getTerminatedAt())
                .terminationReason(contract.getTerminationReason())
                .draftDocumentCode(contract.getDraftDocumentCode())
                .signedDocumentCode(contract.getSignedDocumentCode())
                .canSign(canSign)
                .canDownloadPdf(canDownload)
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
}
