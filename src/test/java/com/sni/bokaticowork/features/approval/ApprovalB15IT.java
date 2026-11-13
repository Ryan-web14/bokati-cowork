package com.sni.bokaticowork.features.approval;

import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.enums.ApprovalStepStatus;
import com.sni.bokaticowork.features.approval.enums.ApproverRule;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.approval.service.ApprovalHierarchy;
import com.sni.bokaticowork.features.approval.service.ApprovalNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.security.admin.user.model.StaffProfile;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.StaffProfileRepository;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot B15 sur le schema de production : une demande a deux niveaux (N+1 puis N+2) approuvee
 * d'abord par le N+2 passe a « approuvee », le niveau 1 est note « approuve en avance », et la
 * demande figure dans l'historique des deux approbateurs.
 */
@Import({ApprovalEngine.class, ApprovalHierarchy.class, CashActorResolver.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ApprovalB15IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Autowired private ApprovalEngine engine;
    @Autowired private UserRepository users;
    @Autowired private StaffProfileRepository profiles;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private ApprovalNotifier notifier;

    private String code;
    private String agent;
    private String chef;
    private String directeur;
    private String type;

    @BeforeEach
    void setUp() {
        code = UUID.randomUUID().toString().substring(0, 6).toLowerCase();
        when(sequences.next(anyString())).thenAnswer(invocation -> "APR-" + code + "-" + SEQ.getAndIncrement());
        Users dg = user("directeur");
        Users cs = user("chef");
        Users ag = user("agent");
        profiles.save(StaffProfile.builder().userId(cs.getId()).jobTitle("Chef de service").managerUserId(dg.getId()).build());
        profiles.save(StaffProfile.builder().userId(ag.getId()).jobTitle("Commerciale").managerUserId(cs.getId()).build());
        agent = ag.getEmail();
        chef = cs.getEmail();
        directeur = dg.getEmail();
        type = "TEST_" + code.toUpperCase();
        as("admin-" + code + "@elleaose.com", "APPROVAL_MANAGE");
        engine.saveCircuit(new ApprovalDtos.CircuitInput("C_" + type, "BILLING", type, "Circuit de test", true, List.of(
                new ApprovalDtos.LevelSpec("Responsable (N+1)", ApproverRule.MANAGER, 1, null, null, null),
                new ApprovalDtos.LevelSpec("Direction (N+2)", ApproverRule.MANAGER, 2, null, null, new BigDecimal("500000")))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("N+2 approuve avant le N+1 : demande approuvee, niveau 1 approuve en avance, historique des deux")
    void higherLevelApprovesEarly() {
        ApprovalRequest request = submit(new BigDecimal("600000"));
        assertThat(request.getRequiredLevels()).isEqualTo(2);

        as(agent);
        assertThatThrownBy(() -> engine.approve(request.getNumber(), null)).isInstanceOf(ForbiddenException.class);

        as(chef);
        assertThat(engine.toApprove("BILLING")).extracting(ApprovalDtos.RequestView::number).contains(request.getNumber());
        as(directeur);
        assertThat(engine.toApprove("BILLING")).extracting(ApprovalDtos.RequestView::number).contains(request.getNumber());

        ApprovalDtos.Outcome outcome = engine.approve(request.getNumber(), new ApprovalDtos.Decision("Urgent, je valide", null));

        assertThat(outcome.finalApproval()).isTrue();
        assertThat(outcome.fromLevel()).isEqualTo(1);
        assertThat(outcome.toLevel()).isEqualTo(2);
        ApprovalDtos.RequestView view = engine.get(request.getNumber());
        assertThat(view.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(view.steps().get(0).status()).isEqualTo(ApprovalStepStatus.APPROVED_BY_HIGHER);
        assertThat(view.steps().get(0).comment()).contains("Approuvé en avance par Prenom directeur");
        assertThat(view.steps().get(1).status()).isEqualTo(ApprovalStepStatus.APPROVED);
        assertThat(view.timeline()).extracting(ApprovalDtos.EventView::event).extracting(Enum::name)
                .containsExactly("SUBMITTED", "APPROVED_BY_HIGHER", "APPROVED");

        assertThat(engine.history("BILLING", type, null, null, null, null, null, 0, 20).getContent())
                .extracting(ApprovalDtos.RequestView::number).containsExactly(request.getNumber());
        as(chef);
        assertThat(engine.history("BILLING", type, null, null, null, null, null, 0, 20).getContent())
                .extracting(ApprovalDtos.RequestView::number).containsExactly(request.getNumber());
        assertThat(engine.toApprove("BILLING")).extracting(ApprovalDtos.RequestView::number).doesNotContain(request.getNumber());
    }

    @Test
    @DisplayName("Sous le seuil, un seul niveau ; le N+1 approuve, un tiers sans lien ne peut pas")
    void belowThresholdOneLevel() {
        ApprovalRequest request = submit(new BigDecimal("100000"));
        assertThat(request.getRequiredLevels()).isEqualTo(1);

        as("tiers-" + code + "@elleaose.com");
        assertThatThrownBy(() -> engine.approve(request.getNumber(), null)).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Prenom chef");

        as(chef);
        assertThat(engine.approve(request.getNumber(), null).finalApproval()).isTrue();
    }

    @Test
    @DisplayName("Delegation datee : le delegue du N+1 approuve a sa place, la frise le dit")
    void delegateApproves() {
        String adjoint = "adjoint-" + code + "@elleaose.com";
        as(chef);
        engine.delegate(new ApprovalDtos.DelegationInput(adjoint, null, null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), "Conge"));
        ApprovalRequest request = submit(new BigDecimal("100000"));

        as(adjoint);
        ApprovalDtos.Outcome outcome = engine.approve(request.getNumber(), null);

        assertThat(outcome.finalApproval()).isTrue();
        assertThat(outcome.delegatedFrom()).isEqualTo(chef);
    }

    @Test
    @DisplayName("Refus par le N+1 : demande refusee, motif obligatoire")
    void managerRejects() {
        ApprovalRequest request = submit(new BigDecimal("600000"));
        as(chef);
        assertThatThrownBy(() -> engine.reject(request.getNumber(), new ApprovalDtos.Decision(" ", null)))
                .hasMessageContaining("motif");

        engine.reject(request.getNumber(), new ApprovalDtos.Decision("Remise trop forte", null));

        as(agent);
        assertThat(engine.mine("BILLING")).filteredOn(view -> view.number().equals(request.getNumber()))
                .extracting(ApprovalDtos.RequestView::status).containsExactly(ApprovalStatus.REJECTED);
    }

    private ApprovalRequest submit(BigDecimal amount) {
        as(agent);
        return engine.submit(new ApprovalDtos.Submission("BILLING", type, "BILLING_DOCUMENT", "INV-" + code + "-" + SEQ.getAndIncrement(),
                "Remise exceptionnelle", amount, "XAF", null, null, null, null));
    }

    private Users user(String role) {
        return users.save(Users.builder().userId("USR-" + role + "-" + code).email(role + "-" + code + "@elleaose.com")
                .firstname("Prenom " + role).lastname("Nom").passwordHash("x").createdAt(LocalDateTime.now())
                .isAccountEnabled(true).isAccountLocked(false).isAccountExpired(false).failedLoginAttempts(0).build());
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
