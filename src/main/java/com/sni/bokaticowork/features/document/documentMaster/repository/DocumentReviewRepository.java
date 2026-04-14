package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {

    List<DocumentReview> findAllByDocumentOrderByReviewedAtDesc(Document document);
}
