package com.sni.bokaticowork.security.admin.role.repository;

import com.sni.bokaticowork.security.admin.role.embedded.RoleUserId;
import com.sni.bokaticowork.security.admin.role.model.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface  RoleUserRepository extends JpaRepository<RoleUser, RoleUserId> {


    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM role_user WHERE user_id = :userId AND role_id = :roleId")
    void deleteRoleFromUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM role_user WHERE role_id = :roleId")
    void deleteAllUserFromRole(@Param("roleId") Long roleId);

    @Query(nativeQuery = true, value = "SELECT * FROM role_user WHERE user_id = :userId")
    List<RoleUser> findByUserId(@Param("userId") Long userId);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM role_user WHERE user_id = :userId AND role_id = :roleId)")
    boolean existsByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Query(nativeQuery = true, value = """
            SELECT EXISTS(
                SELECT 1
                FROM role_user ru
                JOIN role r ON r.id = ru.role_id
                WHERE r.name = :roleName
            )
            """)
    boolean existsAnyUserAssignedToRole(@Param("roleName") String roleName);

    /** Les roles actifs d'un utilisateur · ce que ses autorites refletent. */
    @Query(nativeQuery = true, value = """
            SELECT r.name
            FROM role_user ru
            JOIN role r ON r.id = ru.role_id
            WHERE ru.user_id = :userId
              AND r.is_active = true
            """)
    List<String> findActiveRoleNamesByUserId(@Param("userId") Long userId);

    /** Combien d'autres comptes actifs portent ce role · protege le dernier super-administrateur. */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(DISTINCT u.id)
            FROM role_user ru
            JOIN role r ON r.id = ru.role_id
            JOIN users u ON u.id = ru.user_id
            WHERE r.name = :roleName
              AND r.is_active = true
              AND u.id <> :excludedUserId
              AND u.deleted = false
              AND u.is_account_enabled = true
            """)
    long countOtherEnabledHolders(@Param("roleName") String roleName, @Param("excludedUserId") Long excludedUserId);

    /** Les adresses des comptes actifs qui portent ce role · destinataires des alertes de droits. */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT u.email
            FROM role_user ru
            JOIN role r ON r.id = ru.role_id
            JOIN users u ON u.id = ru.user_id
            WHERE r.name = :roleName
              AND r.is_active = true
              AND u.deleted = false
              AND u.is_account_enabled = true
            """)
    List<String> findEnabledHolderEmails(@Param("roleName") String roleName);

    /** Les permissions actives d'un role · ce qu'il donne reellement a qui le porte. */
    @Query(nativeQuery = true, value = """
            SELECT p.name
            FROM role_permission rp
            JOIN permission p ON p.id = rp.permission_id
            WHERE rp.role_id = :roleId
              AND rp.is_active = true
              AND p.is_active = true
            """)
    List<String> findActivePermissionNamesByRoleId(@Param("roleId") Long roleId);

}
