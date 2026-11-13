package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.CollectionDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.PaymentPromise;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.PaymentPromiseRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C12 : une promesse au 30 suspend les relances jusqu'au 30 ; passee la date, elle est rompue
 * si rien n'est arrive et les relances reprennent. Un plan de reglement repartit le reste du ; un
 * plan long demande le droit d'approbation.
 */
class CollectionC12Test {

    private final PaymentPromiseRepository promises = mock(PaymentPromiseRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final PaymentScheduleService schedules = mock(PaymentScheduleService.class);
    private CollectionService service;
    private final List<PaymentPromise> stored = new ArrayList<>();
    private BillingDocument invoice;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<PaymentScheduleService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(schedules);
        service = new CollectionService(promises, documents, sequences, staff, provider);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("recouvrement@elleaose.com", "Recouvrement", null));
        when(sequences.next("billing_payment_promise")).thenReturn("PRM-1");
        invoice = BillingDocument.builder().documentNumber("FAC-70").documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.OVERDUE)
                .balanceDue(new BigDecimal("300001")).build();
        when(documents.findByDocumentNumber("FAC-70")).thenReturn(Optional.of(invoice));
        when(promises.save(any())).thenAnswer(invocation -> {
            PaymentPromise promise = invocation.getArgument(0);
            if (!stored.contains(promise)) {
                stored.add(promise);
            }
            return promise;
        });
        when(promises.findByDocumentNumberAndStatus(anyString(), eq("OPEN")))
                .thenAnswer(invocation -> stored.stream().filter(p -> "OPEN".equals(p.getStatus())).toList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldHoldDunningUntilThePromisedDateThenMarkItBroken() {
        service.promise("FAC-70", new CollectionDtos.PromiseRequest(LocalDate.now().plusDays(10), null, "PHONE", "Virement annoncé"));
        assertThat(service.holdReason("FAC-70")).isPresent();

        stored.get(0).setPromisedDate(LocalDate.now().minusDays(1));
        assertThat(service.holdReason("FAC-70")).isEmpty();
        assertThat(stored.get(0).getStatus()).isEqualTo("BROKEN");
    }

    @Test
    void shouldMarkThePromiseKeptWhenThePaymentArrived() {
        service.promise("FAC-70", new CollectionDtos.PromiseRequest(LocalDate.now(), new BigDecimal("100000"), null, null));
        stored.get(0).setPromisedDate(LocalDate.now().minusDays(1));
        invoice.setBalanceDue(new BigDecimal("200001"));

        service.holdReason("FAC-70");

        assertThat(stored.get(0).getStatus()).isEqualTo("KEPT");
    }

    @Test
    void shouldSplitTheBalanceIntoEqualInstallmentsAndGuardLongPlans() {
        service.plan("FAC-70", new CollectionDtos.PlanRequest(3, LocalDate.of(2026, 12, 15), 1, null));

        ArgumentCaptor<CreatePaymentScheduleRequest> request = ArgumentCaptor.forClass(CreatePaymentScheduleRequest.class);
        verify(schedules).createSchedule(eq("FAC-70"), request.capture());
        assertThat(request.getValue().installments()).extracting(input -> input.amount().toPlainString())
                .containsExactly("100000", "100000", "100001");
        assertThat(request.getValue().installments().get(2).dueDate()).isEqualTo(LocalDate.of(2027, 2, 15));

        assertThatThrownBy(() -> service.plan("FAC-70", new CollectionDtos.PlanRequest(6, LocalDate.of(2026, 12, 15), 1, null)))
                .isInstanceOf(ForbiddenException.class);
    }
}
