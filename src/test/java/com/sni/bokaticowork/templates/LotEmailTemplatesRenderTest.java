package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les courriels des lots portefeuille, domiciliation, abonnements et relances se rendent, et
 * disent ce qu'ils doivent dire.
 *
 * <p>Chaque gabarit est rendu avec la charge utile que le service émet réellement. Un gabarit qui
 * ne cite pas une variable rend un courriel amputé sans aucune erreur · c'est ce que ce test
 * attrape.</p>
 */
class LotEmailTemplatesRenderTest {

    private final SpringTemplateEngine engine = engine();

    @ParameterizedTest
    @ValueSource(strings = {
            "wallet-transfer-sent", "wallet-transfer-received", "wallet-topup-completed", "wallet-payment-request", "wallet-low-balance",
            "wallet-status-changed", "wallet-security-event", "wallet-kyc-documents-requested", "wallet-treasury-alert",
            "domiciliation-activated", "domiciliation-registration-rejected", "domiciliation-certificate-issued",
            "domiciliation-certificate-expiring", "domiciliation-fiscal-lost", "domiciliation-terminated", "domiciliation-administration-notice",
            "mail-item-event", "subscription-grace-period", "subscription-termination", "subscription-direct-debit-failed",
            "subscription-quote-sent", "billing-dunning", "billing-dunning-handover", "mobile-money-deposit-unresolved",
            "mobile-money-deposit-failed", "mobile-money-deposit-pending-review", "self-service-payment-received",
            "cash-payment-declared", "cash-payment-confirmed", "cash-payment-declaration-expired"
    })
    void everyTemplateRendersWithAnEmptyModel(String template) {
        String html = render(template, Map.of());
        assertThat(html).contains("Elle A Osé").contains("coworkspace@elleaose.com").contains("client");
    }

    @Test
    void transferSentNamesTheCounterpartyAndTheAmount() {
        String html = render("wallet-transfer-sent", model("walletNumber", "WAL-000123", "currency", "XAF", "amount", "25000",
                "availableBalance", "75000", "counterpartyName", "Awa Ngoma", "aggregateId", "WTR-000042", "recipientName", "Joël"));
        assertThat(html).contains("Joël").contains("Awa Ngoma").contains("25000").contains("75000").contains("WTR-000042");
    }

    @Test
    void statusAndSecurityTemplatesSwitchOnTheEventType() {
        assertThat(render("wallet-status-changed", model("eventType", "WALLET_FROZEN", "reason", "Vérification en cours")))
                .contains("temporairement suspendu").contains("Vérification en cours");
        assertThat(render("wallet-status-changed", model("eventType", "WALLET_UNSUSPENDED"))).contains("de nouveau actif");
        assertThat(render("wallet-security-event", model("eventType", "WALLET_REVOKED_DEVICE_USED", "deviceId", "dev-9", "ipAddress", "10.0.0.1")))
                .contains("révoqué").contains("dev-9").contains("10.0.0.1");
    }

    @Test
    void domiciliationTemplatesCarryTheirFacts() {
        assertThat(render("domiciliation-certificate-issued", model("legalName", "SARL Mboté", "contractNumber", "DOM-2026-00007",
                "scope", "FISCAL", "validUntil", "2027-03-31", "documentCode", "DOC-1")))
                .contains("SARL Mboté").contains("DOM-2026-00007").contains("FISCAL").contains("2027-03-31");
        assertThat(render("domiciliation-terminated", model("effectiveDate", "2026-12-31", "reason", "Déménagement")))
                .contains("2026-12-31").contains("Déménagement");
        assertThat(render("mail-item-event", model("eventType", "MAIL_STORAGE_OVERDUE", "itemNumber", "MAIL-000031", "mailType", "REGISTERED_LETTER", "senderName", "DGI")))
                .contains("plusieurs jours").contains("MAIL-000031").contains("DGI");
    }

