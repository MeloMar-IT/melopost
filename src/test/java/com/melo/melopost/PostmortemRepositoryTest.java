package com.melo.melopost;

import com.melo.melopost.model.CheeseLayer;
import com.melo.melopost.model.Hole;
import com.melo.melopost.model.Postmortem;
import com.melo.melopost.model.Story;
import com.melo.melopost.repository.PostmortemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostmortemRepositoryTest {

    @Autowired
    private PostmortemRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSearchByDepartmentAndManager() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Network Failure");
        
        CheeseLayer layer = new CheeseLayer();
        layer.setName("Infrastructure");
        
        Hole hole = new Hole();
        hole.setDescription("Old switch failed");
        
        Story story = new Story();
        story.setStoryNumber("JIRA-101");
        story.setPlatform("Jira");
        story.setFoundByDepartment("IT Operations");
        story.setToSolveByDepartment("Network Engineering");
        story.setManagerName("John Doe");
        
        hole.setStory(story);
        layer.getHoles().add(hole);
        pm.getLayers().add(layer);
        
        repository.save(pm);

        // Search by foundByDepartment
        List<Postmortem> results = repository.search("Operations");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Network Failure");

        // Search by toSolveByDepartment
        results = repository.search("Engineering");
        assertThat(results).hasSize(1);

        // Search by manager name
        results = repository.search("Doe");
        assertThat(results).hasSize(1);

        // Search by story number
        results = repository.search("JIRA-101");
        assertThat(results).hasSize(1);
    }

    @Test
    void testSearchByPlatform() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Database Latency");
        
        CheeseLayer layer = new CheeseLayer();
        layer.setName("Software Design");
        
        Hole hole = new Hole();
        hole.setDescription("Missing index");
        
        Story story = new Story();
        story.setStoryNumber("AD-202");
        story.setPlatform("Azure DevOps");
        
        hole.setStory(story);
        layer.getHoles().add(hole);
        pm.getLayers().add(layer);
        
        repository.save(pm);

        List<Postmortem> results = repository.search("Azure");
        assertThat(results).hasSize(1);
    }
}
