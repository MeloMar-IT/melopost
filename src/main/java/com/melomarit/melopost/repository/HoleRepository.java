package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.Hole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HoleRepository extends JpaRepository<Hole, Long> {
    Optional<Hole> findByStoryId(Long storyId);
}
