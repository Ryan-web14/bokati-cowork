package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/**
 * Le solde d'un droit est libelle dans l'unite de sa definition, la reservation dans la sienne.
 * Confondre les deux retirait 1 heure a une reservation de cinq heures facturee en demi-journee.
 */
@ExtendWith(MockitoExtension.class)
class BookingEntitlementBridgeTest {

    @Mock private EntitlementService entitlementService;
    @Mock private UsageRecordService usageRecordService;
    @Mock private EntitlementDefinitionRepository definitionRepository;

    @Test
    void shouldDebitHoursForAnHourlyEntitlementWhateverTheBookingUnit() {
        // Le coeur du defaut : la demi-journee facturee ne dit rien de ce que le droit compte.
        // Cinq heures occupees retirent cinq heures, que la facture parle d'heures ou de forfait.
        assertThat(charge(EntitlementUnit.HOUR, 300, ResourceBookingUnit.HALF_DAY, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat(charge(EntitlementUnit.HOUR, 300, ResourceBookingUnit.HOUR, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat(charge(EntitlementUnit.HOUR, 600, ResourceBookingUnit.DAY, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void shouldReportTheEntitlementUnitNotTheBookingUnit() {
        BookingEntitlementBridge.Charge charge =
                charge(EntitlementUnit.HOUR, 300, ResourceBookingUnit.HALF_DAY, 1);

        assertThat(charge.unit()).isEqualTo(EntitlementUnit.HOUR);
        // La ligne de reservation doit annoncer la meme chose que le debit.
        assertThat(charge.displayUnit()).isEqualTo(ResourceBookingUnit.HOUR);
    }

    @Test
    void shouldCountTenHoursAsOneDay() {
        // La journee vaut dix heures, de 08h00 a 18h00 · meme reference que la facturation.
        assertThat(charge(EntitlementUnit.DAY, 600, ResourceBookingUnit.DAY, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("1.0000"));
        // Onze heures entament une seconde journee.
        assertThat(charge(EntitlementUnit.DAY, 660, ResourceBookingUnit.DAY, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("2.0000"));
        // Une courte reservation consomme une journee entiere du droit · c'est la nature d'un
        // droit compte en journees, et cela se voit desormais sur la ligne.
        assertThat(charge(EntitlementUnit.DAY, 120, ResourceBookingUnit.HOUR, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void shouldIgnoreDurationForVisitAndBookingEntitlements() {
        assertThat(charge(EntitlementUnit.VISIT, 600, ResourceBookingUnit.DAY, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(charge(EntitlementUnit.BOOKING, 60, ResourceBookingUnit.HOUR, 1).quantity())
                .isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void shouldMultiplyByTheNumberOfBookedPlaces() {
        assertThat(charge(EntitlementUnit.HOUR, 180, ResourceBookingUnit.HOUR, 3).quantity())
                .isEqualByComparingTo(new BigDecimal("9.0000"));
        assertThat(charge(EntitlementUnit.VISIT, 180, ResourceBookingUnit.HOUR, 3).quantity())
                .isEqualByComparingTo(new BigDecimal("3.0000"));
    }

    @Test
    void shouldRefuseAUnitNoDurationConvertsInto() {
        // La requete qui choisit le droit ne filtre pas sur l'unite · une definition en CREDIT peut
        // donc etre retenue pour une reservation. Debiter au juge reproduirait le defaut corrige.
        assertThatThrownBy(() -> charge(EntitlementUnit.CREDIT, 300, ResourceBookingUnit.HOUR, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ENT-TEST")
                .hasMessageContaining("CREDIT");

        assertThatThrownBy(() -> charge(EntitlementUnit.PERCENT, 300, ResourceBookingUnit.HOUR, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRefuseAnUnknownEntitlementCode() {
        BookingEntitlementBridge bridge =
                new BookingEntitlementBridge(entitlementService, usageRecordService, definitionRepository);
        lenient().when(definitionRepository.findByCodeIgnoreCase("ENT-ABSENT")).thenReturn(Optional.empty());

        Booking booking = Booking.builder()
                .entitlementCode("ENT-ABSENT").durationMinutes(60)
                .bookingUnit(ResourceBookingUnit.HOUR).quantity(1).build();

        assertThatThrownBy(() -> bridge.charge(booking))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ENT-ABSENT");
    }

    private BookingEntitlementBridge.Charge charge(EntitlementUnit unit, int minutes,
                                                   ResourceBookingUnit bookingUnit, int places) {
        BookingEntitlementBridge bridge =
                new BookingEntitlementBridge(entitlementService, usageRecordService, definitionRepository);
        lenient().when(definitionRepository.findByCodeIgnoreCase("ENT-TEST"))
                .thenReturn(Optional.of(EntitlementDefinition.builder()
                        .code("ENT-TEST").unit(unit).build()));

        return bridge.charge(Booking.builder()
                .entitlementCode("ENT-TEST")
                .durationMinutes(minutes)
                .bookingUnit(bookingUnit)
                .quantity(places)
                .build());
    }
}
