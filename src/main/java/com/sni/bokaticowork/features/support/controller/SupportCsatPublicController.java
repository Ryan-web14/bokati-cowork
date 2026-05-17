package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SubmitCsatRequest;
import com.sni.bokaticowork.features.support.service.implementation.CsatTokenService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/public/support/csat")
@RequiredArgsConstructor
public class SupportCsatPublicController {

    private final SupportTicketService ticketService;
    private final CsatTokenService tokenService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> submit(
            @RequestParam String ticket,
            @RequestParam Integer score,
            @RequestParam String token) {

        if (!tokenService.validate(ticket, score, token)) {
            return ResponseEntity.ok(page("INVALID_TOKEN", ticket, 0));
        }
        if (score < 1 || score > 5) {
            return ResponseEntity.ok(page("INVALID_SCORE", ticket, score));
        }

        try {
            ticketService.submitCsat(ticket, new SubmitCsatRequest(score, null));
            return ResponseEntity.ok(page("SUCCESS", ticket, score));
        } catch (BadRequestException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            String state = msg.contains("already") ? "ALREADY_SUBMITTED" : "NOT_ELIGIBLE";
            return ResponseEntity.ok(page(state, ticket, score));
        }
    }

    // ── Inline HTML response page ─────────────────────────────────────

    private String page(String state, String ticket, int score) {
        String title;
        String icon;
        String heading;
        String body;

        switch (state) {
            case "SUCCESS" -> {
                title   = "Merci pour votre avis !";
                icon    = "✓";
                heading = "Votre note a bien été enregistrée";
                body    = "Vous avez attribué <strong>" + score + " étoile" + (score > 1 ? "s" : "") + "</strong> "
                        + "pour le ticket <strong>" + esc(ticket) + "</strong>.<br>"
                        + "Votre retour nous aide à améliorer notre service.";
            }
            case "ALREADY_SUBMITTED" -> {
                title   = "Déjà noté";
                icon    = "i";
                heading = "Vous avez déjà évalué ce ticket";
                body    = "La note pour le ticket <strong>" + esc(ticket) + "</strong> a déjà été enregistrée.";
            }
            case "NOT_ELIGIBLE" -> {
                title   = "Ticket non éligible";
                icon    = "!";
                heading = "Ce ticket ne peut pas encore être évalué";
                body    = "Le ticket doit être résolu ou fermé pour pouvoir être noté.";
            }
            default -> {
                title   = "Lien invalide";
                icon    = "✕";
                heading = "Lien invalide ou expiré";
                body    = "Ce lien d'évaluation n'est pas valide. Veuillez utiliser le lien fourni dans l'email de résolution de votre ticket.";
            }
        }

        String stars = "SUCCESS".equals(state) ? buildStars(score) : "";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s — Elle A Osé Support</title>
                  <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{background:#f0eef5;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;
                         display:flex;align-items:center;justify-content:center;min-height:100vh;padding:24px}
                    .card{background:#fff;border-radius:8px;box-shadow:0 8px 32px rgba(30,10,60,.12);
                          max-width:480px;width:100%%;overflow:hidden}
                    .card-header{background:#1a0a2e;padding:24px;text-align:center}
                    .card-stripe{height:3px;background:linear-gradient(90deg,#c9a84c 0%%,#8b5cf6 55%%,#4c1d95 100%%)}
                    .brand{font-size:11px;font-weight:700;letter-spacing:3px;color:#fff;text-transform:uppercase}
                    .icon-wrap{width:56px;height:56px;border-radius:50%%;border:2px solid #c9a84c;
                               display:flex;align-items:center;justify-content:center;
                               margin:16px auto 0;font-size:22px;font-weight:700;color:#c9a84c}
                    .card-body{padding:28px 32px 32px}
                    h1{font-size:20px;font-weight:800;color:#1a0a2e;margin-bottom:10px;text-align:center}
                    p{font-size:13px;color:#5c4f6e;line-height:1.7;text-align:center}
                    p strong{color:#1a0a2e}
                    .stars{font-size:28px;letter-spacing:6px;text-align:center;margin:16px 0 4px}
                    .ticket-ref{display:inline-block;background:#f0eef5;border:1px solid #e8e0f0;
                                border-radius:4px;padding:4px 12px;font-size:11px;font-weight:700;
                                color:#7c6d8f;font-family:monospace;letter-spacing:1px;margin-top:14px}
                    .footer{text-align:center;padding:14px 24px;background:#f7f5fb;
                            border-top:1px solid #e8e0f0;font-size:10px;color:#9b8caa}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="card-header">
                      <div class="brand">Elle A Osé — Support client</div>
                      <div class="icon-wrap">%s</div>
                    </div>
                    <div class="card-stripe"></div>
                    <div class="card-body">
                      <h1>%s</h1>
                      %s
                      <p>%s</p>
                      <div style="text-align:center"><span class="ticket-ref">%s</span></div>
                    </div>
                    <div class="footer">Elle A Osé &mdash; Coworking &amp; Support</div>
                  </div>
                </body>
                </html>
                """.formatted(title, icon, heading, stars, body, esc(ticket));
    }

    private String buildStars(int score) {
        StringBuilder sb = new StringBuilder("<div class=\"stars\">");
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= score ? "★" : "☆");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
