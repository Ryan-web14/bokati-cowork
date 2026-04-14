package com.sni.bokaticowork.security.admin.role.service.interfaces;


import com.sni.bokaticowork.security.admin.role.dto.response.RoleResponse;

import java.util.List;

public interface RoleUserService {

    void addRoleToUser(Long userId, Long roleId);
    void addRoleToUser(Long userId, String roleName, String assignedBy);
    void deleteRoleFromUser(Long userId, Long roleId);
    void deleteRoleFromAllUser(Long roleId);
    List<RoleResponse> getRoleUsers(Long id);
    boolean hasAnyUserAssignedToRole(String roleName);

}
