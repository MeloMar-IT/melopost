package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.Postmortem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostmortemRepository extends JpaRepository<Postmortem, Long> {

    @EntityGraph(attributePaths = {"layers", "layers.holes", "layers.holes.story", "tags", "layers.holes.tags", "layers.holes.story.tags", "localPostmortems"})
    Optional<Postmortem> findById(Long id);

    List<Postmortem> findByType(String type);

    List<Postmortem> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT p FROM Postmortem p " +
           "LEFT JOIN p.layers l " +
           "LEFT JOIN l.holes h " +
           "LEFT JOIN h.story s " +
           "LEFT JOIN p.tags pt " +
           "LEFT JOIN h.tags ht " +
           "LEFT JOIN s.tags st " +
           "WHERE p.title LIKE %:keyword% " +
           "OR p.description LIKE %:keyword% " +
           "OR h.description LIKE %:keyword% " +
           "OR pt LIKE %:keyword% " +
           "OR ht LIKE %:keyword% " +
           "OR st LIKE %:keyword% " +
           "OR s.storyNumber LIKE %:keyword% " +
           "OR s.platform LIKE %:keyword% " +
           "OR s.foundByDepartment LIKE %:keyword% " +
           "OR s.toSolveByDepartment LIKE %:keyword% " +
           "OR s.managerName LIKE %:keyword% " +
           "OR s.whatToFix LIKE %:keyword%")
    List<Postmortem> search(@Param("keyword") String keyword);
}
