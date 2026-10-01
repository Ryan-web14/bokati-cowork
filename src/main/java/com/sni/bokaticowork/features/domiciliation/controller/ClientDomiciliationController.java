package com.sni.bokaticowork.features.domiciliation.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.ContractView;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.MailEventView;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.MailItemView;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.service.MailItemService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * La domiciliation vue par le domicilie · son contrat, son attestation, son courrier.
 *
 * <p>Lecture seule : les etapes du contrat sont des gestes de l'etablissement, et le courrier se
 * remet au guichet. Ce que le domicilie fait ici, c'est savoir ou il en est, et voir ce qui
 * l'attend.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/domiciliation")
@RequiredArgsConstructor
public class ClientDomiciliationController {

    private final ClientContextService clientContextService;
    private final DomiciliationContractRepository contractRepository;
    private final MailItemService mailItemService;

    @GetMapping("/contracts")
    public ResponseEntity<List<ContractView>> contracts() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(contractRepository
                .findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.MEMBER, member.getMemberId())
                .stream().map(ContractView::of).toList());
    }

    @GetMapping("/contracts/{contractNumber}")
    public ResponseEntity<ContractView> contract(@PathVariable String contractNumber) {
        return ResponseEntity.ok(ContractView.of(owned(contractNumber)));
    }

    @GetMapping("/contracts/{contractNumber}/mail")
    public ResponseEntity<PaginatedResponse<MailItemView>> mail(@PathVariable String contractNumber,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        DomiciliationContract contract = owned(contractNumber);
        return ResponseEntity.ok(new PaginatedResponse<>(mailItemService
                .ofContract(contract.getContractNumber(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(MailItemView::of)));
    }

    @GetMapping("/contracts/{contractNumber}/mail/{itemNumber}/trail")
    public ResponseEntity<List<MailEventView>> mailTrail(@PathVariable String contractNumber, @PathVariable String itemNumber) {
        DomiciliationContract contract = owned(contractNumber);
        if (!mailItemService.get(itemNumber).getContract().getId().equals(contract.getId())) {
            throw new ResourceNotFoundException("Pli introuvable");
        }
        return ResponseEntity.ok(mailItemService.trail(itemNumber).stream().map(MailEventView::of).toList());
    }

    private DomiciliationContract owned(String contractNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        DomiciliationContract contract = contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable"));
        boolean mine = contract.getSubscription().getSubscriberType() == SubscriberType.MEMBER
                && member.getMemberId().equals(contract.getSubscription().getSubscriberCode());
        if (!mine) {
            throw new ResourceNotFoundException("Contrat introuvable");
        }
        return contract;
    }
}
