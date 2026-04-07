package com.melo.melopost.repository;

import com.melo.melopost.model.PostmortemQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostmortemQuestionRepository extends JpaRepository<PostmortemQuestion, Long> {
    List<PostmortemQuestion> findByPostmortemId(Long postmortemId);
}
