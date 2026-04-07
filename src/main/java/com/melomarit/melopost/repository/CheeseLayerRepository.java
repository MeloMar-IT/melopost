package com.melo.melopost.repository;

import com.melo.melopost.model.CheeseLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheeseLayerRepository extends JpaRepository<CheeseLayer, Long> {
}
