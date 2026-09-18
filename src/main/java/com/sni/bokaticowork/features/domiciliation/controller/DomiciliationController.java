package com.sni.bokaticowork.features.domiciliation.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.domiciliation.dto.DomiciliationDtos.*;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.service.DomiciliationAddressService;
import com.sni.bokaticowork.features.domiciliation.service.DomiciliationService;
import com.sni.bokaticowork.features.domiciliation.service.MailItemService;
import com.sni.bokaticowork.features.domiciliation.service.SubscribedServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogue de services, domiciliation et courrier · cote administration.
 *
 * <p>Chaque etape du cycle de vie est un point d'entree distinct, parce que chacune a son verrou et
 * son message. Un seul « avancer » qui deviendrait ce qu'il peut ne dirait jamais pourquoi il
 * n'avance pas.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class DomiciliationController {

    private final SubscribedServiceService subscribedServiceService;
    private final DomiciliationAddressService addressService;
    private final DomiciliationService domiciliationService;
    private final MailItemService mailItemService;

    // ---- Catalogue ------------------------------------------------------------------------

    @GetMapping("/catalogue")
    public ResponseEntity<List<ServiceDefinitionView>> catalogue(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(subscribedServiceService.catalogue(includeInactive).stream().map(ServiceDefinitionView::of).toList());
    }

    @PutMapping("/catalogue")
    public ResponseEntity<ServiceDefinitionView> saveDefinition(@Valid @RequestBody ServiceDefinitionRequest request) {
        return ResponseEntity.ok(ServiceDefinitionView.of(subscribedServiceService.saveDefinition(request.toEntity())));
    }

    // ---- Services souscrits ---------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<List<SubscriptionServiceView>> ofSubscription(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscribedServiceService.ofSubscription(subscriptionNumber).stream().map(SubscriptionServiceView::of).toList());
    }

    @PostMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<SubscriptionServiceView> subscribe(@PathVariable String subscriptionNumber,
                                                             @Valid @RequestBody SubscribeServiceRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(SubscriptionServiceView.of(subscribedServiceService.subscribe(subscriptionNumber, request.serviceCode(),
                request.quantity(), request.unitPrice(), request.metadataJson(), actor(authentication))));
    }

    @PostMapping("/subscribed/{serviceNumber}/suspend")
    public ResponseEntity<SubscriptionServiceView> suspend(@PathVariable String serviceNumber, @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(SubscriptionServiceView.of(subscribedServiceService.suspend(serviceNumber, request == null ? null : request.reason())));
    }

    @PostMapping("/subscribed/{serviceNumber}/terminate")
    public ResponseEntity<SubscriptionServiceView> terminateService(@PathVariable String serviceNumber) {
        return ResponseEntity.ok(SubscriptionServiceView.of(subscribedServiceService.terminate(serviceNumber)));
    }

    // ---- Registre des adresses -------------------------------------------------------------

    @GetMapping("/domiciliation/addresses")
    public ResponseEntity<List<AddressView>> addresses() {
        return ResponseEntity.ok(addressService.registry().stream()
                .map(entry -> AddressView.of(entry.address(), entry.occupants())).toList());
    }

    @PostMapping("/domiciliation/addresses")
    public ResponseEntity<AddressView> registerAddress(@Valid @RequestBody RegisterAddressRequest request) {
        return ResponseEntity.ok(AddressView.of(addressService.register(request.label(), request.address(),
                request.fiscalCapable() == null || request.fiscalCapable(), request.maxOccupants(), request.notes()), 0));
    }

    @PatchMapping("/domiciliation/addresses/{code}")
    public ResponseEntity<AddressView> updateAddress(@PathVariable String code, @RequestBody UpdateAddressRequest request) {
        var updated = addressService.update(code, request.label(), request.fiscalCapable(), request.maxOccupants(), request.active(), request.notes());
        long occupants = addressService.registry().stream().filter(e -> e.address().getId().equals(updated.getId()))
                .map(DomiciliationAddressService.AddressEntry::occupants).findFirst().orElse(0L);
        return ResponseEntity.ok(AddressView.of(updated, occupants));
    }

    // ---- Contrats de domiciliation --------------------------------------------------------

    @GetMapping("/domiciliation/contracts")
    public ResponseEntity<PaginatedResponse<ContractView>> contracts(@RequestParam(required = false) List<DomiciliationContract.Status> status,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(domiciliationService.list(status, unsorted(pageable)).map(ContractView::of)));
    }

    @GetMapping("/domiciliation/contracts/{contractNumber}")
    public ResponseEntity<ContractView> contract(@PathVariable String contractNumber) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.get(contractNumber)));
    }

    @PostMapping("/domiciliation/contracts")
    public ResponseEntity<ContractView> open(@Valid @RequestBody OpenContractRequest request, Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.open(new DomiciliationService.OpenOrder(
                request.subscriptionNumber(), request.legalName(), request.legalForm(), request.registrationNumber(), request.taxNumber(),
                request.legalRepresentativeName(), request.addressCode(), request.suiteNumber(), request.billingCycle(),
                request.commitmentMonths(), request.noticePeriodDays(), request.startDate(), request.mailForwardingMode()),
                actor(authentication))));
    }

    /** Verrou 1 · pieces verifiees, contrat genere. */
    @PostMapping("/domiciliation/contracts/{contractNumber}/prepare")
    public ResponseEntity<ContractView> prepare(@PathVariable String contractNumber, Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.prepareForSignature(contractNumber, actor(authentication))));
    }

    @PostMapping("/domiciliation/contracts/{contractNumber}/signed")
    public ResponseEntity<ContractView> signed(@PathVariable String contractNumber, @Valid @RequestBody SignedRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.markSigned(contractNumber, request.signedDocumentCode(), actor(authentication))));
    }

    @GetMapping("/domiciliation/contracts/{contractNumber}/registrations")
    public ResponseEntity<List<RegistrationView>> registrations(@PathVariable String contractNumber) {
        return ResponseEntity.ok(domiciliationService.registrations(contractNumber).stream().map(RegistrationView::of).toList());
    }

    @PostMapping("/domiciliation/contracts/{contractNumber}/registration/submit")
    public ResponseEntity<RegistrationView> submitRegistration(@PathVariable String contractNumber,
                                                               @RequestBody SubmitRegistrationRequest request, Authentication authentication) {
        return ResponseEntity.ok(RegistrationView.of(domiciliationService.submitRegistration(contractNumber,
                new DomiciliationService.RegistrationSubmission(request.administrationOffice(), request.stampDutyAmount(),
                        request.registrationFeeAmount(), request.paidBy(), request.rebilled()), actor(authentication))));
    }

    /** Verrou 2 · enregistrement obtenu, contrat actif. */
    @PostMapping("/domiciliation/contracts/{contractNumber}/registration/confirm")
    public ResponseEntity<ContractView> confirmRegistration(@PathVariable String contractNumber,
                                                            @Valid @RequestBody ConfirmRegistrationRequest request, Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.confirmRegistration(contractNumber,
                new DomiciliationService.RegistrationOutcome(request.administrationReference(), request.registrationDate(),
                        request.expiresAt(), request.receiptDocumentCode(), request.registeredDocumentCode()), actor(authentication))));
    }

    @PostMapping("/domiciliation/contracts/{contractNumber}/registration/reject")
    public ResponseEntity<RegistrationView> rejectRegistration(@PathVariable String contractNumber,
                                                               @Valid @RequestBody RejectRegistrationRequest request, Authentication authentication) {
        return ResponseEntity.ok(RegistrationView.of(domiciliationService.rejectRegistration(contractNumber, request.reason(), actor(authentication))));
    }

    /** Le tableau de suivi · la ou les dossiers s'enlisent. */
    @GetMapping("/domiciliation/registrations/in-progress")
    public ResponseEntity<PaginatedResponse<RegistrationView>> registrationsInProgress(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(domiciliationService.registrationsInProgress(unsorted(pageable)).map(RegistrationView::of)));
    }

    /** Verrou 3 · attestation fiscale seulement sous engagement annuel. */
    @PostMapping("/domiciliation/contracts/{contractNumber}/certificate")
    public ResponseEntity<ContractView> certificate(@PathVariable String contractNumber, @Valid @RequestBody CertificateRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.issueCertificate(contractNumber, request.scope(), actor(authentication))));
    }

    @PostMapping("/domiciliation/contracts/{contractNumber}/commitment")
    public ResponseEntity<ContractView> commitment(@PathVariable String contractNumber, @RequestBody CommitmentRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.changeCommitment(contractNumber, request.billingCycle(),
                request.commitmentMonths(), actor(authentication))));
    }

    @PostMapping("/domiciliation/contracts/{contractNumber}/terminate")
    public ResponseEntity<ContractView> terminate(@PathVariable String contractNumber, @Valid @RequestBody TerminateRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(ContractView.of(domiciliationService.terminate(contractNumber, request.reason(), request.effectiveDate(), actor(authentication))));
    }

    @GetMapping(value = "/domiciliation/registry", produces = "text/csv")
    public ResponseEntity<byte[]> registry() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registre-domicilies.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(domiciliationService.registryCsv());
    }

    // ---- Courrier ---------------------------------------------------------------------------

    @PostMapping("/domiciliation/contracts/{contractNumber}/mail")
    public ResponseEntity<MailItemView> receiveMail(@PathVariable String contractNumber, @Valid @RequestBody ReceiveMailRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.receive(contractNumber,
                new MailItemService.Receipt(request.mailType(), request.senderName(), request.senderReference(), request.weightGrams(),
                        request.dimensions(), request.notes()), actor(authentication))));
    }

    @GetMapping("/domiciliation/contracts/{contractNumber}/mail")
    public ResponseEntity<PaginatedResponse<MailItemView>> contractMail(@PathVariable String contractNumber,
                                                                        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(mailItemService.ofContract(contractNumber, unsorted(pageable)).map(MailItemView::of)));
    }

    @GetMapping("/domiciliation/mail/pending")
    public ResponseEntity<PaginatedResponse<MailItemView>> pendingMail(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(mailItemService.pending(unsorted(pageable)).map(MailItemView::of)));
    }

    @GetMapping("/domiciliation/mail/{itemNumber}")
    public ResponseEntity<MailItemView> mailItem(@PathVariable String itemNumber) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.get(itemNumber)));
    }

    @GetMapping("/domiciliation/mail/{itemNumber}/trail")
    public ResponseEntity<List<MailEventView>> mailTrail(@PathVariable String itemNumber) {
        return ResponseEntity.ok(mailItemService.trail(itemNumber).stream().map(MailEventView::of).toList());
    }

    @PostMapping("/domiciliation/mail/{itemNumber}/scan")
    public ResponseEntity<MailItemView> scan(@PathVariable String itemNumber, @Valid @RequestBody ScanRequest request, Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.scan(itemNumber, request.scanDocumentCode(), actor(authentication))));
    }

    @PostMapping("/domiciliation/mail/{itemNumber}/collect")
    public ResponseEntity<MailItemView> collect(@PathVariable String itemNumber, @Valid @RequestBody CollectRequest request, Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.collect(itemNumber,
                new MailItemService.Collection(request.collectedBy(), request.collectorIdDocument(), request.collectorSignatureUrl()), actor(authentication))));
    }

    @PostMapping("/domiciliation/mail/{itemNumber}/forward")
    public ResponseEntity<MailItemView> forward(@PathVariable String itemNumber, @RequestBody ForwardRequest request, Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.forward(itemNumber,
                new MailItemService.Forwarding(request.trackingNumber(), request.transportCost()), actor(authentication))));
    }

    @PostMapping("/domiciliation/mail/{itemNumber}/return")
    public ResponseEntity<MailItemView> returnMail(@PathVariable String itemNumber, @RequestBody(required = false) ReasonRequest request, Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.returnToSender(itemNumber, request == null ? null : request.reason(), actor(authentication))));
    }

    @PostMapping("/domiciliation/mail/{itemNumber}/destroy")
    public ResponseEntity<MailItemView> destroy(@PathVariable String itemNumber, @Valid @RequestBody ReasonRequest request, Authentication authentication) {
        return ResponseEntity.ok(MailItemView.of(mailItemService.destroy(itemNumber, request.reason(), actor(authentication))));
    }

    // -----------------------------------------------------------------------------------------

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
