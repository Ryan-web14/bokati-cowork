package com.sni.bokaticowork.features.domiciliation.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.ContractView;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.MailEventView;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.MailItemView;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.service.DomiciliationCompanyAccess;
import com.sni.bokaticowork.features.domiciliation.service.MailItemService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * La domiciliation vue par le domicilie · son contrat, son attestation, son courrier.
 *
 * <p>Le domicilie est un membre (a son nom), ou une organisation : son gerant et les membres
 * autorises a retirer le courrier ({@code can_collect_mail}) la voient alors. Lecture seule : les
 * etapes du contrat sont des gestes de l'etablissement, et le courrier se remet au guichet.</p>
 */
@RestController
@RequiredArgsConstructor
public class ClientDomiciliationController {

    private final ClientContextService clientContextService;
    private final DomiciliationContractRepository contractRepository;
    private final MailItemService mailItemService;
    private final DomiciliationCompanyAccess companyAccess;
    private final DocumentService documentService;

    /** Mes domiciliations · a mon nom, et celles des organisations dont je vois le courrier. */
    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts")
    public ResponseEntity<List<ContractView>> contracts() {
        Member member = clientContextService.getAuthenticatedMember();
        List<DomiciliationContract> contracts = new ArrayList<>(contractRepository
                .findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.MEMBER, member.getMemberId()));
        for (String company : companyAccess.companiesFor(member)) {
            contracts.addAll(contractRepository
                    .findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.CUSTOMER, company));
        }
        return ResponseEntity.ok(contracts.stream().map(ContractView::of).toList());
    }

    /** La domiciliation d'une organisation · pour son gerant et les membres autorises a retirer le courrier. */
    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/domiciliation")
    public ResponseEntity<List<ContractView>> companyDomiciliation(@PathVariable String customerId) {
        Member member = clientContextService.getAuthenticatedMember();
        if (companyAccess.companiesFor(member).stream().noneMatch(code -> code.equalsIgnoreCase(customerId))) {
            throw new ResourceNotFoundException("Organisation introuvable");
        }
        return ResponseEntity.ok(contractRepository
                .findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.CUSTOMER, customerId)
                .stream().map(ContractView::of).toList());
    }

    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts/{contractNumber}")
    public ResponseEntity<ContractView> contract(@PathVariable String contractNumber) {
        return ResponseEntity.ok(ContractView.of(owned(contractNumber)));
    }

    /** Le contrat signe, ou a defaut celui genere pour la signature. */
    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts/{contractNumber}/document")
    public ResponseEntity<byte[]> contractDocument(@PathVariable String contractNumber) {
        DomiciliationContract contract = owned(contractNumber);
        String code = StringUtils.hasText(contract.getSignedDocumentCode()) ? contract.getSignedDocumentCode() : contract.getContractDocumentCode();
        return pdf(code, "contrat-" + contract.getContractNumber() + ".pdf");
    }

    /** L'attestation de domiciliation en vigueur. */
    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts/{contractNumber}/certificate")
    public ResponseEntity<byte[]> certificate(@PathVariable String contractNumber) {
        DomiciliationContract contract = owned(contractNumber);
        if (!contract.certificateInForce()) {
            throw new ResourceNotFoundException("Aucune attestation en vigueur");
        }
        return pdf(contract.getCertificateDocumentCode(), "attestation-" + contract.getContractNumber() + ".pdf");
    }

    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts/{contractNumber}/mail")
    public ResponseEntity<PaginatedResponse<MailItemView>> mail(@PathVariable String contractNumber,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        DomiciliationContract contract = owned(contractNumber);
        return ResponseEntity.ok(new PaginatedResponse<>(mailItemService
                .ofContract(contract.getContractNumber(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(MailItemView::of)));
    }

    @GetMapping(ApiPath.V1 + "/client/domiciliation/contracts/{contractNumber}/mail/{itemNumber}/trail")
    public ResponseEntity<List<MailEventView>> mailTrail(@PathVariable String contractNumber, @PathVariable String itemNumber) {
        DomiciliationContract contract = owned(contractNumber);
        if (!mailItemService.get(itemNumber).getContract().getId().equals(contract.getId())) {
            throw new ResourceNotFoundException("Pli introuvable");
        }
        return ResponseEntity.ok(mailItemService.trail(itemNumber).stream().map(MailEventView::of).toList());
    }

    private ResponseEntity<byte[]> pdf(String documentCode, String fileName) {
        if (!StringUtils.hasText(documentCode)) {
            throw new ResourceNotFoundException("Document pas encore disponible");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(documentService.downloadFile(documentCode));
    }

    /** Le contrat, si le membre y a acces · 404 sinon, sans dire qu'il existe. */
    private DomiciliationContract owned(String contractNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        DomiciliationContract contract = contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable"));
        if (!companyAccess.mayAccess(member, contract)) {
            throw new ResourceNotFoundException("Contrat introuvable");
        }
        return contract;
    }
}
