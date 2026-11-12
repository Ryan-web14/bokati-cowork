package com.sni.bokaticowork.features.document.explorer.access;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentPermission;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentPermissionRepository;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFolderAccess;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFolderRef;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedGrant;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedGrantRequest;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Qui voit et que peut-on faire dans un dossier de la GED.
 *
 * <p>Un dossier non restreint suit les droits generaux ({@code DOCUMENT:READ}, {@code UPLOAD},
 * {@code WRITE}) : rien ne change pour lui. Un dossier restreint, et tout ce qu'il contient, n'est
 * accessible qu'aux personnes et roles qui y ont un droit. Quand plusieurs dossiers restreints se
 * suivent sur le chemin, il faut un droit sur chacun : le niveau retenu est le plus faible.</p>
 *
 * <p>Pour qui n'a aucun droit, le dossier n'existe pas : il disparait des listes, de l'arborescence
 * et de la recherche, et l'ouvrir rend 404 plutot que 403, pour ne pas reveler son existence. Un
 * super administrateur voit tout. Un traitement sans utilisateur (classement automatique, lecture
 * du texte) n'est pas concerne.</p>
 */
@Service
public class GedAccessService {

    private static final String FOLDER = "FOLDER";
    public static final String USER = "USER";
    public static final String ROLE = "ROLE";

    @PersistenceContext
    private EntityManager entityManager;

    private final DocumentFolderRepository folderRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository documentRepository;

    public GedAccessService(DocumentFolderRepository folderRepository, DocumentPermissionRepository permissionRepository,
                            com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository documentRepository) {
        this.folderRepository = folderRepository;
        this.permissionRepository = permissionRepository;
        this.documentRepository = documentRepository;
    }

    /** Un droit lu en base, avec l'identifiant du beneficiaire, qui ne sort pas d'ici. */
    private record GrantRow(String type, Long granteeId, GedGrant view) {}

    /** La personne qui agit : ses roles, ou un passe-droit (super administrateur, traitement systeme). */
    public record Viewer(Long userId, Set<Long> roleIds, boolean bypass) {}

