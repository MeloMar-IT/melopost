package com.melo.melopost.repository;

import com.melo.melopost.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByStoryNumber(String storyNumber);
    List<Story> findByWhatToFixContainingIgnoreCase(String whatToFix);
}
