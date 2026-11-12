package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.features.payment.service.interfaces.CashAnomalyDetectionService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterAdminService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStaffService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Toutes les routes de caisse cohabitent : Spring refuse au demarrage deux routes ambigues, ce
 * test le voit sans lancer l'application.
 */
class CashRoutesMappingTest {

    private final CashRegisterService registers = mock(CashRegisterService.class);
    private final CashRegisterAdminService admin = mock(CashRegisterAdminService.class);
    private final CashSessionLifecycleService lifecycle = mock(CashSessionLifecycleService.class);
    private final CashStaffService staff = mock(CashStaffService.class);

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(
                new CashRegisterController(registers),
                new CashRegisterAdminController(admin),
                new CashSessionLifecycleController(lifecycle),
                new CashStaffController(staff),
                new CashRequestController(mock(CashRequestService.class)),
                new CashDocumentController(mock(CashDocumentPdfService.class)),
                new CashAnomalyFlagController(mock(CashAnomalyDetectionService.class)),
                new CashStatisticsController(mock(CashStatisticsService.class))
        ).build();
    }

    @Test
    @DisplayName("Les routes se resolvent vers le bon controleur, sans ambiguite")
    void routesResolve() throws Exception {
        MockMvc mvc = mvc();
        mvc.perform(get("/sni/api/v1/cash-registers/CSR-1"));
        org.mockito.Mockito.verify(admin).get("CSR-1");

        mvc.perform(get("/sni/api/v1/cash-registers/mine"));
        org.mockito.Mockito.verify(admin).myRegisters();

        mvc.perform(get("/sni/api/v1/cash-registers/CSR-1/expected-opening"));
        org.mockito.Mockito.verify(lifecycle).expectedOpening("CSR-1");

        mvc.perform(patch("/sni/api/v1/cash-registers/sessions/CSS-1/pause"));
        org.mockito.Mockito.verify(lifecycle).pause("CSS-1");

        mvc.perform(patch("/sni/api/v1/cash-registers/CSR-1/reactivate"));
        org.mockito.Mockito.verify(admin).reactivate("CSR-1");

        mvc.perform(get("/sni/api/v1/cash-registers/absences"));
        org.mockito.Mockito.verify(staff).absences(null, null);
        assertThat(mvc).isNotNull();
    }
}
