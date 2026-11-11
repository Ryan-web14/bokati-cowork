package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel;
import com.sni.bokaticowork.features.document.explorer.access.GedAccessService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFolderAccess;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedGrantRequest;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les dossiers restreints sur un PostgreSQL reel : droits par personne et par role, droits
 * cumules le long du chemin, dossiers caches, gestion de l'acces et ses garde-fous.
 *
 * <p>Arborescence : {@code Direction} (restreint) contient {@code Comptes} (ouvert) et
 * {@code Paie} (restreint) ; {@code Public} est ouvert. Alice gere Direction ; le role
 * {@code GED_RH} peut voir Direction et ouvrir Paie ; Bob a ce role ; Carol n'a rien.</p>
 */
@Import(GedAccessService.class)
class GedAccessIT extends PostgresIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(994_000_000L);

    @Autowired private EntityManager entityManager;
    @Autowired private GedAccessService access;
    @Autowired private DocumentFolderRepository folders;

    private long alice;
    private long bob;
    private long carol;
    private DocumentFolder direction;
    private DocumentFolder comptes;
    private DocumentFolder paie;
    private DocumentFolder publicFolder;

    @BeforeEach
    void seed() {
        alice = user("alice.ged@test.cg");
        bob = user("bob.ged@test.cg");
        carol = user("carol.ged@test.cg");
        long role = IDS.incrementAndGet();
        entityManager.createNativeQuery("INSERT INTO role (id, name, is_system_role, is_active) VALUES (:id, 'GED_RH', FALSE, TRUE)")
                .setParameter("id", role).executeUpdate();
        entityManager.createNativeQuery("INSERT INTO role_user (user_id, role_id, assigned_by, assigned_at) VALUES (:u, :r, 'test', now())")
                .setParameter("u", bob).setParameter("r", role).executeUpdate();

        long directionId = folder(null, "Direction", true, null);
        long comptesId = folder(directionId, "Comptes", false, null);
        long paieId = folder(directionId, "Paie", true, null);
        long publicId = folder(null, "Public", false, null);
        grant(directionId, "USER", alice, "ADMIN");
        grant(directionId, "ROLE", role, "READ");
        grant(paieId, "ROLE", role, "DOWNLOAD");
        entityManager.flush();
        entityManager.clear();
        direction = folders.findById(directionId).orElseThrow();
        comptes = folders.findById(comptesId).orElseThrow();
        paie = folders.findById(paieId).orElseThrow();
        publicFolder = folders.findById(publicId).orElseThrow();
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sans droit, un dossier restreint et tout son contenu n'existent pas ; un dossier ouvert reste a tous")
    void strangersSeeNothing() {
        as(carol);
        assertThat(access.levelOn(direction)).isEqualTo(GedAccessLevel.NONE);
        assertThat(access.levelOn(comptes)).isEqualTo(GedAccessLevel.NONE);
        assertThat(access.levelOn(publicFolder)).isEqualTo(GedAccessLevel.ADMIN);
        Set<Long> hidden = access.hiddenFolderIds();
        assertThat(hidden).contains(direction.getId(), paie.getId());
        assertThat(GedAccessService.visible(comptes, hidden)).isFalse();
        assertThat(GedAccessService.visible(publicFolder, hidden)).isTrue();
        assertThatThrownBy(() -> access.require(comptes, GedAccessLevel.READ)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un droit de role vaut pour ses membres, et le plus faible droit du chemin l'emporte")
    void roleGrantsAndWeakestLink() {
        as(bob);
        assertThat(access.levelOn(direction)).isEqualTo(GedAccessLevel.READ);
        assertThat(access.levelOn(comptes)).isEqualTo(GedAccessLevel.READ);
        // DOWNLOAD sur Paie, mais seulement READ sur Direction au-dessus
        assertThat(access.levelOn(paie)).isEqualTo(GedAccessLevel.READ);
        assertThatThrownBy(() -> access.require(comptes, GedAccessLevel.EDIT))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("modifier");

        // Alice gere Direction mais n'a rien sur Paie : il faut un droit sur chaque dossier restreint
        as(alice);
        assertThat(access.levelOn(direction)).isEqualTo(GedAccessLevel.ADMIN);
        assertThat(access.levelOn(paie)).isEqualTo(GedAccessLevel.NONE);
        assertThat(access.hiddenFolderIds()).containsExactly(paie.getId());
    }

    @Test
    @DisplayName("Un super administrateur voit tout ; un traitement sans utilisateur n'est pas concerne")
    void bypasses() {
        as(carol, "ROLE_SUPER_ADMIN");
        assertThat(access.levelOn(paie)).isEqualTo(GedAccessLevel.ADMIN);
        assertThat(access.hiddenFolderIds()).isEmpty();
        SecurityContextHolder.clearContext();
        assertThat(access.levelOn(paie)).isEqualTo(GedAccessLevel.ADMIN);
    }

    @Test
    @DisplayName("Restreindre donne la gestion a son auteur ; le dernier gestionnaire ne se retire pas ; rouvrir efface les droits")
    void manageAccess() {
        as(alice);
        GedFolderAccess restricted = access.restrict(comptes, List.of(new GedGrantRequest("USER", "carol.ged@test.cg", GedAccessLevel.EDIT)));
        assertThat(restricted.restricted()).isTrue();
        assertThat(restricted.inheritedFrom()).extracting(ref -> ref.name()).containsExactly("Direction");
        assertThat(restricted.grants()).extracting(grant -> grant.grantee() + "=" + grant.level())
                .containsExactlyInAnyOrder("alice.ged@test.cg=ADMIN", "carol.ged@test.cg=EDIT");

        // Carol a EDIT sur Comptes mais rien sur Direction : elle ne voit toujours rien
        as(carol);
        assertThat(access.levelOn(comptes)).isEqualTo(GedAccessLevel.NONE);

        as(alice);
        GedFolderAccess withRole = access.grant(comptes, new GedGrantRequest("ROLE", "GED_RH", GedAccessLevel.DELETE));
        assertThat(withRole.grants()).extracting(grant -> grant.grantee()).contains("GED_RH");
        assertThatThrownBy(() -> access.revoke(comptes, "USER", "alice.ged@test.cg"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("dernier");
        assertThat(access.revoke(comptes, "USER", "carol.ged@test.cg").grants())
                .extracting(grant -> grant.grantee()).doesNotContain("carol.ged@test.cg");

        // Bob (role GED_RH) n'a que READ sur Direction : il ne gere pas Comptes
        as(bob);
        assertThatThrownBy(() -> access.grant(comptes, new GedGrantRequest("USER", "bob.ged@test.cg", GedAccessLevel.ADMIN)))
                .isInstanceOf(ForbiddenException.class);

        as(alice);
        GedFolderAccess reopened = access.unrestrict(comptes);
        assertThat(reopened.restricted()).isFalse();
        assertThat(reopened.grants()).isEmpty();
    }

    @Test
    @DisplayName("Un dossier du plan ne se restreint pas, un beneficiaire inconnu est refuse")
    void guards() {
        as(alice);
        long systemId = folder(null, "Membres", false, "root:MEMBER");
        entityManager.flush();
        DocumentFolder system = folders.findById(systemId).orElseThrow();
        assertThatThrownBy(() -> access.restrict(system, List.of())).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("plan de classement");
        assertThatThrownBy(() -> access.grant(direction, new GedGrantRequest("USER", "personne@test.cg", GedAccessLevel.READ)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> access.grant(direction, new GedGrantRequest("GROUPE", "x", GedAccessLevel.READ)))
                .isInstanceOf(BadRequestException.class);
    }

    // Donnees ------------------------------------------------------------------------------------

    private long user(String email) {
        long id = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO users (id, user_id, email, password_hash, created_at, deleted)
                VALUES (:id, :userId, :email, 'x', now(), FALSE)
                """).setParameter("id", id).setParameter("userId", "USR-" + id).setParameter("email", email).executeUpdate();
        return id;
    }

    private long folder(Long parentId, String name, boolean restricted, String systemKey) {
        long id = IDS.incrementAndGet();
        String parentPath = parentId == null ? "/" : (String) entityManager
                .createNativeQuery("SELECT path FROM document_folder WHERE id = :id").setParameter("id", parentId).getSingleResult();
        entityManager.createNativeQuery("""
                INSERT INTO document_folder (id, code, name, parent_id, space, path, depth, created_at, updated_at, deleted,
                                             restricted, system_key)
                VALUES (:id, :code, :name, :parent, 'GENERIC', :path, :depth, now(), now(), FALSE, :restricted, :systemKey)
                """).setParameter("id", id).setParameter("code", "FLD-ACC-" + id).setParameter("name", name)
                .setParameter("parent", parentId).setParameter("path", parentPath + id + "/")
                .setParameter("depth", parentId == null ? 0 : 1).setParameter("restricted", restricted)
                .setParameter("systemKey", systemKey).executeUpdate();
        return id;
    }

    private void grant(long folderId, String type, long granteeId, String level) {
        entityManager.createNativeQuery("""
                INSERT INTO document_permission (id, target_type, target_id, grantee_type, grantee_id, permission, granted_at)
                VALUES (:id, 'FOLDER', :folder, :type, :grantee, :level, now())
                """).setParameter("id", IDS.incrementAndGet()).setParameter("folder", folderId).setParameter("type", type)
                .setParameter("grantee", granteeId).setParameter("level", level).executeUpdate();
    }

    private static void as(long userId, String... authorities) {
        Users user = new Users();
        user.setId(userId);
        UserPrincipal principal = new UserPrincipal();
        ReflectionTestUtils.setField(principal, "user", user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
