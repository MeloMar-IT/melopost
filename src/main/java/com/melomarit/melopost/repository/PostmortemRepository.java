package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.Postmortem;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostmortemRepository extends CassandraRepository<Postmortem, UUID> {

    @AllowFiltering
    List<Postmortem> findByType(String type);

    // Simplified search as Cassandra doesn't support complex JOINs or multiple LIKEs across columns easily
    // In a real scenario, we would use an search index or a more specialized repository
    @AllowFiltering
    List<Postmortem> findByTitleContaining(String keyword);
}
