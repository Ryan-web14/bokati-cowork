package com.sni.bokaticowork.features.fund;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.model.FundRequestLine;
import com.sni.bokaticowork.features.fund.repository.FundRequestLineRepository;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Decaissement : la caissiere complete la nature d'operation des lignes qui n'en ont pas. */
class FundDisburseNatureTest {

    @Test
    void theCashierGivesTheMissingNatureAtDisbursement() throws Exception {
        FundRequestService service = mock(FundRequestService.class, CALLS_REAL_METHODS);
        FundRequestLineRepository lines = mock(FundRequestLineRepository.class);
        CashOperationNatureRepository natures = mock(CashOperationNatureRepository.class);
        ReflectionTestUtils.setField(service, "lineRepository", lines);
        ReflectionTestUtils.setField(service, "natureRepository", natures);
        when(natures.findByCode("FOURNITURES")).thenReturn(Optional.of(mock(CashOperationNature.class)));
        when(natures.findByCode("INCONNUE")).thenReturn(Optional.empty());
        FundRequestLine line = FundRequestLine.builder().lineNo(1).build();
        Method complete = FundRequestService.class.getDeclaredMethod("completeNatures", List.class, List.class);
        complete.setAccessible(true);

        complete.invoke(service, List.of(line), List.of(new FundDtos.LineNature(1, "fournitures")));
        assertThat(line.getNatureCode()).isEqualTo("FOURNITURES");

        assertThatThrownBy(() -> invoke(complete, service, line, new FundDtos.LineNature(1, "inconnue")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Nature d'opération inconnue");
        assertThatThrownBy(() -> invoke(complete, service, line, new FundDtos.LineNature(4, "fournitures")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Ligne 4 inconnue");
    }

    private static void invoke(Method method, FundRequestService service, FundRequestLine line, FundDtos.LineNature nature) throws Throwable {
        try {
            method.invoke(service, List.of(line), List.of(nature));
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
