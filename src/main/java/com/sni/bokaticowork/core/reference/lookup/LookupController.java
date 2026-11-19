package com.sni.bokaticowork.core.reference.lookup;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recherches de choix transverses (lot L0), ouvertes a toute l'equipe : un formulaire qui assigne
 * une tache ou designe un responsable n'a pas a demander {@code SYSTEM:USERS}.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/lookup")
@RequiredArgsConstructor
public class LookupController {

    static final int MAX_SIZE = 50;

    private final UserRepository users;

    /** Utilisateurs de l'equipe : {@code code} = courriel, {@code label} = nom, {@code hint} = courriel. */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<PaginatedResponse<LookupItem>> users(@RequestParam(required = false) String q,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "false") boolean includeInactive) {
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), Sort.by("email"));
        Page<LookupItem> result;
        if (StringUtils.hasText(q)) {
            List<LookupItem> found = users.searchAdminUsers(q.trim()).stream().map(LookupController::item)
                    .filter(item -> includeInactive || item.active()).toList();
            int from = (int) Math.min(request.getOffset(), found.size());
            int to = Math.min(from + request.getPageSize(), found.size());
            result = new PageImpl<>(found.subList(from, to), request, found.size());
        } else {
            result = users.findAllByDeletedFalse(request).map(LookupController::item);
        }
        return ResponseEntity.ok(new PaginatedResponse<>(result));
    }

    static LookupItem item(Users user) {
        String name = ((user.getFirstname() == null ? "" : user.getFirstname()) + " " + (user.getLastname() == null ? "" : user.getLastname())).trim();
        boolean active = !user.isDeleted() && !Boolean.FALSE.equals(user.getIsAccountEnabled()) && !Boolean.TRUE.equals(user.getIsAccountLocked());
        return new LookupItem("USER", user.getEmail(), name.isEmpty() ? user.getEmail() : name, user.getEmail(), active);
    }
}
