package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.DataSource;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DataSourceRepository extends CassandraRepository<DataSource, UUID> {
}
