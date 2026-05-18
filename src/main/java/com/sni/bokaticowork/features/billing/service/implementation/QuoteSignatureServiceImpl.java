package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.request.ConfirmSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.request.RequestSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentSignatureResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSignatureRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.QuoteSignatureService;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QuoteSignatureServiceImpl implements QuoteSignatureService {

    private static final long TOKEN_VALIDITY_DAYS = 7;

    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentSignatureRepository signatureRepository;
    private final BillingEventWriter eventWriter;
    private final BillingDocumentMapper mapper;
    private final DefaultEmailSender emailSender;

    @Value("${app.api-base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public BillingDocumentSignatureResponse requestSignature(String quoteNumber, RequestSignatureRequest request) {
        BillingDocument quote = billingDocumentService.serviceByNumber(quoteNumber);
        if (quote.getDocumentType() != BillingDocumentType.QUOTE) {
            throw new BadRequestException("Signature électronique uniquement disponible pour les devis");
        }
        if (quote.getStatus() == BillingDocumentStatus.CANCELLED
                || quote.getStatus() == BillingDocumentStatus.CONVERTED
                || quote.getStatus() == BillingDocumentStatus.REJECTED) {
            throw new BadRequestException("Impossible de demander une signature sur un devis " + quote.getStatus());
        }

        String signerName  = StringUtils.hasText(request != null ? request.signerName() : null)  ? request.signerName()  : quote.getCustomerName();
        String signerEmail = StringUtils.hasText(request != null ? request.signerEmail() : null) ? request.signerEmail() : quote.getCustomerEmail();
        if (!StringUtils.hasText(signerEmail)) {
            throw new BadRequestException("Aucun email disponible pour envoyer la demande de signature");
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        token = token.substring(0, 64);

        BillingDocumentSignature sig = signatureRepository.save(BillingDocumentSignature.builder()
                .document(quote)
                .signerName(signerName)
                .signerEmail(signerEmail)
                .signatureToken(token)
                .customMessage(request != null ? request.customMessage() : null)
                .expiresAt(Instant.now().plus(TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS))
                .build());

        sendSignatureEmail(signerEmail, signerName, quoteNumber, token,
                request != null ? request.customMessage() : null);

        eventWriter.write(quote, "QUOTE_SIGNATURE_REQUESTED", Map.of("signerEmail", signerEmail));

        return toResponse(sig);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocumentSignature resolveToken(String token) {
        BillingDocumentSignature sig = signatureRepository.findBySignatureToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Lien de signature invalide ou expiré"));
        if (sig.getStatus() == BillingSignatureStatus.SIGNED) {
            throw new BadRequestException("Ce devis a déjà été signé");
        }
        if (sig.getExpiresAt().isBefore(Instant.now())) {
            sig.setStatus(BillingSignatureStatus.EXPIRED);
            signatureRepository.save(sig);
            throw new BadRequestException("Le lien de signature a expiré");
        }
        return sig;
    }

    @Override
    public BillingDocumentResponse confirmSignature(String token, ConfirmSignatureRequest request) {
        BillingDocumentSignature sig = resolveToken(token);
        sig.setSignerName(request.signerName());
        sig.setSignerEmail(request.signerEmail() != null ? request.signerEmail() : sig.getSignerEmail());
        sig.setSignatureImageBase64(request.signatureImageBase64());
        sig.setIpAddress(request.ipAddress());
        sig.setUserAgent(request.userAgent());
        sig.setSignedAt(Instant.now());
        sig.setStatus(BillingSignatureStatus.SIGNED);
        signatureRepository.save(sig);

        BillingDocument quote = sig.getDocument();
        if (quote.getStatus() != BillingDocumentStatus.ACCEPTED) {
            quote.setStatus(BillingDocumentStatus.ACCEPTED);
            eventWriter.write(quote, "QUOTE_SIGNED_AND_ACCEPTED",
                    Map.of("signerName", request.signerName(), "signerEmail", sig.getSignerEmail()));
        }

        return mapper.toResponse(quote);
    }

    private void sendSignatureEmail(String to, String signerName, String quoteNumber, String token, String customMessage) {
        String signatureUrl = baseUrl + "/public/quotes/sign/" + token;
        String body = buildSignatureEmailHtml(signerName, quoteNumber, signatureUrl, customMessage);
        try {
            emailSender.sendHtmlEmail(to, "Signature électronique — Devis " + quoteNumber, body);
        } catch (Exception ex) {
            log.warn("Signature email send failed for {}: {}", quoteNumber, ex.getMessage());
        }
    }

    private String buildSignatureEmailHtml(String signerName, String quoteNumber, String url, String customMessage) {
        String msg = StringUtils.hasText(customMessage)
                ? "<p style='color:#5c4f6e;font-size:13px;line-height:1.7'>" + escapeHtml(customMessage) + "</p>"
                : "";
        return """
                <!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"></head>
                <body style="font-family:Helvetica Neue,Arial,sans-serif;background:#f0eef5;padding:32px">
                  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:8px;
                              box-shadow:0 8px 32px rgba(30,10,60,.12);overflow:hidden">
                    <div style="background:#1a0a2e;padding:24px;text-align:center">
                      <span style="font-size:11px;font-weight:700;letter-spacing:3px;color:#fff;text-transform:uppercase">
                        Bokati Cowork
                      </span>
                    </div>
                    <div style="height:3px;background:linear-gradient(90deg,#c9a84c 0%%,#8b5cf6 55%%,#4c1d95 100%%)"></div>
                    <div style="padding:32px">
                      <h1 style="font-size:20px;color:#1a0a2e;margin-bottom:12px">Signature de votre devis</h1>
                      <p style="color:#5c4f6e;font-size:13px;line-height:1.7">
                        Bonjour <strong>%s</strong>,<br>
                        Le devis <strong>%s</strong> est prêt à être signé électroniquement.
                      </p>
                      %s
                      <div style="text-align:center;margin:28px 0">
                        <a href="%s" style="display:inline-block;background:#1a0a2e;color:#fff;
                                            text-decoration:none;padding:14px 32px;border-radius:6px;
                                            font-weight:700;font-size:14px;letter-spacing:1px">
                          Signer le devis
                        </a>
                      </div>
                      <p style="color:#9c8fb0;font-size:11px;text-align:center">
                        Ce lien est valable 7 jours. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                      </p>
                    </div>
                  </div>
                </body></html>
                """.formatted(escapeHtml(signerName), escapeHtml(quoteNumber), msg, url);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private BillingDocumentSignatureResponse toResponse(BillingDocumentSignature sig) {
        return new BillingDocumentSignatureResponse(
                sig.getSignerName(),
                sig.getSignerEmail(),
                sig.getStatus(),
                sig.getSignedAt(),
                sig.getExpiresAt()
        );
    }
}
