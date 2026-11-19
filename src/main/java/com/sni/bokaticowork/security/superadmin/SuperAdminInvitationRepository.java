package com.sni.bokaticowork.security.superadmin;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SuperAdminInvitationRepository extends JpaRepository<SuperAdminInvitation, Long> {

    @Query("select i from SuperAdminInvitation i where lower(i.targetEmail) = lower(:email) and i.status <> 'USED'")
    List<SuperAdminInvitation> findOpenFor(@Param("email") String email);

    Optional<SuperAdminInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SuperAdminInvitation i where i.tokenHash = :hash")
    Optional<SuperAdminInvitation> lockByTokenHash(@Param("hash") String hash);
}
