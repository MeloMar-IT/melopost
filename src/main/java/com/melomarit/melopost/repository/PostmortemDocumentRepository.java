package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.PostmortemDocument;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostmortemDocumentRepository extends CassandraRepository<PostmortemDocument, UUID> {
    @Query("SELECT * FROM postmortem_document WHERE postmortem_uuid = ?0")
    List<PostmortemDocument> findByPostmortemUuid(UUID postmortemUuid);
}
