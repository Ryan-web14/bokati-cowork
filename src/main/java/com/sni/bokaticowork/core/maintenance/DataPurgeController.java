package com.sni.bokaticowork.core.maintenance;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/maintenance")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class DataPurgeController {

    private final DataPurgeService dataPurgeService;

    @DeleteMapping("/purge-data")
    @Audited(module = "MAINTENANCE", action = "PURGE_ALL_DATA", ressource = "system")
    public ResponseEntity<Map<String, Object>> purgeAllData(
            @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "This will permanently delete ALL non-admin data. Pass ?confirm=true to proceed.",
                    "warning", "This action is irreversible."
            ));
        }
        DataPurgeService.DataPurgeResult result = dataPurgeService.purgeAllNonAdminData();
        return ResponseEntity.ok(Map.of(
                "message", "Data purge completed successfully",
                "tablesTruncated", result.tablesTruncated(),
                "nonAdminUsersDeleted", result.nonAdminUsersDeleted(),
                "nonAdminRolesDeleted", result.nonAdminRolesDeleted(),
                "adminUsersPreserved", result.adminUsersPreserved(),
                "durationMs", result.durationMs()
        ));
    }
}
