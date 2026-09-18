package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.model.MailItemEvent;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemEventRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.domiciliation.repository.ServiceDefinitionRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le courrier du domicilie · avec preuve de remise.
 *
 * <p>Le recommande et les actes se notifient sans delai et se remettent contre identite et
 * signature. Un pli simple se remet contre un nom. La base tient la premiere regle par contrainte ;
 * le journal des evenements tient la preuve, et ne se reecrit pas.</p>
 *
 * <p>Ce qui se facture a l'acte · numerisation, reexpedition, garde prolongee · passe par le
 * module d'usage, qui sait deja produire une ligne facturable. Le prix vient du catalogue de
 * services, pas d'une constante.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailItemService {

    private final MailItemRepository mailRepository;
    private final MailItemEventRepository eventRepository;
    private final ServiceDefinitionRepository definitionRepository;
    private final DomiciliationService domiciliationService;
    private final UsageRecordService usageRecordService;
    private final TransactionContextResolver contextResolver;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.domiciliation.mail.storage-days:30}")
    private int storageDays;

    public record Receipt(MailItem.Type mailType, String senderName, String senderReference,
                          Integer weightGrams, String dimensions, String notes) {
    }

    public record Collection(String collectedBy, String collectorIdDocument, String collectorSignatureUrl) {
    }

    public record Forwarding(String trackingNumber, BigDecimal transportCost) {
    }

    // -----------------------------------------------------------------------------------------

    /** Recoit un pli · et previent aussitot, tout de suite pour ce qui presse, dans la foulee pour le reste. */
    @Transactional
    public MailItem receive(String contractNumber, Receipt receipt, String receivedBy) {
        DomiciliationContract contract = domiciliationService.get(contractNumber);
        if (!contract.getStatus().live()) {
            throw new ConflictException("courrier", "ce contrat n'est pas actif · le courrier ne peut pas y être enregistré");
        }
        if (receipt.mailType() == null) {
            throw new BadRequestException("La nature du pli est requise");
        }
        MailItem item = mailRepository.save(MailItem.builder()
                .itemNumber(sequenceGenerator.next("mail_item"))
                .contract(contract)
                .mailType(receipt.mailType())
                .senderName(trim(receipt.senderName()))
                .senderReference(trim(receipt.senderReference()))
                .receivedAt(Instant.now())
                .receivedBy(receivedBy)
                .weightGrams(receipt.weightGrams())
                .dimensions(trim(receipt.dimensions()))
                .notes(trim(receipt.notes()))
                .status(MailItem.Status.RECEIVED)
                .storageDeadline(LocalDate.now().plusDays(Math.max(1, storageDays)))
                .build());
        event(item, "RECEIVED", receivedBy, receipt.mailType() + (receipt.senderName() == null ? "" : " de " + receipt.senderName()));
        notifyHolder(item, "MAIL_RECEIVED", receipt.mailType().requiresIdentityOnCollection()
                ? "Courrier important reçu · " + label(receipt.mailType())
                : "Courrier reçu");
        item.setStatus(MailItem.Status.NOTIFIED);
        item.setNotifiedAt(Instant.now());
        item.setNotificationChannel("EMAIL");
        event(item, "NOTIFIED", "SYSTEM", "courriel");
        return mailRepository.save(item);
    }

    /** Numerise · le document est depose par ailleurs, ici on le rattache et on facture l'acte. */
    @Transactional
    public MailItem scan(String itemNumber, String scanDocumentCode, String actor) {
        MailItem item = open(itemNumber);
        if (!StringUtils.hasText(scanDocumentCode)) {
            throw new BadRequestException("Déposez la numérisation avant de la rattacher");
        }
        item.setScanDocumentCode(scanDocumentCode.trim());
        item.setStatus(MailItem.Status.SCANNED);
        bill(item, "SVC-MAIL-SCAN", BigDecimal.ONE, null, "numérisation");
        event(item, "SCANNED", actor, scanDocumentCode.trim());
        notifyHolder(item, "MAIL_SCANNED", "Votre courrier a été numérisé");
        return mailRepository.save(item);
    }

    /**
     * Remise en main propre · contre identite pour ce qui l'exige.
     *
     * <p>Le message dit ce qui manque plutot que de refuser : au guichet, la personne est devant
     * soi, et « piece d'identité requise pour un recommandé » se resout en une minute.</p>
     */
    @Transactional
    public MailItem collect(String itemNumber, Collection collection, String handedOverBy) {
        MailItem item = open(itemNumber);
        if (!StringUtils.hasText(collection.collectedBy())) {
            throw new BadRequestException("Le nom de la personne qui retire le pli est requis");
        }
        if (item.getMailType().requiresIdentityOnCollection() && !StringUtils.hasText(collection.collectorIdDocument())) {
            throw new BadRequestException("Un " + label(item.getMailType()).toLowerCase()
                    + " se remet contre pièce d'identité · indiquez sa référence");
        }
        item.setStatus(MailItem.Status.COLLECTED);
        item.setCollectedAt(Instant.now());
        item.setCollectedBy(collection.collectedBy().trim());
        item.setCollectorIdDocument(trim(collection.collectorIdDocument()));
        item.setCollectorSignatureUrl(trim(collection.collectorSignatureUrl()));
        item.setHandedOverBy(handedOverBy);
        event(item, "COLLECTED", handedOverBy, "remis à " + collection.collectedBy().trim()
                + (collection.collectorIdDocument() == null ? "" : " · pièce " + collection.collectorIdDocument().trim()));
        return mailRepository.save(item);
    }

    /** Reexpedie · l'acte et le transport se facturent, le suivi se garde. */
    @Transactional
    public MailItem forward(String itemNumber, Forwarding forwarding, String actor) {
        MailItem item = open(itemNumber);
        DomiciliationContract contract = item.getContract();
        if (contract.getMailForwardingMode() == DomiciliationContract.MailForwardingMode.HOLD
                || contract.getMailForwardingMode() == DomiciliationContract.MailForwardingMode.SCAN_ONLY) {
            throw new ConflictException("courrier", "ce contrat ne prévoit pas la réexpédition · mode " + contract.getMailForwardingMode());
        }
        if (contract.getForwardingAddress() == null) {
            throw new ConflictException("courrier", "aucune adresse de réexpédition n'est enregistrée sur ce contrat");
        }
        item.setStatus(MailItem.Status.FORWARDED);
        item.setForwardedAt(Instant.now());
        item.setForwardingTrackingNumber(trim(forwarding.trackingNumber()));
        item.setForwardingCost(forwarding.transportCost());
        bill(item, "SVC-MAIL-FORWARD", BigDecimal.ONE, forwarding.transportCost(), "réexpédition");
        event(item, "FORWARDED", actor, forwarding.trackingNumber() == null ? "sans suivi" : "suivi " + forwarding.trackingNumber());
        notifyHolder(item, "MAIL_FORWARDED", "Votre courrier a été réexpédié");
        return mailRepository.save(item);
    }

    @Transactional
    public MailItem returnToSender(String itemNumber, String reason, String actor) {
        MailItem item = open(itemNumber);
        item.setStatus(MailItem.Status.RETURNED);
        event(item, "RETURNED", actor, reason);
        return mailRepository.save(item);
    }

    /** Destruction · jamais pour ce qui se remet contre identite, quel que soit le delai. */
    @Transactional
    public MailItem destroy(String itemNumber, String reason, String actor) {
        MailItem item = open(itemNumber);
        if (item.getMailType().requiresIdentityOnCollection()) {
            throw new ConflictException("courrier", "un " + label(item.getMailType()).toLowerCase()
                    + " ne se détruit pas · il se retourne à l'expéditeur");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Une destruction se motive");
        }
        item.setStatus(MailItem.Status.DESTROYED);
        event(item, "DESTROYED", actor, reason.trim());
        return mailRepository.save(item);
    }

    /** Garde prolongee · facturee par jour au-dela du delai, et relance du titulaire. */
    @Transactional
    public int chaseOverdue() {
        List<MailItem> overdue = mailRepository.findByStatusInAndStorageDeadlineBefore(
                List.of(MailItem.Status.RECEIVED, MailItem.Status.NOTIFIED, MailItem.Status.SCANNED), LocalDate.now());
        for (MailItem item : overdue) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(item.getStorageDeadline(), LocalDate.now());
            if (days <= 0) {
                continue;
            }
            bill(item, "SVC-MAIL-STORAGE", BigDecimal.valueOf(days), null, "garde prolongée · " + days + " jour(s)");
            // Le delai avance d'autant · on ne refacture pas les memes jours au prochain passage.
            item.setStorageDeadline(LocalDate.now());
            event(item, "STORAGE_CHASED", "SYSTEM", days + " jour(s) au-delà du délai de garde");
            mailRepository.save(item);
            notifyHolder(item, "MAIL_STORAGE_OVERDUE", "Courrier en attente de retrait depuis " + days + " jour(s)");
        }
        return overdue.size();
    }

    // -----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MailItem get(String itemNumber) {
        return mailRepository.findByItemNumber(itemNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pli introuvable"));
    }

    @Transactional(readOnly = true)
    public Page<MailItem> ofContract(String contractNumber, Pageable pageable) {
        return mailRepository.findByContract_IdOrderByReceivedAtDesc(domiciliationService.get(contractNumber).getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<MailItem> pending(Pageable pageable) {
        return mailRepository.findByStatusInOrderByReceivedAtAsc(
                List.of(MailItem.Status.RECEIVED, MailItem.Status.NOTIFIED, MailItem.Status.SCANNED), pageable);
    }

    @Transactional(readOnly = true)
    public List<MailItemEvent> trail(String itemNumber) {
        return eventRepository.findByMailItem_IdOrderByOccurredAtAsc(get(itemNumber).getId());
    }

    // -----------------------------------------------------------------------------------------

    private MailItem open(String itemNumber) {
        MailItem item = get(itemNumber);
        if (item.getStatus().closed()) {
            throw new ConflictException("courrier", "ce pli est déjà " + item.getStatus());
        }
        return item;
    }

    private void event(MailItem item, String type, String actor, String details) {
        eventRepository.save(MailItemEvent.builder()
                .mailItem(item)
                .eventType(type)
                .actor(actor == null ? "SYSTEM" : actor)
                .details(details == null ? null : details.length() > 1000 ? details.substring(0, 1000) : details)
                .build());
    }

    /**
     * Facture l'acte par le module d'usage · le prix est celui du catalogue.
     *
     * <p>Un service absent du catalogue ou sans prix ne facture rien, et le dit dans le journal : on
     * ne devine pas un prix, et on ne perd pas silencieusement un acte facturable.</p>
     */
    private void bill(MailItem item, String serviceCode, BigDecimal quantity, BigDecimal extra, String what) {
        ServiceDefinition definition = definitionRepository.findByCode(serviceCode).orElse(null);
        if (definition == null || definition.getUnitPrice() == null || !StringUtils.hasText(definition.getUsageEntitlementCode())) {
            log.warn("Courrier {} · {} non facturé · service {} sans prix au catalogue", item.getItemNumber(), what, serviceCode);
            return;
        }
        BigDecimal amount = definition.getUnitPrice().multiply(quantity).add(extra == null ? BigDecimal.ZERO : extra);
        Subscription subscription = item.getContract().getSubscription();
        var usage = usageRecordService.record(new CreateUsageRecordRequest(
                subscription.getSubscriberType(), subscription.getSubscriberCode(),
                definition.getUsageEntitlementCode(), quantity, EntitlementUnit.CREDIT,
                "MAIL_ITEM", item.getItemNumber(), false, true, amount, definition.getCurrency(),
                Instant.now(), "{\"what\":\"" + what.replace("\"", "'") + "\"}"));
        item.setBillable(true);
        item.setBilledAmount((item.getBilledAmount() == null ? BigDecimal.ZERO : item.getBilledAmount()).add(amount));
        item.setUsageNumber(usage == null ? item.getUsageNumber() : usage.usageNumber());
    }

    private void notifyHolder(MailItem item, String eventType, String subject) {
        Subscription subscription = item.getContract().getSubscription();
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(
                subscription.getSubscriberType().name(), subscription.getSubscriberCode());
        if (!StringUtils.hasText(party.email())) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientEmail", party.email());
        payload.put("recipientName", party.name());
        payload.put("recipientType", subscription.getSubscriberType().name());
        payload.put("recipientCode", subscription.getSubscriberCode());
        payload.put("subject", subject);
        payload.put("templateCode", eventType);
        payload.put("itemNumber", item.getItemNumber());
        payload.put("mailType", item.getMailType().name());
        payload.put("senderName", item.getSenderName() == null ? "" : item.getSenderName());
        payload.put("contractNumber", item.getContract().getContractNumber());
        outboxService.publish(eventType, "DOMICILIATION", item.getItemNumber(), payload);
    }

    private String label(MailItem.Type type) {
        return switch (type) {
            case LETTER -> "Lettre";
            case REGISTERED_LETTER -> "Recommandé";
            case PARCEL -> "Colis";
            case ADMINISTRATIVE -> "Courrier administratif";
            case LEGAL_NOTICE -> "Acte";
            case OTHER -> "Pli";
        };
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
