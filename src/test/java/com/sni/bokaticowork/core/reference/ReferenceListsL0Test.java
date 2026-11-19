package com.sni.bokaticowork.core.reference;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.EnumValue;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ValueRequest;
import com.sni.bokaticowork.core.reference.model.ReferenceList;
import com.sni.bokaticowork.core.reference.model.ReferenceValue;
import com.sni.bokaticowork.core.reference.repository.ReferenceListRepository;
import com.sni.bokaticowork.core.reference.repository.ReferenceValueRepository;
import com.sni.bokaticowork.core.reference.service.ReferenceEnumRegistry;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.core.reference.service.SessionActor;
import com.sni.bokaticowork.features.booking.service.support.BookingReferenceEnums;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot L0 : socle des listes de choix (valeurs fixes, listes de l'administrateur, motifs). */
class ReferenceListsL0Test {

    private final ReferenceListRepository listRepository = mock(ReferenceListRepository.class);
    private final ReferenceValueRepository valueRepository = mock(ReferenceValueRepository.class);
    private final ReferenceLists lists = new ReferenceLists(listRepository, valueRepository, new SessionActor());

    private static ReferenceValue value(String list, String code, String label, boolean active, boolean requiresText) {
        return ReferenceValue.builder().listCode(list).code(code).label(label).active(active).requiresText(requiresText).usageCount(0L).build();
    }

    @Test
    void aStrictListRefusesAnUnknownOrInactiveValueAndCountsEachUse() {
        when(listRepository.findByCode("CANCEL_REASON")).thenReturn(Optional.of(
                ReferenceList.builder().code("CANCEL_REASON").label("Motifs d'annulation").strict(true).build()));
        ReferenceValue other = value("CANCEL_REASON", "OTHER", "Autre, préciser", true, true);
        ReferenceValue moved = value("CANCEL_REASON", "MOVED", "Rendez-vous déplacé", true, false);
        when(valueRepository.findByListCodeAndCode("CANCEL_REASON", "OTHER")).thenReturn(Optional.of(other));
        when(valueRepository.findByListCodeAndCode("CANCEL_REASON", "MOVED")).thenReturn(Optional.of(moved));
        when(valueRepository.findByListCodeAndCode("CANCEL_REASON", "OLD")).thenReturn(Optional.of(value("CANCEL_REASON", "OLD", "Ancien", false, false)));

        assertThatThrownBy(() -> lists.describe("cancel_reason", "inconnu", null)).isInstanceOf(BadRequestException.class)
                .hasMessage("Valeur inconnue « inconnu » dans la liste « Motifs d'annulation »");
        assertThatThrownBy(() -> lists.describe("CANCEL_REASON", "OLD", null)).hasMessageContaining("n'est plus proposé");
        assertThatThrownBy(() -> lists.describe("CANCEL_REASON", "OTHER", " ")).hasMessage("Précisez « Autre, préciser »");
        assertThat(lists.describe("CANCEL_REASON", "other", "client malade")).isEqualTo("Autre, préciser : client malade");
        assertThat(lists.describe("CANCEL_REASON", "MOVED", null)).isEqualTo("Rendez-vous déplacé");
        assertThat(other.getUsageCount()).isEqualTo(1L);
        assertThat(moved.getUsageCount()).isEqualTo(1L);
    }

    @Test
    void aSoftListAddsAnUnknownValueForTheAdministratorToReview() {
        when(listRepository.findByCode("ROOM_LAYOUT")).thenReturn(Optional.of(
                ReferenceList.builder().code("ROOM_LAYOUT").label("Dispositions").strict(false).build()));
        when(valueRepository.save(any(ReferenceValue.class))).thenAnswer(call -> call.getArgument(0));

        ReferenceValue added = lists.require("ROOM_LAYOUT", "En U");

        assertThat(added.getCode()).isEqualTo("EN_U");
        assertThat(added.getLabel()).isEqualTo("En U");
        assertThat(added.getAutoAdded()).isTrue();
    }

    @Test
    void aReasonAcceptsTheLegacyFreeText() {
        assertThat(lists.describeReason("CANCEL_REASON", null, null, "  ancien motif ")).isEqualTo("ancien motif");
        assertThat(lists.describeReason("CANCEL_REASON", " ", "précision seule", "ancien")).isEqualTo("précision seule");
        assertThat(lists.describeReason("CANCEL_REASON", null, null, null)).isNull();
    }

    @Test
    void replacingAListDeactivatesTheMissingValuesInsteadOfDeletingThem() {
        when(listRepository.findByCode("BANK")).thenReturn(Optional.of(ReferenceList.builder().code("BANK").label("Banques").build()));
        ReferenceValue kept = value("BANK", "BGFI", "BGFI", true, false);
        ReferenceValue dropped = value("BANK", "LCB", "LCB", true, false);
        when(valueRepository.findByListCodeOrderBySortOrderAscLabelAsc("BANK")).thenReturn(new ArrayList<>(List.of(kept, dropped)));

        lists.replace("BANK", List.of(new ValueRequest("bgfi", "BGFI Bank Congo", null, null, null), new ValueRequest("ubA", "UBA", null, null, null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReferenceValue>> saved = ArgumentCaptor.forClass(List.class);
        verify(valueRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ReferenceValue::getCode, ReferenceValue::getLabel, ReferenceValue::getActive)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("BGFI", "BGFI Bank Congo", true),
                        org.assertj.core.groups.Tuple.tuple("UBA", "UBA", true),
                        org.assertj.core.groups.Tuple.tuple("LCB", "LCB", false));
    }

    @Test
    void theEnumRegistryExposesAndControlsTheSameValues() {
        ReferenceEnumRegistry registry = new ReferenceEnumRegistry(List.of(new BookingReferenceEnums(),
                builder -> builder.add("DEMO", "COLOR", "Couleur", List.of(new EnumValue("RED", "Rouge", null, 10, true),
                        new EnumValue("OLD", "Ancienne", null, 20, false)))));

        assertThat(registry.groups("booking", "channel")).singleElement().satisfies(group -> {
            assertThat(group.label()).isEqualTo("Canal");
            assertThat(group.values()).filteredOn(EnumValue::selectable).extracting(EnumValue::code)
                    .containsExactly("PHONE", "DESK", "EMAIL", "WHATSAPP");
        });
        assertThat(registry.label("BOOKING", "STATUS", "CONFIRMED")).isEqualTo("Confirmée");
        assertThat(registry.require("demo", "color", " red ")).isEqualTo("RED");
        assertThatThrownBy(() -> registry.require("DEMO", "COLOR", "OLD")).isInstanceOf(BadRequestException.class)
                .hasMessage("Valeur inconnue « OLD » pour « Couleur » ; valeurs permises : RED");
        assertThatThrownBy(() -> new ReferenceEnumRegistry(List.of(new BookingReferenceEnums(), new BookingReferenceEnums())))
                .isInstanceOf(IllegalStateException.class);
    }
}
