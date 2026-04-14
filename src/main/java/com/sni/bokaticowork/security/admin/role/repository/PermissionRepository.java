package com.sni.bokaticowork.security.admin.role.repository;

import com.sni.bokaticowork.security.admin.role.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM permission WHERE lower(name) = lower(:name))")
    boolean existsByName(String name);

    @Query(nativeQuery = true, value = "SELECT * FROM permission WHERE is_active = true")
    Optional<List<Permission>> findAllActivePermission();

    @Query(nativeQuery = true, value = "SELECT * FROM permission WHERE is_system_permission  = true")
    Optional<List<Permission>> findSystemPermissions();
}