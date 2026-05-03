package com.sni.bokaticowork.features.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingDocumentPdfServiceImplTest {

    @Mock
    private BillingDocumentService billingDocumentService;

    private BillingDocumentPdfServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillingDocumentPdfServiceImpl(billingDocumentService, templateEngine(), new ObjectMapper());
    }

    @Test
    void shouldGeneratePdfFromBillingHtmlTemplate() {
        when(billingDocumentService.get("INV-CUS-20260422-00000001")).thenReturn(sampleDocument());

        byte[] pdf = service.generatePdf("INV-CUS-20260422-00000001");

        assertTrue(pdf.length > 0);
        assertArrayEquals("%PDF-".getBytes(StandardCharsets.US_ASCII), java.util.Arrays.copyOf(pdf, 5));
    }

    @Test
    void shouldRenderObjectCustomerAndRoundedAmountsInPdf() throws Exception {
        when(billingDocumentService.get("INV-CUS-20260422-00000001")).thenReturn(sampleDocument());

        byte[] pdf = service.generatePdf("INV-CUS-20260422-00000001");

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            String normalized = text.toUpperCase(Locale.ROOT);
            assertTrue(normalized.contains("OBJET"));
            assertTrue(normalized.contains("JEAN BEBERRE"));
            assertTrue(normalized.contains("RESERVATION SALLE"));
            assertTrue(normalized.contains("PAIEMENT A RECEPTION"));
            assertTrue(normalized.contains("119 XAF"));
            assertTrue(normalized.contains("20 XAF"));
            assertTrue(!normalized.contains("119.25"));
            assertTrue(!normalized.contains("100.00"));
            assertTrue(!normalized.contains("AUTOMATIQUEMENT"));
        }
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private BillingDocumentResponse sampleDocument() {
        return new BillingDocumentResponse(
                "INV-CUS-20260422-00000001",
                BillingDocumentType.INVOICE,
                BillingDocumentStatus.ISSUED,
                "CUSTOMER",
                "CUS-0001",
                "Jean Beberre",
                "jean@example.com",
                "061234567",
                "{\"streetNumber\":\"12\",\"streetName\":\"Rue des Fleurs\",\"district\":\"Bonanjo\",\"city\":\"Douala\"}",
                true,
                "BOOKING",
                "BKG-0001",
                "BOOKING",
                "BKG-0001",
                "Reservation BKG-0001",
                true,
                "Reservation salle",
                "Facture de reservation",
                "Paiement a reception",
                "XAF",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("19.25"),
                BigDecimal.ZERO,
                new BigDecimal("19.25"),
                new BigDecimal("119.25"),
                new BigDecimal("20.00"),
                new BigDecimal("99.25"),
                LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 27),
                Instant.parse("2026-04-22T09:00:00Z"),
                Instant.parse("2026-04-22T10:00:00Z"),
                null,
                null,
                List.of(new BillingDocumentLineResponse(
                        1,
                        BillingLineType.BOOKING,
                        "RES-BUR-202604-00000002",
                        "Bureau prive",
                        "Reservation 1 heure",
                        BigDecimal.ONE,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true,
                        new BigDecimal("19.25"),
                        BigDecimal.ZERO,
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("19.25"),
                        BigDecimal.ZERO,
                        new BigDecimal("19.25"),
                        new BigDecimal("119.25"),
                        "BOOKING",
                        "BKG-0001"
                )),
                List.of(),
                List.of(),
                List.of(new BillingDocumentClauseResponse(
                        "PAY-001",
                        "Paiement",
                        "Paiement attendu avant echeance.",
                        1
                ))
        );
    }
}
