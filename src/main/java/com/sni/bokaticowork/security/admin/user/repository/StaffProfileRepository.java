package com.sni.bokaticowork.security.admin.user.repository;

import com.sni.bokaticowork.security.admin.user.model.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByUserId(Long userId);

    List<StaffProfile> findAllByManagerUserId(Long managerUserId);
}
