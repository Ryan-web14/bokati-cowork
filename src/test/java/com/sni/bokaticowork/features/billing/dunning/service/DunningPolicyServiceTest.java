package com.sni.bokaticowork.features.billing.dunning.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningNoticeRepository;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Une politique active par segment · les paliers dans l'ordre des jours. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DunningPolicyServiceTest {

    @Mock private DunningPolicyRepository policyRepository;
    @Mock private DunningNoticeRepository noticeRepository;

    @InjectMocks
    private DunningPolicyService service;

    @BeforeEach
    void setUp() {
        when(policyRepository.findByPolicyCode(anyString())).thenReturn(Optional.empty());
        when(policyRepository.findFirstBySegmentAndActiveTrue(any())).thenReturn(Optional.empty());
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static DunningPolicyService.StepSpec step(int days, DunningStep.Action action) {
        return new DunningPolicyService.StepSpec(days, action, null, "s", "m");
    }

    @Test
    void stepsAreNumberedAndMustBeInDayOrder() {
        DunningPolicy saved = service.save(new DunningPolicyService.PolicySpec("DNP-X", "Test", DunningPolicy.Segment.CUSTOMER, null, null,
                List.of(step(3, DunningStep.Action.REMINDER), step(10, DunningStep.Action.FORMAL_NOTICE), step(10, DunningStep.Action.SUSPEND))));

        assertEquals(List.of(1, 2, 3), saved.getSteps().stream().map(DunningStep::getStepOrder).toList());
        assertEquals(DunningStep.Channel.EMAIL, saved.getSteps().getFirst().getChannel());
        assertEquals(DunningPolicy.Tone.STANDARD, saved.getTone());

        assertThrows(ConflictException.class, () -> service.save(new DunningPolicyService.PolicySpec("DNP-Y", "Test", null, null, null,
                List.of(step(10, DunningStep.Action.REMINDER), step(3, DunningStep.Action.REMINDER)))));
        assertThrows(BadRequestException.class, () -> service.save(new DunningPolicyService.PolicySpec("DNP-Z", "Test", null, null, null, List.of())));
        assertThrows(BadRequestException.class, () -> service.save(new DunningPolicyService.PolicySpec("DNP-Z", "Test", null, null, null,
                List.of(step(-1, DunningStep.Action.REMINDER)))));
    }

    @Test
    void activatingAPolicyDeactivatesTheOtherOneOfTheSegment() {
        DunningPolicy other = DunningPolicy.builder().policyCode("DNP-OLD").segment(DunningPolicy.Segment.DEFAULT).active(true).build();
        when(policyRepository.findFirstBySegmentAndActiveTrue(DunningPolicy.Segment.DEFAULT)).thenReturn(Optional.of(other));

        DunningPolicy saved = service.save(new DunningPolicyService.PolicySpec("DNP-NEW", "Nouvelle", DunningPolicy.Segment.DEFAULT, null, true,
                List.of(step(5, DunningStep.Action.REMINDER))));

        assertTrue(saved.getActive());
        assertFalse(other.getActive());
        verify(policyRepository).save(other);
    }

    @Test
    void savingAgainReplacesTheStepsInBlock() {
        DunningPolicy existing = DunningPolicy.builder().policyCode("DNP-X").segment(DunningPolicy.Segment.DEFAULT).build();
        existing.getSteps().add(DunningStep.builder().policy(existing).stepOrder(1).daysAfterDue(3).action(DunningStep.Action.REMINDER).build());
        when(policyRepository.findByPolicyCode("DNP-X")).thenReturn(Optional.of(existing));

        DunningPolicy saved = service.save(new DunningPolicyService.PolicySpec("DNP-X", "Renommée", null, DunningPolicy.Tone.FIRM, null,
                List.of(step(1, DunningStep.Action.REMINDER), step(15, DunningStep.Action.SUSPEND))));

        assertSame(existing, saved);
        assertEquals("Renommée", saved.getName());
        assertEquals(2, saved.getSteps().size());
        assertEquals(15, saved.getSteps().get(1).getDaysAfterDue());
    }
}
