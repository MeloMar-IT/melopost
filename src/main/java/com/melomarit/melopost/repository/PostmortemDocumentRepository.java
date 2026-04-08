package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.PostmortemDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostmortemDocumentRepository extends JpaRepository<PostmortemDocument, Long> {
    List<PostmortemDocument> findByPostmortemId(Long postmortemId);
}