    // Lecture du niveau ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Viewer viewer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return new Viewer(null, Set.of(), true);
        }
        boolean superAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
        Long userId = principal.getUser().getId();
        return new Viewer(userId, superAdmin ? Set.of() : roleIdsOf(userId), superAdmin);
    }

    /** Le niveau de la personne sur un dossier ; la racine et les dossiers non restreints : tout. */
    @Transactional(readOnly = true)
    public GedAccessLevel levelOn(DocumentFolder folder) {
        return levelOn(folder, viewer());
    }

    public GedAccessLevel levelOn(DocumentFolder folder, Viewer viewer) {
        return folder == null ? GedAccessLevel.ADMIN : levelOnPath(folder.getPath(), viewer);
    }

    /**
     * Le niveau sur un dossier designe par son id, y compris a la corbeille (les entites ne voient
     * plus un dossier supprime). Un dossier inconnu : tout, comme la racine.
     */
    @Transactional(readOnly = true)
    public GedAccessLevel levelOnFolderId(Long folderId) {
        if (folderId == null) {
            return GedAccessLevel.ADMIN;
        }
        @SuppressWarnings("unchecked")
        List<String> paths = entityManager.createNativeQuery("SELECT path FROM document_folder WHERE id = :id")
                .setParameter("id", folderId).getResultList();
        return paths.isEmpty() ? GedAccessLevel.ADMIN : levelOnPath(paths.getFirst(), viewer());
    }

    private GedAccessLevel levelOnPath(String path, Viewer viewer) {
        if (viewer.bypass()) {
            return GedAccessLevel.ADMIN;
        }
        List<Long> restricted = restrictedAmong(pathIds(path));
        if (restricted.isEmpty()) {
            return GedAccessLevel.ADMIN;
        }
        Map<Long, GedAccessLevel> best = bestGrants(restricted, viewer);
        GedAccessLevel level = GedAccessLevel.ADMIN;
        for (Long id : restricted) {
            GedAccessLevel granted = best.getOrDefault(id, GedAccessLevel.NONE);
            if (granted.ordinal() < level.ordinal()) {
                level = granted;
            }
        }
        return level;
    }

    /**
     * Exige un niveau sur un dossier. Sans meme le droit de voir : 404, le dossier n'existe pas
     * pour cette personne. Avec moins que le niveau demande : 403, en disant ce qui manque.
     */
    @Transactional(readOnly = true)
    public void require(DocumentFolder folder, GedAccessLevel needed) {
        GedAccessLevel level = levelOn(folder);
        if (!level.atLeast(GedAccessLevel.READ)) {
            throw new ResourceNotFoundException("Dossier introuvable : " + folder.getCode());
        }
        if (!level.atLeast(needed)) {
            throw new ForbiddenException("Ce dossier est restreint : il faut le droit « " + needed.label()
                    + " » (vous avez « " + level.label() + " »).");
        }
    }

    /** Comme {@link #require}, pour un fichier : c'est le dossier qui le contient qui compte. */
    @Transactional(readOnly = true)
    public void requireDocument(Document document, GedAccessLevel needed) {
        GedAccessLevel level = levelOn(document.getFolder());
        if (!level.atLeast(GedAccessLevel.READ)) {
            throw new ResourceNotFoundException("Fichier introuvable : " + document.getCode());
        }
        if (!level.atLeast(needed)) {
            throw new ForbiddenException("Ce fichier est dans un dossier restreint : il faut le droit « " + needed.label()
                    + " » (vous avez « " + level.label() + " »).");
        }
    }

    /**
     * Les dossiers restreints que la personne ne peut pas voir. Tout ce dont le chemin passe par
     * l'un d'eux lui est cache.
     */
    @Transactional(readOnly = true)
    public Set<Long> hiddenFolderIds() {
        Viewer viewer = viewer();
        if (viewer.bypass()) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<Number> restricted = entityManager.createNativeQuery("SELECT id FROM document_folder WHERE restricted")
                .getResultList();
        if (restricted.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = restricted.stream().map(Number::longValue).toList();
        Map<Long, GedAccessLevel> best = bestGrants(ids, viewer);
        Set<Long> hidden = new HashSet<>();
        for (Long id : ids) {
            if (!best.getOrDefault(id, GedAccessLevel.NONE).atLeast(GedAccessLevel.READ)) {
                hidden.add(id);
            }
        }
        return hidden;
    }

    /** Le dossier (ou un fichier de ce dossier) est-il visible, vu les dossiers caches ? */
    public static boolean visible(DocumentFolder folder, Set<Long> hidden) {
        if (folder == null || hidden.isEmpty()) {
            return true;
        }
        return pathIds(folder.getPath()).stream().noneMatch(hidden::contains);
    }

    // Gestion de l'acces --------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public GedFolderAccess access(DocumentFolder folder) {
        GedAccessLevel level = levelOn(folder);
        if (!level.atLeast(GedAccessLevel.READ)) {
            throw new ResourceNotFoundException("Dossier introuvable : " + folder.getCode());
        }
        List<Long> ancestors = new ArrayList<>(pathIds(folder.getPath()));
        ancestors.remove(folder.getId());
        List<GedFolderRef> inherited = restrictedAmong(ancestors).isEmpty() ? List.of()
                : folderRepository.findAllById(restrictedAmong(ancestors)).stream()
                .sorted((a, b) -> Integer.compare(depth(a), depth(b)))
                .map(f -> new GedFolderRef(f.getCode(), f.getName(), f.getParent() == null ? null : f.getParent().getCode()))
                .toList();
        boolean restricted = Boolean.TRUE.equals(folder.getRestricted());
        return new GedFolderAccess(folder.getCode(), restricted, folder.getRestrictedAt(), email(folder.getRestrictedBy()),
                folder.getSystemKey() != null, inherited, restricted ? grants(folder).stream().map(GrantRow::view).toList() : List.of(), level,
                level.atLeast(GedAccessLevel.ADMIN));
    }

    /**
     * Restreint un dossier. La personne qui le fait recoit le droit de gerer l'acces, pour ne pas
     * s'enfermer dehors ; les droits fournis s'ajoutent. Les dossiers du plan de classement ne se
     * restreignent pas : les modules qui y rangent (KYC, contrats) doivent pouvoir y lire.
     */
    @Transactional
    public GedFolderAccess restrict(DocumentFolder folder, List<GedGrantRequest> grants) {
        if (folder.getSystemKey() != null) {
            throw new BadRequestException("Un dossier du plan de classement ne se restreint pas : créez un sous-dossier "
                    + "à vous et restreignez-le.");
        }
        requireManage(folder);
        Viewer viewer = viewer();
        folder.setRestricted(true);
        folder.setRestrictedAt(Instant.now());
        folder.setRestrictedBy(viewer.userId());
        folderRepository.save(folder);
        if (viewer.userId() != null) {
            setGrant(folder, USER, viewer.userId(), GedAccessLevel.ADMIN, viewer.userId());
        }
        for (GedGrantRequest grant : grants == null ? List.<GedGrantRequest>of() : grants) {
            applyGrant(folder, grant, viewer.userId());
        }
        entityManager.flush();
        return access(folder);
    }

    /** Ouvre a nouveau le dossier a tous ceux qui ont les droits generaux ; les droits sont retires. */
    @Transactional
    public GedFolderAccess unrestrict(DocumentFolder folder) {
        if (!Boolean.TRUE.equals(folder.getRestricted())) {
            return access(folder);
        }
        requireManage(folder);
        folder.setRestricted(false);
        folder.setRestrictedAt(null);
        folder.setRestrictedBy(null);
        folderRepository.save(folder);
        entityManager.createNativeQuery("DELETE FROM document_permission WHERE target_type = 'FOLDER' AND target_id = :id")
                .setParameter("id", folder.getId()).executeUpdate();
        return access(folder);
    }

    /** Donne ou change le droit d'une personne ou d'un role : un seul niveau par beneficiaire. */
    @Transactional
    public GedFolderAccess grant(DocumentFolder folder, GedGrantRequest request) {
        requireRestricted(folder);
        requireManage(folder);
        applyGrant(folder, request, viewer().userId());
        entityManager.flush();
        return access(folder);
    }

    /** Retire le droit d'un beneficiaire. Le dernier droit de gestion ne se retire pas. */
    @Transactional
    public GedFolderAccess revoke(DocumentFolder folder, String granteeType, String grantee) {
        requireRestricted(folder);
        requireManage(folder);
        String type = type(granteeType);
        Long granteeId = resolveGrantee(type, grantee);
        List<GrantRow> current = grants(folder);
        boolean removesAdmin = current.stream()
                .anyMatch(g -> g.type().equals(type) && g.granteeId().equals(granteeId) && g.view().level() == GedAccessLevel.ADMIN);
        long admins = current.stream().filter(g -> g.view().level() == GedAccessLevel.ADMIN).count();
        if (removesAdmin && admins <= 1) {
            throw new ConflictException("ged-access", "c'est le dernier droit de gestion de ce dossier : donnez-le d'abord "
                    + "à quelqu'un d'autre, ou ouvrez le dossier à tous");
        }
        entityManager.createNativeQuery("""
                DELETE FROM document_permission
                WHERE target_type = 'FOLDER' AND target_id = :folder AND grantee_type = :type AND grantee_id = :grantee
                """).setParameter("folder", folder.getId()).setParameter("type", type).setParameter("grantee", granteeId)
                .executeUpdate();
        return access(folder);
    }

    // Aides --------------------------------------------------------------------------------------

    private void requireManage(DocumentFolder folder) {
        require(folder, GedAccessLevel.ADMIN);
    }

    private static void requireRestricted(DocumentFolder folder) {
        if (!Boolean.TRUE.equals(folder.getRestricted())) {
            throw new BadRequestException("Ce dossier n'est pas restreint : restreignez-le d'abord.");
        }
    }

    private void applyGrant(DocumentFolder folder, GedGrantRequest request, Long grantedBy) {
        if (request.level() == null || request.level() == GedAccessLevel.NONE) {
            throw new BadRequestException("Indiquez un niveau : READ, DOWNLOAD, EDIT, DELETE ou ADMIN.");
        }
        String type = type(request.granteeType());
        setGrant(folder, type, resolveGrantee(type, request.grantee()), request.level(), grantedBy);
    }

    private void setGrant(DocumentFolder folder, String type, Long granteeId, GedAccessLevel level, Long grantedBy) {
        entityManager.createNativeQuery("""
                DELETE FROM document_permission
                WHERE target_type = 'FOLDER' AND target_id = :folder AND grantee_type = :type AND grantee_id = :grantee
                """).setParameter("folder", folder.getId()).setParameter("type", type).setParameter("grantee", granteeId)
                .executeUpdate();
        permissionRepository.saveAndFlush(DocumentPermission.builder()
                .targetType(FOLDER).targetId(folder.getId())
                .granteeType(type).granteeId(granteeId)
                .permission(level.name()).grantedBy(grantedBy)
                .build());
    }

    private static String type(String granteeType) {
        String type = granteeType == null ? "" : granteeType.trim().toUpperCase(Locale.ROOT);
        if (!USER.equals(type) && !ROLE.equals(type)) {
            throw new BadRequestException("Le bénéficiaire est un utilisateur (USER, désigné par son e-mail) ou un rôle "
                    + "(ROLE, désigné par son nom).");
        }
        return type;
    }

    /** Un utilisateur se designe par son e-mail, un role par son nom (ADMIN, STAFF...). */
    private Long resolveGrantee(String type, String grantee) {
        if (grantee == null || grantee.isBlank()) {
            throw new BadRequestException("Indiquez le bénéficiaire.");
        }
        String sql = USER.equals(type)
                ? "SELECT id FROM users WHERE lower(email) = lower(:value)"
                : "SELECT id FROM role WHERE upper(name) = upper(:value) OR upper(name) = upper('ROLE_' || :value)";
        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery(sql).setParameter("value", grantee.trim()).getResultList();
        if (ids.isEmpty()) {
            throw new ResourceNotFoundException((USER.equals(type) ? "Utilisateur introuvable : " : "Rôle introuvable : ") + grantee);
        }
        return ids.getFirst().longValue();
    }

    @SuppressWarnings("unchecked")
    private List<GrantRow> grants(DocumentFolder folder) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT p.grantee_type, p.grantee_id, p.permission, p.granted_at,
                       CASE WHEN p.grantee_type = 'USER' THEN u.email ELSE r.name END,
                       CASE WHEN p.grantee_type = 'USER' THEN NULL ELSE r.display_name END,
                       g.email
                FROM document_permission p
                LEFT JOIN users u ON p.grantee_type = 'USER' AND u.id = p.grantee_id
                LEFT JOIN role r ON p.grantee_type = 'ROLE' AND r.id = p.grantee_id
                LEFT JOIN users g ON g.id = p.granted_by
                WHERE p.target_type = 'FOLDER' AND p.target_id = :folder
                ORDER BY p.grantee_type, 5
                """).setParameter("folder", folder.getId()).getResultList();
        List<GrantRow> grants = new ArrayList<>();
        for (Object[] row : rows) {
            grants.add(new GrantRow((String) row[0], ((Number) row[1]).longValue(), new GedGrant((String) row[0], (String) row[4],
                    (String) row[5], GedAccessLevel.of((String) row[2]), instant(row[3]), (String) row[6])));
        }
        return grants;
    }

    @SuppressWarnings("unchecked")
    private Set<Long> roleIdsOf(Long userId) {
        List<Number> ids = entityManager.createNativeQuery("SELECT role_id FROM role_user WHERE user_id = :user")
                .setParameter("user", userId).getResultList();
        Set<Long> roles = new HashSet<>();
        ids.forEach(id -> roles.add(id.longValue()));
        return roles;
    }

    @SuppressWarnings("unchecked")
    private List<Long> restrictedAmong(Collection<Long> folderIds) {
        if (folderIds.isEmpty()) {
            return List.of();
        }
        List<Number> ids = entityManager.createNativeQuery("SELECT id FROM document_folder WHERE restricted AND id IN (:ids)")
                .setParameter("ids", folderIds).getResultList();
        return ids.stream().map(Number::longValue).toList();
    }

    /** Pour chaque dossier, le meilleur droit de la personne : le sien, ou celui d'un de ses roles. */
    @SuppressWarnings("unchecked")
    private Map<Long, GedAccessLevel> bestGrants(Collection<Long> folderIds, Viewer viewer) {
        Map<Long, GedAccessLevel> best = new HashMap<>();
        if (folderIds.isEmpty() || viewer.userId() == null) {
            return best;
        }
        List<Long> roles = viewer.roleIds().isEmpty() ? List.of(-1L) : List.copyOf(viewer.roleIds());
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT target_id, permission FROM document_permission
                WHERE target_type = 'FOLDER' AND target_id IN (:folders)
                  AND ((grantee_type = 'USER' AND grantee_id = :user) OR (grantee_type = 'ROLE' AND grantee_id IN (:roles)))
                """).setParameter("folders", folderIds).setParameter("user", viewer.userId()).setParameter("roles", roles)
                .getResultList();
        for (Object[] row : rows) {
            Long folder = ((Number) row[0]).longValue();
            GedAccessLevel level = GedAccessLevel.of((String) row[1]);
            best.merge(folder, level, (a, b) -> a.ordinal() >= b.ordinal() ? a : b);
        }
        return best;
    }

    // Pour les anciennes routes, qui designent dossiers et documents par leur code -----------------

    /** Exige un niveau sur un document designe par son code ; un code inconnu est laisse a la route. */
    @Transactional(readOnly = true)
    public void requireDocumentCode(String code, GedAccessLevel needed) {
        if (code != null) {
            documentRepository.findByCode(code.trim()).ifPresent(document -> requireDocument(document, needed));
        }
    }

    /** Exige un niveau sur un dossier designe par son code ; un code inconnu est laisse a la route. */
    @Transactional(readOnly = true)
    public void requireFolderCode(String code, GedAccessLevel needed) {
        if (code != null && !code.isBlank()) {
            folderRepository.findByCode(code.trim()).ifPresent(folder -> require(folder, needed));
        }
    }

    /** Retire d'une page les documents ranges sous un dossier cache. */
    @Transactional(readOnly = true)
    public com.sni.bokaticowork.core.templateResponse.PaginatedResponse<com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse> withoutHiddenDocuments(
            com.sni.bokaticowork.core.templateResponse.PaginatedResponse<com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse> page) {
        Set<String> hidden = hiddenFolderCodes();
        if (!hidden.isEmpty() && page != null && page.getData() != null) {
            page.setData(page.getData().stream().filter(document -> !hidden.contains(document.getFolderCode())).toList());
        }
        return page;
    }

    /** Retire d'une liste les dossiers caches. */
    @Transactional(readOnly = true)
    public List<com.sni.bokaticowork.features.document.documentMaster.dto.response.FolderDetailResponse> withoutHiddenFolders(List<com.sni.bokaticowork.features.document.documentMaster.dto.response.FolderDetailResponse> folders) {
        Set<String> hidden = hiddenFolderCodes();
        return hidden.isEmpty() ? folders : folders.stream().filter(folder -> !hidden.contains(folder.getCode())).toList();
    }

    /** Elague d'une arborescence les dossiers caches. */
    @Transactional(readOnly = true)
    public com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode withoutHiddenBranches(com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode node) {
        Set<String> hidden = hiddenFolderCodes();
        if (!hidden.isEmpty()) {
            prune(node, hidden);
        }
        return node;
    }

    private static void prune(com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode node, Set<String> hidden) {
        if (node == null || node.getChildren() == null) {
            return;
        }
        node.setChildren(node.getChildren().stream().filter(child -> !hidden.contains(child.getCode())).toList());
        node.getChildren().forEach(child -> prune(child, hidden));
    }

    /** Un dossier restreint se trouve-t-il sur le chemin (le dossier lui-meme compris) ? */
    @Transactional(readOnly = true)
    public boolean isRestrictedPath(DocumentFolder folder) {
        return folder != null && !restrictedAmong(pathIds(folder.getPath())).isEmpty();
    }

    /** Les codes de tous les dossiers caches a la personne, sous-dossiers compris (anciens ecrans). */
    @Transactional(readOnly = true)
    public Set<String> hiddenFolderCodes() {
        Set<Long> hidden = hiddenFolderIds();
        if (hidden.isEmpty()) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<String> codes = entityManager.createNativeQuery("""
                SELECT DISTINCT f.code FROM document_folder f
                JOIN document_folder h ON f.path LIKE h.path || '%'
                WHERE h.id IN (:hidden)
                """).setParameter("hidden", hidden).getResultList();
        return new HashSet<>(codes);
    }

    public String email(Long userId) {
        if (userId == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<String> emails = entityManager.createNativeQuery("SELECT email FROM users WHERE id = :id")
                .setParameter("id", userId).getResultList();
        return emails.isEmpty() ? null : emails.getFirst();
    }

    /** « /100/105/112/ » vers [100, 105, 112]. */
    public static List<Long> pathIds(String path) {
        List<Long> ids = new ArrayList<>();
        if (path == null) {
            return ids;
        }
        for (String part : path.split("/")) {
            if (!part.isBlank()) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException ignored) {
                    // chemin ancien ou mal forme : la piece est sautee
                }
            }
        }
        return ids;
    }

    private static int depth(DocumentFolder folder) {
        return folder.getDepth() == null ? 0 : folder.getDepth();
    }

    private static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        return null;
    }
}