    @Test
    void lifecycleTemplatesCarryTheirFacts() {
        assertThat(render("subscription-grace-period", model("subscriptionNumber", "SUB-1", "daysBeforeSuspension", "14")))
                .contains("SUB-1").contains("14");
        String accepted = render("subscription-termination", model("eventType", "SUBSCRIPTION_TERMINATION_ACCEPTED", "subscriptionNumber", "SUB-1",
                "terminationCode", "TRM-2026-00003", "effectiveDate", "2026-10-31", "feeAmount", "60000"));
        assertThat(accepted).contains("acceptée").contains("TRM-2026-00003").contains("2026-10-31").contains("60000");
        String noFee = render("subscription-termination", model("eventType", "SUBSCRIPTION_TERMINATION_REQUESTED", "feeAmount", "0"));
        assertThat(noFee).doesNotContain("Frais de rupture");
        assertThat(render("subscription-direct-debit-failed", model("invoiceNumber", "INV-9", "amount", "30000", "currency", "XAF",
                "status", "INSUFFICIENT_FUNDS", "mandateSuspended", "true")))
                .contains("INV-9").contains("30000").contains("INSUFFICIENT_FUNDS").contains("mandat de prélèvement est suspendu");
        assertThat(render("subscription-quote-sent", model("quoteNumber", "QTE-2026-00001", "planName", "Flex", "quotedPrice", "90000",
                "currency", "XAF", "billingCycle", "MONTHLY", "commitmentMonths", "12", "validUntil", "2026-10-15")))
                .contains("QTE-2026-00001").contains("Flex").contains("90000").contains("12").contains("2026-10-15");
    }

    @Test
    void dunningTemplatesCarryTheRenderedMessage() {
        String notice = render("billing-dunning", model("eventType", "BILLING_DUNNING_FORMAL_NOTICE", "documentNumber", "INV-CUS-20260901-00000012",
                "balanceDue", "45000", "currency", "XAF", "dueDate", "2026-09-01", "daysOverdue", "20",
                "message", "Sans règlement sous huit jours, votre abonnement sera suspendu."));
        assertThat(notice).contains("mise en demeure").contains("INV-CUS-20260901-00000012").contains("45000").contains("20")
                .contains("Sans règlement sous huit jours");
        assertThat(render("billing-dunning-handover", model("documentNumber", "INV-1", "customerName", "SARL Mboté", "customerCode", "BIZ-1",
                "balanceDue", "45000", "currency", "XAF", "daysOverdue", "45", "message", "À traiter.")))
                .contains("SARL Mboté").contains("BIZ-1").contains("45");
    }

    @Test
    void theUnresolvedDepositAlertNamesWhatMustBeReconciled() {
        String html = render("mobile-money-deposit-unresolved", model("depositId", "dep-42", "intentNumber", "PIN-7",
                "transactionNumber", "TRX-9", "phoneNumber", "+242061234567", "provider", "MTN_MOMO_COG",
                "amount", "5000", "currency", "XAF", "hours", "24"));
        assertThat(html).contains("dep-42").contains("PIN-7").contains("TRX-9")
                .contains("+242061234567").contains("5000").contains("24");
    }

    @Test
    void theFailedDepositNoticeGivesTheReasonAndTheNextStep() {
        String retryable = render("mobile-money-deposit-failed", model("recipientName", "Joël",
                "amount", "5000", "currency", "XAF", "provider", "MTN Mobile Money Congo",
                "phoneNumber", "+242061234567", "transactionNumber", "TRX-1",
                "userMessage", "Le solde de votre compte mobile money est insuffisant.", "retryable", true));
        assertThat(retryable).contains("Joël").contains("5000").contains("MTN Mobile Money Congo")
                .contains("insuffisant").contains("Reprenez le paiement");

        String notRetryable = render("mobile-money-deposit-failed", model("amount", "5000", "currency", "XAF",
                "userMessage", "Ce numéro n'est pas un compte mobile money actif.", "retryable", false));
        assertThat(notRetryable).contains("Utilisez un autre numéro");
    }

    @Test
    void thePendingReviewNoticeTellsTheClientNotToPayTwice() {
        String html = render("mobile-money-deposit-pending-review", model("recipientName", "Joël",
                "amount", "5000", "currency", "XAF", "provider", "Airtel Money Congo", "transactionNumber", "TRX-1"));
        assertThat(html).contains("Joël").contains("une seconde fois").contains("Airtel Money Congo");
    }

