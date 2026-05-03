package com.sni.bokaticowork.features.visitor.repository;

import com.sni.bokaticowork.features.visitor.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {
}
