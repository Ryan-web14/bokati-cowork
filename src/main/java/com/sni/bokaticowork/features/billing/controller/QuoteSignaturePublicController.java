package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.features.billing.dto.request.ConfirmSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.QuoteSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/public/quotes/sign")
@RequiredArgsConstructor
public class QuoteSignaturePublicController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuoteSignatureService quoteSignatureService;
    private final BillingDocumentService billingDocumentService;

    @GetMapping(value = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> signaturePage(@PathVariable String token) {
        BillingDocumentSignature sig = quoteSignatureService.resolveToken(token);
        BillingDocumentResponse quote = billingDocumentService.get(sig.getDocument().getDocumentNumber());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildPage(sig, quote));
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<BillingDocumentResponse> confirmSignature(@PathVariable String token,
                                                                    @Valid @RequestBody ConfirmSignatureRequest request,
                                                                    HttpServletRequest http) {
        ConfirmSignatureRequest enriched = new ConfirmSignatureRequest(
                request.signerName(),
                request.signerEmail(),
                request.signatureImageBase64(),
                http.getRemoteAddr(),
                http.getHeader("User-Agent")
        );
        return ResponseEntity.ok(quoteSignatureService.confirmSignature(token, enriched));
    }

    //  Page HTML

    private String buildPage(BillingDocumentSignature sig, BillingDocumentResponse quote) {
        String expiresStr = sig.getExpiresAt()
                .atZone(ZoneId.of("Africa/Brazzaville"))
                .format(DATE_FMT);
        String totalStr = formatAmount(quote.totalAmount(), quote.currency());
        String dueDateStr = quote.dueDate() != null ? quote.dueDate().format(DATE_FMT) : "";
        String customMsg  = sig.getCustomMessage() != null
                ? "<p style='background:#f0eef5;border-left:3px solid #8b5cf6;padding:10px 14px;"
                + "border-radius:4px;font-size:13px;color:#5c4f6e;margin:16px 0'>"
                + escapeHtml(sig.getCustomMessage()) + "</p>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>Signature · %s</title>
                  <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{background:#f0eef5;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;
                         min-height:100vh;padding:24px;display:flex;align-items:flex-start;justify-content:center}
                    .card{background:#fff;border-radius:8px;box-shadow:0 8px 32px rgba(30,10,60,.12);
                          max-width:560px;width:100%%;overflow:hidden;margin-top:16px}
                    .card-header{background:#1a0a2e;padding:20px 24px;text-align:center}
                    .stripe{height:3px;background:linear-gradient(90deg,#c9a84c 0%%,#8b5cf6 55%%,#4c1d95 100%%)}
                    .brand{font-size:11px;font-weight:700;letter-spacing:3px;color:#fff;text-transform:uppercase}
                    .card-body{padding:28px 32px 32px}
                    h1{font-size:19px;font-weight:800;color:#1a0a2e;margin-bottom:8px}
                    .meta{font-size:12px;color:#9c8fb0;margin-bottom:16px}
                    .summary{background:#f9f7fd;border-radius:6px;padding:14px 16px;margin:16px 0}
                    .summary-row{display:flex;justify-content:space-between;font-size:13px;
                                 color:#5c4f6e;padding:3px 0}
                    .summary-row.total{font-weight:800;color:#1a0a2e;font-size:15px;
                                       border-top:1px solid #e8e0f0;margin-top:6px;padding-top:8px}
                    .canvas-wrap{border:2px solid #e8e0f0;border-radius:6px;position:relative;
                                 margin:16px 0;background:#fafaf9;cursor:crosshair}
                    canvas{display:block;width:100%%;touch-action:none}
                    .canvas-label{font-size:11px;color:#9c8fb0;text-align:center;padding:4px 0 8px}
                    .btn{display:block;width:100%%;padding:14px;border:none;border-radius:6px;
                         font-size:15px;font-weight:700;cursor:pointer;letter-spacing:.5px;margin-top:8px}
                    .btn-clear{background:#f0eef5;color:#7c6d8f;margin-bottom:6px}
                    .btn-sign{background:#1a0a2e;color:#fff}
                    .btn-sign:disabled{opacity:.5;cursor:not-allowed}
                    .expiry{font-size:11px;color:#9c8fb0;text-align:center;margin-top:14px}
                    #msg{display:none;padding:14px;border-radius:6px;font-size:13px;
                         font-weight:600;text-align:center;margin-top:12px}
                    .success{background:#d1fae5;color:#065f46}
                    .error{background:#fee2e2;color:#991b1b}
                    #form-area{}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="card-header">
                      <span class="brand">Bokati Cowork</span>
                    </div>
                    <div class="stripe"></div>
                    <div class="card-body">
                      <h1>Signature électronique</h1>
                      <div class="meta">Devis %s &nbsp;·&nbsp; %s</div>
                      %s
                      <div class="summary">
                        <div class="summary-row"><span>Client</span><span>%s</span></div>
                        <div class="summary-row"><span>Échéance</span><span>%s</span></div>
                        <div class="summary-row total"><span>Total</span><span>%s</span></div>
                      </div>
                      <div id="form-area">
                        <p style="font-size:13px;color:#5c4f6e;margin-bottom:12px">
                          Signez ci-dessous pour accepter ce devis.
                        </p>
                        <div>
                          <label style="font-size:12px;color:#7c6d8f;display:block;margin-bottom:6px">
                            Votre nom complet
                          </label>
                          <input id="signerName" type="text" value="%s"
                                 placeholder="Prénom Nom"
                                 style="width:100%%;padding:10px 12px;border:1px solid #e8e0f0;
                                        border-radius:6px;font-size:14px;color:#1a0a2e;
                                        outline:none;margin-bottom:12px">
                        </div>
                        <div class="canvas-wrap">
                          <canvas id="sigCanvas" height="160"></canvas>
                          <div class="canvas-label">Dessinez votre signature ici</div>
                        </div>
                        <button class="btn btn-clear" onclick="clearCanvas()">Effacer</button>
                        <button class="btn btn-sign" id="btnSign" onclick="submit()">
                          Signer et accepter le devis
                        </button>
                        <p class="expiry">Lien valable jusqu'au %s</p>
                      </div>
                      <div id="msg"></div>
                    </div>
                  </div>
                  <script>
                    const TOKEN = '%s';
                    const canvas = document.getElementById('sigCanvas');
                    const ctx = canvas.getContext('2d');
                    let drawing = false, hasSig = false;

                    function resize() {
                      const w = canvas.parentElement.clientWidth;
                      canvas.width = w;
                      canvas.height = 160;
                      ctx.strokeStyle = '#1a0a2e';
                      ctx.lineWidth = 2.5;
                      ctx.lineCap = 'round';
                      ctx.lineJoin = 'round';
                    }
                    resize();
                    window.addEventListener('resize', resize);

                    function pos(e) {
                      const r = canvas.getBoundingClientRect();
                      const src = e.touches ? e.touches[0] : e;
                      return {x: src.clientX - r.left, y: src.clientY - r.top};
                    }
                    canvas.addEventListener('mousedown',  e => { drawing = true; ctx.beginPath(); const p=pos(e); ctx.moveTo(p.x,p.y); });
                    canvas.addEventListener('mousemove',  e => { if (!drawing) return; const p=pos(e); ctx.lineTo(p.x,p.y); ctx.stroke(); hasSig=true; });
                    canvas.addEventListener('mouseup',    () => drawing = false);
                    canvas.addEventListener('mouseleave', () => drawing = false);
                    canvas.addEventListener('touchstart', e => { e.preventDefault(); drawing=true; ctx.beginPath(); const p=pos(e); ctx.moveTo(p.x,p.y); });
                    canvas.addEventListener('touchmove',  e => { e.preventDefault(); if(!drawing)return; const p=pos(e); ctx.lineTo(p.x,p.y); ctx.stroke(); hasSig=true; });
                    canvas.addEventListener('touchend',   () => drawing=false);

                    function clearCanvas() { ctx.clearRect(0,0,canvas.width,canvas.height); hasSig=false; }

                    async function submit() {
                      const name = document.getElementById('signerName').value.trim();
                      if (!name) { showMsg('Veuillez saisir votre nom complet.', false); return; }
                      if (!hasSig) { showMsg('Veuillez dessiner votre signature.', false); return; }
                      const btn = document.getElementById('btnSign');
                      btn.disabled = true;
                      btn.textContent = 'Envoi en cours…';
                      const imageBase64 = canvas.toDataURL('image/png');
                      try {
                        const r = await fetch('/public/quotes/sign/' + TOKEN + '/confirm', {
                          method: 'POST',
                          headers: {'Content-Type':'application/json'},
                          body: JSON.stringify({ signerName: name, signatureImageBase64: imageBase64 })
                        });
                        if (r.ok) {
                          document.getElementById('form-area').style.display = 'none';
                          showMsg('✓ Devis signé et accepté avec succès. Merci !', true);
                        } else {
                          const err = await r.json().catch(() => ({}));
                          showMsg(err.message || 'Une erreur est survenue.', false);
                          btn.disabled = false;
                          btn.textContent = 'Signer et accepter le devis';
                        }
                      } catch(e) {
                        showMsg('Erreur réseau. Veuillez réessayer.', false);
                        btn.disabled = false;
                        btn.textContent = 'Signer et accepter le devis';
                      }
                    }

                    function showMsg(text, ok) {
                      const el = document.getElementById('msg');
                      el.textContent = text;
                      el.className = ok ? 'success' : 'error';
                      el.style.display = 'block';
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                        escapeHtml(quote.documentNumber()),
                        escapeHtml(quote.documentNumber()),
                        escapeHtml(quote.title() != null ? quote.title() : ""),
                        customMsg,
                        escapeHtml(quote.customerName()),
                        dueDateStr,
                        totalStr,
                        escapeHtml(sig.getSignerName() != null ? sig.getSignerName() : ""),
                        expiresStr,
                        sig.getSignatureToken()
                );
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "";
        return String.format("%,.0f %s", amount, currency != null ? currency : "");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
