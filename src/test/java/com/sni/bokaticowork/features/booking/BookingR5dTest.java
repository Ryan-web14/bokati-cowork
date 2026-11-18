package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingEndReportRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingOperations;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceIncident;
import com.sni.bokaticowork.features.ressource.model.ResourceTaskTemplate;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceIncidentRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTaskTemplateRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceOperations;
import com.sni.bokaticowork.features.task.dto.TaskDtos;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import com.sni.bokaticowork.features.task.repository.TaskItemRepository;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R5d : exploitation (taches, reglement, etat des lieux, incidents). */
class BookingR5dTest {

    private final Resource resource = Resource.builder().id(1L).code("SAL-1").name("Baobab").status(ResourceStatus.ACTIVE).build();
    private final LocalDateTime start = LocalDateTime.of(2026, 12, 2, 10, 0);
    private final Booking booking = Booking.builder().id(5L).bookingNumber("BKG-1").resource(resource)
            .status(BookingStatus.CONFIRMED).startedAt(start).endedAt(start.plusHours(2)).contactName("Awa").build();

    @Test
    void templatesCreateDatedTasksOnceAndCancelWithTheBooking() {
        ResourceTaskTemplateRepository templates = mock(ResourceTaskTemplateRepository.class);
        TaskManagementService tasks = mock(TaskManagementService.class);
        TaskItemRepository items = mock(TaskItemRepository.class);
        when(templates.forBooking(resource)).thenReturn(List.of(
                ResourceTaskTemplate.builder().title("Préparer la salle").moment(ResourceTaskTemplate.Moment.BEFORE_START)
                        .offsetMinutes(30).checklist("Chaises\nVidéoprojecteur").build(),
                ResourceTaskTemplate.builder().title("Remettre en état").moment(ResourceTaskTemplate.Moment.AFTER_END)
                        .offsetMinutes(15).build()));
        BookingOperations operations = new BookingOperations(mock(BookingRepository.class), mock(BookingEndReportRepository.class),
                templates, mock(BookingEventWriter.class));
        ReflectionTestUtils.setField(operations, "tasks", tasks);
        ReflectionTestUtils.setField(operations, "taskItems", items);

        assertThat(operations.hasTemplates(booking)).isTrue();
        assertThat(operations.createBookingTasks(booking)).isEqualTo(2);

        ArgumentCaptor<TaskDtos.CreateTaskRequest> created = ArgumentCaptor.forClass(TaskDtos.CreateTaskRequest.class);
        verify(tasks, times(2)).createFromAutomation(created.capture());
        ZoneId zone = ZoneId.of("Africa/Brazzaville");
        assertThat(created.getAllValues().get(0).dueAt()).isEqualTo(start.minusMinutes(30).atZone(zone).toInstant());
        assertThat(created.getAllValues().get(0).checklist()).hasSize(2);
        assertThat(created.getAllValues().get(1).dueAt()).isEqualTo(start.plusHours(2).plusMinutes(15).atZone(zone).toInstant());
        assertThat(created.getAllValues().get(0).sourceCode()).isEqualTo("BKG-1");

        when(items.existsBySourceTypeAndSourceCodeAndTitle(eq("BOOKING"), eq("BKG-1"), anyString())).thenReturn(true);
        assertThat(operations.createBookingTasks(booking)).isZero();

        TaskItem open = TaskItem.builder().status(TaskStatus.OPEN).build();
        TaskItem done = TaskItem.builder().status(TaskStatus.COMPLETED).build();
        when(items.findAllBySourceTypeAndSourceCode("BOOKING", "BKG-1")).thenReturn(List.of(open, done));
        assertThat(operations.cancelTasks(booking)).isEqualTo(1);
        assertThat(open.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(done.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void changedRulesMustBeAcceptedAgain() {
        BookingOperations operations = new BookingOperations(mock(BookingRepository.class), mock(BookingEndReportRepository.class),
                mock(ResourceTaskTemplateRepository.class), mock(BookingEventWriter.class));
        resource.setUsageRules("Pas de nourriture dans la salle.");

        assertThat(operations.usageRules(booking).upToDate()).isFalse();
        operations.acceptUsageRules(booking, "MBR-1");
        assertThat(operations.usageRules(booking).upToDate()).isTrue();

        resource.setUsageRules("Pas de nourriture ni de boisson dans la salle.");
        assertThat(operations.usageRules(booking).upToDate()).isFalse();
    }

    private ResourceOperations resourceOperations(ResourceIncidentRepository incidents, ResourceService service) {
        SequenceGeneratorFacade sequence = mock(SequenceGeneratorFacade.class);
        when(sequence.next(eq("resource_incident"), any(LocalDate.class))).thenReturn("INC-00001");
        ResourceRepository resources = mock(ResourceRepository.class);
        when(resources.findByCode("SAL-1")).thenReturn(Optional.of(resource));
        ResourceOperations operations = new ResourceOperations(mock(ResourceTaskTemplateRepository.class), incidents, resources,
                mock(ResourceClosureRepository.class), sequence);
        ReflectionTestUtils.setField(operations, "resourceService", service);
        return operations;
    }

    @Test
    void onlyTheTeamBlocksAResourceAndResolvingReopensIt() {
        ResourceIncidentRepository incidents = mock(ResourceIncidentRepository.class);
        when(incidents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ResourceService service = mock(ResourceService.class);
        ResourceOperations operations = resourceOperations(incidents, service);

        operations.report(new ResourceOperations.IncidentRequest("SAL-1", "BKG-1", "Fuite", null, "HIGH", true, false, null),
                new ResourceOperations.Reporter("MEMBER", "MBR-1", "Awa", null, null));
        verify(service, never()).updateStatus(anyString(), any(ResourceStatus.class), any(Boolean.class));

        ResourceOperations.IncidentResponse blocked = operations.report(
                new ResourceOperations.IncidentRequest("SAL-1", null, "Climatisation en panne", null, "CRITICAL", true, false, null),
                new ResourceOperations.Reporter("TEAM", "admin", "admin", null, null));
        verify(service).updateStatus("SAL-1", ResourceStatus.MAINTENANCE, false);
        assertThat(blocked.blocked()).isTrue();

        resource.setStatus(ResourceStatus.MAINTENANCE);
        ResourceIncident stored = ResourceIncident.builder().code(blocked.code()).resource(resource).title("Climatisation en panne")
                .severity("CRITICAL").status("OPEN").blocked(true).reportedByType("TEAM").build();
        when(incidents.findByCodeIgnoreCase(blocked.code())).thenReturn(Optional.of(stored));
        when(incidents.countByResourceAndBlockedTrueAndStatusNot(resource, "RESOLVED")).thenReturn(0L);

        operations.resolve(blocked.code(), "Compresseur changé", true, "admin");

        verify(service).updateStatus("SAL-1", ResourceStatus.ACTIVE);
        assertThat(stored.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void aDamagedReturnOpensAnIncident() {
        BookingEndReportRepository reports = mock(BookingEndReportRepository.class);
        when(reports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ResourceIncidentRepository incidents = mock(ResourceIncidentRepository.class);
        when(incidents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BookingOperations operations = new BookingOperations(mock(BookingRepository.class), reports,
                mock(ResourceTaskTemplateRepository.class), mock(BookingEventWriter.class));
        ReflectionTestUtils.setField(operations, "operations", resourceOperations(incidents, mock(ResourceService.class)));

        BookingOperations.EndReportResponse report = operations.endReport(booking, new BookingOperations.EndReportRequest("DAMAGED",
                        List.of(new BookingOperations.ChecklistAnswer("Tableau effacé", true)), List.of("DOC-1"), "Chaise cassée"),
                "MEMBER", "MBR-1", "Awa", null, null);

        assertThat(report.incidentCode()).startsWith("INC");
        assertThat(report.checklist()).singleElement().satisfies(answer -> assertThat(answer.done()).isTrue());
        assertThat(report.photoDocumentCodes()).containsExactly("DOC-1");
    }
}
