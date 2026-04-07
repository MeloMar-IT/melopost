package com.melo.melopost.repository;

import com.melo.melopost.model.Hole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HoleRepository extends JpaRepository<Hole, Long> {
    Optional<Hole> findByStoryId(Long storyId);
}
