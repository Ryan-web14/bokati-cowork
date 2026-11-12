package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassStatusHistoryRepository extends JpaRepository<PassStatusHistory, Long> {

    /** Le pass est lu avec l'historique : le controleur le rend tel quel, hors transaction. */
    @EntityGraph(attributePaths = "pass")
    List<PassStatusHistory> findAllByPassOrderByChangedAtDesc(Pass pass);
}
