package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
}
