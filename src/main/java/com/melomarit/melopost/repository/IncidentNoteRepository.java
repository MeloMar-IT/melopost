package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.IncidentNote;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentNoteRepository extends CassandraRepository<IncidentNote, UUID> {
    
    @AllowFiltering
    List<IncidentNote> findByIncidentRef(String incidentRef);
    
    @AllowFiltering
    List<IncidentNote> findByIncidentRefContaining(String keyword);
}
