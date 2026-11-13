package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStatisticsService;
import com.sni.bokaticowork.features.payment.service.support.CashReportDocumentData;
import com.sni.bokaticowork.features.payment.service.support.CashVoucherDocumentData;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CashDocumentPdfServiceImpl implements CashDocumentPdfService {

    private final CashMovementRepository movementRepository;
    private final CashSessionRepository sessionRepository;
    private final CashRegisterMapper cashRegisterMapper;
    private final CashRegisterService cashRegisterService;
    private final CashRequestService cashRequestService;
    private final CashStatisticsService cashStatisticsService;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final BrandLogo brandLogo;
    private final OperatorIdentityService operatorIdentityService;
    private DocumentTemplatePrinter templatePrinter;

    /** Facultatif · injecte par mutateur pour ne pas toucher au constructeur des tests. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setTemplatePrinter(DocumentTemplatePrinter templatePrinter) {
        this.templatePrinter = templatePrinter;
    }

    private com.sni.bokaticowork.features.document.template.render.DocumentData requestData(CashRequestResponse request) {
        return CashReportDocumentData.request(request, requestTypeLabel(request.requestType()), requestStatusLabel(request.status()));
    }

    private com.sni.bokaticowork.features.document.template.render.DocumentData closingData(CashSessionSummaryResponse summary,
                                                                                           List<CashMovementResponse> movements, String reviewedBy) {
        CashDocumentTemplateFormatter fmt = new CashDocumentTemplateFormatter(appLocale);
        return CashReportDocumentData.closing(summary, movements, reviewedBy, movement -> fmt.movementTypeLabel(movement.movementType()));
    }

    @Override
    public String templateCode(String movementNumber) {
        return CashVoucherDocumentData.templateCode(cashRegisterMapper.toCashMovementResponse(movement(movementNumber)));
    }

    /** Donnees d'un mouvement pour l'apercu d'un modele de bon de caisse. */
    @Override
    public com.sni.bokaticowork.features.document.template.render.DocumentData previewData(String templateCode, String movementNumber) {
        if (CashReportDocumentData.REQUEST.equals(templateCode)) {
            return requestData(cashRequestService.get(movementNumber));
        }
        if (CashReportDocumentData.CLOSING.equals(templateCode)) {
            CashSession session = session(movementNumber);
            List<CashMovementResponse> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId()).stream()
                    .map(cashRegisterMapper::toCashMovementResponse).toList();
            return closingData(cashRegisterService.sessionSummary(movementNumber), movements, session.getReviewedBy());
        }
        CashMovement movement = movement(movementNumber);
        CashMovementResponse response = cashRegisterMapper.toCashMovementResponse(movement);
        if (!templateCode.equals(CashVoucherDocumentData.templateCode(response))) {
            throw new BadRequestException("Le mouvement " + movementNumber + " ne relève pas de ce modèle");
        }
        return CashVoucherDocumentData.from(templateCode, response, documentTypeLabel(response.documentType(), response.movementType()),
                false, null);
    }

    @Override
    public byte[] generateMovementDocument(String movementNumber) {
        CashMovement movement = movement(movementNumber);
        CashMovementResponse response = cashRegisterMapper.toCashMovementResponse(movement);
        Context context = new Context(appLocale);
        context.setVariable("movement", response);
        context.setVariable("docTypeLabel", documentTypeLabel(response.documentType(), response.movementType()));
        context.setVariable("generatedAt", Instant.now());
        context.setVariable("fmt", new CashDocumentTemplateFormatter(appLocale));
        brand(context);
        // Lot C4 : une piece deja imprimee ressort en DUPLICATA ; une piece contre-passee porte la
        // mention ANNULE PAR et le numero du mouvement inverse.
        context.setVariable("duplicate", movement.getPrintCount() != null && movement.getPrintCount() > 0);
        String reversedBy = movement.getStatus() == com.sni.bokaticowork.features.payment.enums.CashMovementStatus.REVERSED
                ? response.relatedMovementNumber() : null;
        context.setVariable("reversedBy", reversedBy);
        // Modeles de documents (lot D2) : bon d'entree ou de sortie selon la version publiee de son
        // modele ; sans version publiee, ou si son rendu echoue, le gabarit d'origine sert.
        String templateCode = CashVoucherDocumentData.templateCode(response);
        if (templatePrinter != null && templateCode != null) {
            java.util.Optional<byte[]> printed = templatePrinter.tryRender(templateCode, CashVoucherDocumentData.from(templateCode, response,
                    (String) context.getVariable("docTypeLabel"), Boolean.TRUE.equals(context.getVariable("duplicate")), reversedBy));
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return renderToPdf(templateEngine.process("cash/movement-document", context));
    }

    @Override
    public byte[] generateRequestDocument(String requestNumber) {
        CashRequestResponse request = cashRequestService.get(requestNumber);
        Context context = new Context(appLocale);
        context.setVariable("request", request);
        context.setVariable("requestTypeLabel", requestTypeLabel(request.requestType()));
        context.setVariable("statusLabel", requestStatusLabel(request.status()));
        context.setVariable("generatedAt", Instant.now());
        context.setVariable("fmt", new CashDocumentTemplateFormatter(appLocale));
        brand(context);
        if (templatePrinter != null) {
            java.util.Optional<byte[]> printed = templatePrinter.tryRender(CashReportDocumentData.REQUEST, requestData(request));
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return renderToPdf(templateEngine.process("cash/request-document", context));
    }

    @Override
    public byte[] generateSessionClosingReport(String sessionNumber) {
        CashSession session = session(sessionNumber);
        CashSessionSummaryResponse summary = cashRegisterService.sessionSummary(sessionNumber);
        CashSessionStatisticsResponse statistics = cashStatisticsService.sessionStatistics(sessionNumber);
        List<CashMovementResponse> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(cashRegisterMapper::toCashMovementResponse)
                .toList();

        Context context = new Context(appLocale);
        context.setVariable("summary", summary);
        context.setVariable("statistics", statistics);
        context.setVariable("movements", movements);
        context.setVariable("reviewedBy", session.getReviewedBy());
        context.setVariable("generatedAt", Instant.now());
        context.setVariable("fmt", new CashDocumentTemplateFormatter(appLocale));
        brand(context);
        if (templatePrinter != null) {
            java.util.Optional<byte[]> printed = templatePrinter.tryRender(CashReportDocumentData.CLOSING,
                    closingData(summary, movements, session.getReviewedBy()));
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return renderToPdf(templateEngine.process("cash/session-closing-report", context));
    }

    /** En-tete commun · logo encre et identite de l'exploitant. */
    private void brand(Context context) {
        context.setVariable("logo", brandLogo.base64());
        context.setVariable("operator", operatorIdentityService.current());
    }

    private byte[] renderToPdf(String html) {
        try {
            return PdfRenderer.render(html);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate cash document PDF", ex);
        }
    }

    private String documentTypeLabel(CashDocumentType documentType, CashMovementType movementType) {
        if (documentType != null) {
            return switch (documentType) {
                case CASH_VOUCHER -> "Pièce de caisse";
                case ENTRY_VOUCHER -> "Bon d'entrée";
                case EXIT_VOUCHER -> "Bon de sortie";
                case RECEIPT -> "Reçu";
                case INVOICE -> "Facture";
                case EXPENSE_NOTE -> "Note de frais";
                case BANK_SLIP -> "Bordereau de remise en banque";
                case TRANSFER_NOTE -> "Avis de virement";
                case EXTERNAL_REFERENCE, OTHER -> "Pièce justificative";
            };
        }
        if (movementType != null) {
            return switch (movementType) {
                case SAFE_DEPOSIT -> "Bordereau de remise de fonds";
                case CASH_OUT -> "Bon de sortie de caisse";
                case CASH_IN -> "Bon d'entrée de caisse";
                case ADJUSTMENT -> "Pièce de justification";
                default -> "Pièce justificative de caisse";
            };
        }
        return "Pièce justificative de caisse";
    }

    private String requestTypeLabel(CashRequestType type) {
        if (type == null) {
            return "Demande de caisse";
        }
        return switch (type) {
            case JUSTIFICATIF -> "Demande de justificatif";
            case REMISE_DE_FONDS -> "Demande de remise de fonds";
            case CASH_ADVANCE -> "Demande d'avance de caisse";
            case EXPENSE_REIMBURSEMENT -> "Demande de remboursement de frais";
        };
    }

    private String requestStatusLabel(CashRequestStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "En attente de validation";
            case APPROVED -> "Approuvée";
            case REJECTED -> "Rejetée";
            case EXECUTED -> "Exécutée";
            case CANCELLED -> "Annulée";
        };
    }

    private CashMovement movement(String movementNumber) {
        if (!StringUtils.hasText(movementNumber)) {
            throw new BadRequestException("Cash movement number is required");
        }
        return movementRepository.findByMovementNumber(movementNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash movement not found: " + movementNumber));
    }

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Cash session number is required");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash session not found: " + sessionNumber));
    }

    public static final class CashDocumentTemplateFormatter {

        private static final String EMPTY_VALUE = "";
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        private final Locale locale;

        public CashDocumentTemplateFormatter(Locale locale) {
            this.locale = locale;
        }

        public String money(BigDecimal amount) {
            return money(amount, null);
        }

        public String money(BigDecimal amount, String currency) {
            if (amount == null) {
                return EMPTY_VALUE;
            }
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            String formatted = nf.format(amount.setScale(0, RoundingMode.HALF_UP));
            return StringUtils.hasText(currency) ? formatted + " " + currency.trim() : formatted;
        }

        public String timestamp(Instant value) {
            return value == null ? EMPTY_VALUE : DATE_TIME_FORMATTER.format(value.atZone(ZoneId.systemDefault()));
        }

        public String time(Instant value) {
            return value == null ? EMPTY_VALUE : TIME_FORMATTER.format(value.atZone(ZoneId.systemDefault()));
        }

        public String movementTypeLabel(CashMovementType type) {
            if (type == null) {
                return EMPTY_VALUE;
            }
            return switch (type) {
                case OPENING_FLOAT -> "Fonds d'ouverture";
                case PAYMENT -> "Paiement encaissé";
                case REFUND -> "Remboursement";
                case CASH_IN -> "Entrée de caisse";
                case CASH_OUT -> "Sortie de caisse";
                case SAFE_DEPOSIT -> "Remise en coffre";
                case TRANSFER_IN -> "Transfert entrant";
                case TRANSFER_OUT -> "Transfert sortant";
                case ADJUSTMENT -> "Ajustement";
                case CLOSING_COUNT -> "Comptage de clôture";
                case EXCHANGE -> "Échange de monnaie";
                case VARIANCE -> "Écart de caisse";
            };
        }
    }
}
