package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.User;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {
    @AllowFiltering
    Optional<User> findByUsername(String username);
}
