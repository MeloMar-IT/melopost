package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.CheeseLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheeseLayerRepository extends JpaRepository<CheeseLayer, Long> {
}
