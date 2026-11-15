package com.sni.bokaticowork.features.document.template.controller;

import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'apercu d'un modele repond quel que soit l'en-tete Accept du frontend : avec {@code produces}, un
 * appel en {@code Accept: application/json} finissait en 500 (« No acceptable representation »).
 */
class TemplatePreviewAcceptTest {

    @Test
    void previewAnswersAPdfEvenWhenJsonIsAccepted() throws Exception {
        DocumentTemplateService service = mock(DocumentTemplateService.class);
        when(service.preview(eq("CASH_IN_VOUCHER"), isNull(), any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});
        when(service.previewEmail(eq("EMAIL_PAYMENT_CONFIRMATION"), any())).thenReturn("<p>ok</p>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocumentTemplateController(service)).build();

        mvc.perform(post("/sni/api/v1/document-templates/CASH_IN_VOUCHER/preview").accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(post("/sni/api/v1/document-templates/EMAIL_PAYMENT_CONFIRMATION/preview-email").accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
