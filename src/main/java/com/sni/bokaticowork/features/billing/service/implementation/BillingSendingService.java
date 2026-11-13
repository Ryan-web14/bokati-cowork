package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessage;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageRepository;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.validation.EmailAddresses;
import com.sni.bokaticowork.features.billing.dto.request.BillingRecipientPreferenceRequest;
import com.sni.bokaticowork.features.billing.dto.request.SendDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingRecipientsResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingSendingResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSending;
import com.sni.bokaticowork.features.billing.model.BillingRecipientPreference;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSendingRepository;
import com.sni.bokaticowork.features.billing.repository.BillingRecipientPreferenceRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Envois des pieces (lot B4) : destinataires choisis (principaux, copie, copie cachee), canaux
 * (courriel avec le PDF, WhatsApp par Infobip avec un lien vers l'espace client), journal de chaque
 * envoi. L'envoi ne change jamais le statut de la piece (ecart E6).
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingSendingService {

    static final String EMAIL = "EMAIL";
    static final String WHATSAPP = "WHATSAPP";
    private static final String PORTAL_PATH = "factures";

    private final BillingDocumentServiceImpl billing;
    private final BillingEmailService emails;
    private final BillingDocumentSendingRepository sendings;
    private final BillingRecipientPreferenceRepository preferences;
    private final MemberBillingContact memberContact;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<WhatsAppMessageSender> whatsApp;
    private final ObjectProvider<WhatsAppMessageRepository> whatsAppMessages;

    public List<BillingSendingResponse> send(String documentNumber, SendDocumentRequest request) {
        // Trace l'envoi sur la piece (date, version transmise) ; refuse un brouillon, ne change pas le statut.
        billing.send(documentNumber);
        BillingDocument document = billing.serviceByNumber(documentNumber);
        Optional<BillingRecipientPreference> preference = preferences.findByCustomerTypeAndCustomerCode(
                String.valueOf(document.getCustomerType()), String.valueOf(document.getCustomerCode()));
        boolean explicit = request != null && ((request.to() != null && !request.to().isEmpty()) || (request.channels() != null && !request.channels().isEmpty())
                || (request.whatsappNumbers() != null && !request.whatsappNumbers().isEmpty()));
        List<String> channels = request != null && request.channels() != null && !request.channels().isEmpty()
                ? request.channels().stream().map(channel -> channel.trim().toUpperCase(Locale.ROOT)).distinct().toList()
                : preference.map(BillingRecipientPreference::getChannels).filter(StringUtils::hasText).map(BillingSendingService::split)
                .orElse(List.of(EMAIL));
        StaffProfileService.Author author = staffProfiles.currentAuthor();
        String message = request == null ? null : request.message();
        boolean duplicate = Boolean.TRUE.equals(document.getLocked()) && document.getRecipientCorrectedAt() != null;
        List<BillingSendingResponse> result = new ArrayList<>();
        for (String channel : channels) {
            if (EMAIL.equals(channel)) {
                List<String> to = explicit && request.to() != null && !request.to().isEmpty() ? emailsOf(request.to())
                        : preference.map(BillingRecipientPreference::getToAddresses).map(BillingSendingService::emailsOf).filter(list -> !list.isEmpty())
                        .orElseGet(() -> emailsOf(List.of(String.valueOf(memberContact.recipient(document.getCustomerType(), document.getCustomerCode(),
                                document.getCustomerEmail())))));
                List<String> cc = explicit ? emailsOf(request.cc()) : preference.map(BillingRecipientPreference::getCcAddresses).map(BillingSendingService::emailsOf).orElse(List.of());
                List<String> bcc = explicit ? emailsOf(request.bcc()) : preference.map(BillingRecipientPreference::getBccAddresses).map(BillingSendingService::emailsOf).orElse(List.of());
                if (to.isEmpty()) {
                    throw new BadRequestException("Aucune adresse de destinataire pour " + documentNumber + " : indiquez-en une");
                }
                BillingDocumentSending sending = BillingDocumentSending.builder().documentId(document.getId()).documentNumber(documentNumber)
                        .channel(EMAIL).recipientsTo(String.join(", ", to)).recipientsCc(join(cc)).recipientsBcc(join(bcc)).message(message)
                        .duplicate(duplicate).sentBy(author.login()).sentByName(author.name()).sentAt(Instant.now()).build();
                try {
                    sending.setSubject(emails.sendDocumentTo(documentNumber, to, cc, bcc, message));
                    sending.setStatus("QUEUED");
                } catch (RuntimeException exception) {
                    sending.setStatus("FAILED");
                    sending.setError(truncate(exception.getMessage()));
                }
                result.add(view(sendings.save(sending)));
            } else if (WHATSAPP.equals(channel)) {
                List<String> phones = explicit && request.whatsappNumbers() != null && !request.whatsappNumbers().isEmpty() ? clean(request.whatsappNumbers())
                        : preference.map(BillingRecipientPreference::getWhatsappNumbers).map(BillingSendingService::split).filter(list -> !list.isEmpty())
                        .orElseGet(() -> StringUtils.hasText(document.getCustomerPhone()) ? List.of(document.getCustomerPhone().trim()) : List.of());
                if (phones.isEmpty()) {
                    throw new BadRequestException("Aucun numéro WhatsApp pour " + documentNumber + " : indiquez-en un");
                }
                for (String phone : phones) {
                    result.add(view(sendings.save(sendWhatsApp(document, phone, message, duplicate, author))));
                }
            } else {
                throw new BadRequestException("Canal inconnu : " + channel + " (EMAIL ou WHATSAPP)");
            }
        }
        return result;
    }

    /** WhatsApp : facture soldee (modele facture_payee) ou a regler (modele rappel_paiement), lien vers l'espace client. */
    private BillingDocumentSending sendWhatsApp(BillingDocument document, String phone, String message, boolean duplicate,
                                                StaffProfileService.Author author) {
        boolean settled = document.getBalanceDue() == null || document.getBalanceDue().signum() <= 0;
        String name = StringUtils.hasText(document.getCustomerName()) ? document.getCustomerName().trim().split("\\s+")[0] : "client";
        String purpose = settled ? "INVOICE_PAID" : "PAYMENT_REMINDER";
        List<String> values = settled
                ? List.of(name, amount(document.getPaidAmount()), document.getDocumentNumber())
                : List.of(name, document.getDocumentNumber(), amount(document.getBalanceDue()),
                document.getDueDate() == null ? "réception" : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(document.getDueDate()));
        BillingDocumentSending sending = BillingDocumentSending.builder().documentId(document.getId()).documentNumber(document.getDocumentNumber())
                .channel(WHATSAPP).recipientsTo(phone).subject(purpose).message(message).duplicate(duplicate).sentBy(author.login())
                .sentByName(author.name()).sentAt(Instant.now()).build();
        WhatsAppMessageSender sender = whatsApp.getIfAvailable();
        if (sender == null) {
            sending.setStatus("FAILED");
            sending.setError("WhatsApp n'est pas configuré");
            return sending;
        }
        try {
            WhatsAppMessageSender.Result sent = sender.send(phone, purpose, values, message, document.getDocumentNumber(), PORTAL_PATH);
            sending.setProviderReference(sent.messageId());
            sending.setStatus(sent.statusGroup() == null ? "SENT" : sent.statusGroup());
        } catch (RuntimeException exception) {
            sending.setStatus("FAILED");
            sending.setError(truncate(exception.getMessage()));
        }
        return sending;
    }

    @Transactional(readOnly = true)
    public List<BillingSendingResponse> sendings(String documentNumber) {
        return sendings.findByDocumentNumberOrderBySentAtDesc(documentNumber).stream().map(this::view).toList();
    }

    // ------------------------------------------------------------------ preferences

    @Transactional(readOnly = true)
    public BillingRecipientsResponse recipients(String customerType, String customerCode) {
        Optional<BillingRecipientPreference> preference = preferences.findByCustomerTypeAndCustomerCode(customerType, customerCode);
        Set<String> suggestedEmails = new LinkedHashSet<>();
        String known = memberContact.recipient(customerType, customerCode, null);
        if (StringUtils.hasText(known)) {
            suggestedEmails.addAll(EmailAddresses.split(known));
        }
        preference.ifPresent(found -> {
            suggestedEmails.addAll(emailsOf(found.getToAddresses()));
            suggestedEmails.addAll(emailsOf(found.getCcAddresses()));
        });
        Set<String> suggestedPhones = new LinkedHashSet<>(preference.map(BillingRecipientPreference::getWhatsappNumbers).map(BillingSendingService::split)
                .orElse(List.of()));
        return new BillingRecipientsResponse(customerType, customerCode,
                preference.map(BillingRecipientPreference::getToAddresses).map(BillingSendingService::emailsOf).orElse(List.of()),
                preference.map(BillingRecipientPreference::getCcAddresses).map(BillingSendingService::emailsOf).orElse(List.of()),
                preference.map(BillingRecipientPreference::getBccAddresses).map(BillingSendingService::emailsOf).orElse(List.of()),
                preference.map(BillingRecipientPreference::getWhatsappNumbers).map(BillingSendingService::split).orElse(List.of()),
                preference.map(BillingRecipientPreference::getChannels).map(BillingSendingService::split).orElse(List.of(EMAIL)),
                List.copyOf(suggestedEmails), List.copyOf(suggestedPhones));
    }

    public BillingRecipientsResponse saveRecipients(String customerType, String customerCode, BillingRecipientPreferenceRequest request) {
        List<String> channels = request.channels() == null || request.channels().isEmpty() ? List.of(EMAIL)
                : request.channels().stream().map(channel -> channel.trim().toUpperCase(Locale.ROOT)).distinct().toList();
        if (channels.stream().anyMatch(channel -> !EMAIL.equals(channel) && !WHATSAPP.equals(channel))) {
            throw new BadRequestException("Canaux possibles : EMAIL, WHATSAPP");
        }
        BillingRecipientPreference preference = preferences.findByCustomerTypeAndCustomerCode(customerType, customerCode)
                .orElseGet(() -> BillingRecipientPreference.builder().customerType(customerType).customerCode(customerCode).build());
        preference.setToAddresses(join(emailsOf(request.to())));
        preference.setCcAddresses(join(emailsOf(request.cc())));
        preference.setBccAddresses(join(emailsOf(request.bcc())));
        preference.setWhatsappNumbers(join(clean(request.whatsappNumbers())));
        preference.setChannels(String.join(",", channels));
        preference.setUpdatedBy(staffProfiles.currentAuthor().login());
        preferences.save(preference);
        return recipients(customerType, customerCode);
    }

    // ------------------------------------------------------------------ outils

    private BillingSendingResponse view(BillingDocumentSending sending) {
        Instant delivered = null;
        Instant seen = null;
        String status = sending.getStatus();
        if (WHATSAPP.equals(sending.getChannel()) && StringUtils.hasText(sending.getProviderReference())) {
            WhatsAppMessageRepository repository = whatsAppMessages.getIfAvailable();
            Optional<WhatsAppMessage> message = repository == null ? Optional.empty() : repository.findByMessageId(sending.getProviderReference());
            if (message.isPresent()) {
                delivered = message.get().getDoneAt();
                seen = message.get().getSeenAt();
                status = message.get().getStatusGroup() != null ? message.get().getStatusGroup() : status;
            }
        }
        return new BillingSendingResponse(sending.getId(), sending.getDocumentNumber(), sending.getChannel(), split(sending.getRecipientsTo()),
                split(sending.getRecipientsCc()), split(sending.getRecipientsBcc()), sending.getSubject(), sending.getMessage(),
                Boolean.TRUE.equals(sending.getDuplicate()), status, sending.getProviderReference(), sending.getError(), sending.getSentBy(),
                sending.getSentByName(), sending.getSentAt(), delivered, seen);
    }

    static List<String> emailsOf(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.addAll(EmailAddresses.split(value));
            }
        }
        return result.stream().map(String::trim).distinct().toList();
    }

    static List<String> emailsOf(String value) {
        return StringUtils.hasText(value) ? emailsOf(List.of(value)) : List.of();
    }

    static List<String> split(String value) {
        return !StringUtils.hasText(value) ? List.of()
                : Arrays.stream(value.split("[,;]")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }

    private static String amount(BigDecimal value) {
        return value == null ? "0" : String.format(Locale.FRANCE, "%,d", value.setScale(0, java.math.RoundingMode.HALF_UP).longValue())
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    private static String truncate(String value) {
        return value == null ? null : value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