    @Test
    void theSelfServicePaymentNoticeNamesTheClientAndTheAmount() {
        String html = render("self-service-payment-received", model("recipientName", "l'équipe",
                "customerName", "SARL Mboté", "customerCode", "BIZ-1", "amount", "25000", "currency", "XAF",
                "paymentMethod", "Mobile money", "transactionNumber", "TRX-1", "receiptNumber", "REC-1",
                "purpose", "Facture INV-1"));
        assertThat(html).contains("SARL Mboté").contains("25000").contains("TRX-1").contains("REC-1")
                .contains("Facture INV-1").contains("rien à valider");
    }

    @Test
    void theCashDeclarationNoticeTellsTheDeskWhatIsNotYetCollected() {
        String html = render("cash-payment-declared", model("recipientName", "l'équipe",
                "customerName", "Joël Bikindou", "amount", "25000", "currency", "XAF",
                "declarationNumber", "ESP-2026-000001", "documentNumber", "INV-1",
                "bookingNumber", "BKG-1", "note", "Je passe vers 14h"));
        assertThat(html).contains("Joël Bikindou").contains("25000").contains("ESP-2026-000001")
                .contains("BKG-1").contains("Je passe vers 14h")
                .contains("tant que vous n").contains("confirmé");
    }

    @Test
    void theCashConfirmationIsProofForTheClient() {
        String html = render("cash-payment-confirmed", model("recipientName", "Joël",
                "confirmedAmount", "25000", "currency", "XAF", "documentNumber", "INV-1",
                "transactionNumber", "TXN-1"));
        assertThat(html).contains("Joël").contains("25000").contains("TXN-1").contains("confirmation");
    }

    @Test
    void theExpiredCashNoticeSaysTheSlotWasReleased() {
        String released = render("cash-payment-declaration-expired", model("recipientName", "Joël",
                "amount", "25000", "currency", "XAF", "documentNumber", "INV-1",
                "bookingNumber", "BKG-1", "bookingReleased", true));
        assertThat(released).contains("BKG-1").contains("a été rendu").contains("reste réglable");

        String invoiceOnly = render("cash-payment-declaration-expired", model("amount", "25000",
                "currency", "XAF", "documentNumber", "INV-1", "bookingReleased", false));
        assertThat(invoiceOnly).doesNotContain("a été rendu").contains("reste réglable");
    }

    @Test
    void theBillingDocumentNoticeSaysSettledInsteadOfZeroDue() {
        String settled = render("billing-document", model("customerName", "Joël", "documentLabel", "facture",
                "documentNumber", "INV-1", "title", "Votre facture est soldee",
                "subtitle", "Plus rien ne reste a regler", "message", "Nous accusons reception de votre reglement de 25000 XAF.",
                "totalAmount", "25000 XAF", "paidAmount", "25000 XAF", "balanceDue", "0 XAF",
                "issueDate", "23/09/2026", "overdue", false, "settled", true));
        // Une facture soldee ne montre pas « Solde du : 0 XAF » · elle montre « Regle ».
        assertThat(settled).contains("Réglé").doesNotContain("Solde dû");

        String due = render("billing-document", model("customerName", "Joël", "documentLabel", "facture",
                "documentNumber", "INV-1", "totalAmount", "25000 XAF", "paidAmount", "0 XAF",
                "balanceDue", "25000 XAF", "issueDate", "23/09/2026", "overdue", false, "settled", false));
        assertThat(due).contains("Solde dû").contains("25000 XAF");
    }

    /** Les valeurs ne sont pas toutes des chaines · un drapeau se rend differemment d'un texte. */
    private static Map<String, Object> model(Object... kv) {
        Map<String, Object> model = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            model.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return model;
    }

    private String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.FRANCE);
        model.forEach(context::setVariable);
        return engine.process("email/" + template, context).replace("&#39;", "'");
    }

    private SpringTemplateEngine engine() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));
        return engine;
    }
}
