package com.sni.bokaticowork.security.authentication.repository;

import com.sni.bokaticowork.security.authentication.model.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    OneTimeToken findByToken(String token);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM one_time_token WHERE token = :token AND expired_at > CURRENT_TIMESTAMP)")
    boolean isValidToken(String token);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM one_time_token WHERE token = :token)")
    boolean existsByToken(String token);

    void deleteByToken(String token);

    /**
     * Les codes, les plus recents d'abord, filtres sur le titulaire et l'usage.
     *
     * <p>Requete native plutot que JPQL : un parametre facultatif se teste proprement avec
     * {@code IS NULL} en SQL, la ou le typage d'un {@code Boolean} nul en JPQL n'est pas portable.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT t.*
            FROM one_time_token t
            JOIN users u ON u.id = t.user_id
            WHERE (CAST(:email AS VARCHAR) IS NULL OR LOWER(u.email) = CAST(:email AS VARCHAR))
              AND (CAST(:used AS BOOLEAN) IS NULL OR t.is_used = CAST(:used AS BOOLEAN))
            ORDER BY t.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM one_time_token t
            JOIN users u ON u.id = t.user_id
            WHERE (CAST(:email AS VARCHAR) IS NULL OR LOWER(u.email) = CAST(:email AS VARCHAR))
              AND (CAST(:used AS BOOLEAN) IS NULL OR t.is_used = CAST(:used AS BOOLEAN))
            """)
    Page<OneTimeToken> search(@Param("email") String email, @Param("used") Boolean used, Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE one_time_token SET is_used = true WHERE user_id = :userId")
    void invalidateAllTokensForUser(Long userId);
}
