package com.sni.bokaticowork.features.domiciliation.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Les pieces generees de la domiciliation · le contrat et l'attestation.
 *
 * <p>Rendues par les memes gabarits et le meme moteur que les autres pieces, puis deposees au
 * module document qui sait les conserver, les versionner et les retrouver. Le contrat porte le
 * numero du dossier et ce qui engage ; l'attestation porte sa portee, commerciale ou fiscale, et
 * sa date de fin · une attestation sans date de fin serait vraie pour toujours, ce qu'aucune ne
 * l'est.</p>
 */
@Component
@RequiredArgsConstructor
public class DomiciliationDocuments {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentService documentService;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;

    public DocumentResponse contract(DomiciliationContract contract) {
        Context context = base(contract);
        context.setVariable("noticePeriodDays", contract.getNoticePeriodDays());
        context.setVariable("commitmentMonths", contract.getCommitmentMonths());
        context.setVariable("billingCycle", cycleLabel(contract));
        context.setVariable("fiscalEligible", Boolean.TRUE.equals(contract.getFiscalAddressEligible()));
        context.setVariable("mailMode", contract.getMailForwardingMode().name());
        byte[] pdf = render(templateEngine.process("domiciliation/contract", context));
        return store(contract, "DOMICILIATION_CONTRACT", "Contrat de domiciliation " + contract.getContractNumber(),
                contract.getContractNumber(), null, pdf);
    }

    public DocumentResponse certificate(DomiciliationContract contract, DomiciliationRegistration registration,
                                        DomiciliationContract.CertificateScope scope, LocalDate validUntil) {
        Context context = base(contract);
        context.setVariable("scope", scope.name());
        context.setVariable("scopeLabel", scope == DomiciliationContract.CertificateScope.FISCAL
                ? "Attestation de domiciliation fiscale" : "Attestation de domiciliation commerciale");
        context.setVariable("validUntil", DATE.format(validUntil));
        context.setVariable("registrationReference", registration == null ? null : registration.getAdministrationReference());
        context.setVariable("registrationDate", registration == null || registration.getRegistrationDate() == null
                ? null : DATE.format(registration.getRegistrationDate()));
        context.setVariable("registrationOffice", registration == null ? null : registration.getAdministrationOffice());
        byte[] pdf = render(templateEngine.process("domiciliation/certificate", context));
        return store(contract, "DOMICILIATION_CERTIFICATE",
                (scope == DomiciliationContract.CertificateScope.FISCAL ? "Attestation fiscale " : "Attestation commerciale ")
                        + contract.getContractNumber(), contract.getContractNumber() + "-" + scope.name(), validUntil, pdf);
    }

    // -----------------------------------------------------------------------------------------

    private Context base(DomiciliationContract contract) {
        Context context = new Context(appLocale);
        context.setVariable("contractNumber", contract.getContractNumber());
        context.setVariable("legalName", contract.getLegalName());
        context.setVariable("legalForm", contract.getLegalForm());
        context.setVariable("registrationNumber", contract.getRegistrationNumber());
        context.setVariable("taxNumber", contract.getTaxNumber());
        context.setVariable("legalRepresentativeName", contract.getLegalRepresentativeName());
        context.setVariable("addressLine", addressLine(contract));
        context.setVariable("suiteNumber", contract.getSuiteNumber());
        context.setVariable("startDate", contract.getStartDate() == null ? "" : DATE.format(contract.getStartDate()));
        context.setVariable("endDate", contract.getEndDate() == null ? "" : DATE.format(contract.getEndDate()));
        context.setVariable("issuedAt", DATE.format(LocalDate.now(APP_ZONE)));
        return context;
    }

    private String addressLine(DomiciliationContract contract) {
        Address address = contract.getAssignedAddress().getAddress();
        StringBuilder line = new StringBuilder();
        if (address.getStreetNumber() != null) line.append(address.getStreetNumber()).append(' ');
        if (address.getStreetName() != null) line.append(address.getStreetName());
        if (address.getDistrict() != null) line.append(", ").append(address.getDistrict());
        if (address.getCity() != null) line.append(", ").append(address.getCity());
        if (address.getCountry() != null && address.getCountry().getName() != null) line.append(", ").append(address.getCountry().getName());
        return line.toString().trim();
    }

    private String cycleLabel(DomiciliationContract contract) {
        return switch (contract.getBillingCycle()) {
            case MONTHLY -> "mensuelle";
            case QUARTERLY -> "trimestrielle";
            case YEARLY -> "annuelle";
            default -> contract.getBillingCycle().name().toLowerCase(Locale.ROOT);
        };
    }

    private DocumentResponse store(DomiciliationContract contract, String typeCode, String title, String number,
                                   LocalDate expiry, byte[] pdf) {
        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(ownerType(contract));
        metadata.setOwnerCode(contract.getSubscription().getSubscriberCode());
        metadata.setDocumentTypeCode(typeCode);
        metadata.setTitle(title);
        metadata.setDocumentNumber(number);
        metadata.setIssueDate(LocalDate.now(APP_ZONE));
        metadata.setExpiryDate(expiry);
        return documentService.createGeneratedDocument(metadata, number.toLowerCase(Locale.ROOT) + ".pdf", "application/pdf", pdf);
    }

    private DocumentOwnerType ownerType(DomiciliationContract contract) {
        SubscriberType type = contract.getSubscription().getSubscriberType();
        return switch (type) {
            case MEMBER -> DocumentOwnerType.MEMBER;
            case CUSTOMER -> DocumentOwnerType.CUSTOMER;
            case BUSINESS_ENTITY -> DocumentOwnerType.BUSINESS;
        };
    }

    private byte[] render(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(html);
            document.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(document), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de générer la pièce de domiciliation", ex);
        }
    }
}
