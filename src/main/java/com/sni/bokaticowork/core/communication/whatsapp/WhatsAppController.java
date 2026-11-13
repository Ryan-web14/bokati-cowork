package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administration de WhatsApp (Infobip) : modeles de message, envois, message d'essai. */
@RestController
@RequestMapping(ApiPath.V1 + "/admin/whatsapp")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')")
public class WhatsAppController {

    private final WhatsAppService whatsapp;

    public record TestRequest(String phone, String purpose, List<String> placeholders, String text) {
    }

    @GetMapping("/templates")
    public ResponseEntity<List<WhatsAppTemplate>> templates() {
        return ResponseEntity.ok(whatsapp.templates());
    }

    @Audited(module = "WHATSAPP", action = "TEMPLATE_CREATE", ressource = "whatsapp_template")
    @PostMapping("/templates")
    public ResponseEntity<WhatsAppTemplate> create(@RequestBody WhatsAppService.TemplateRequest request) {
        return ResponseEntity.ok(whatsapp.createTemplate(request));
    }

    @Audited(module = "WHATSAPP", action = "TEMPLATE_DEFAULTS", ressource = "whatsapp_template")
    @PostMapping("/templates/defaults")
    public ResponseEntity<List<WhatsAppTemplate>> defaults() {
        return ResponseEntity.ok(whatsapp.createDefaults());
    }

    @PostMapping("/templates/sync")
    public ResponseEntity<List<WhatsAppTemplate>> sync() {
        return ResponseEntity.ok(whatsapp.sync());
    }

    @GetMapping("/messages")
    public ResponseEntity<List<WhatsAppMessage>> messages(@RequestParam(required = false) String reference) {
        return ResponseEntity.ok(whatsapp.messages(reference));
    }

    @Audited(module = "WHATSAPP", action = "TEST_MESSAGE", ressource = "whatsapp_message")
    @PostMapping("/test")
    public ResponseEntity<WhatsAppMessageSender.Result> test(@RequestBody TestRequest request) {
        return ResponseEntity.ok(whatsapp.send(request.phone(), request.purpose(), request.placeholders(), request.text(), "TEST"));
    }
}
