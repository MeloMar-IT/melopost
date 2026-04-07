package com.melo.melopost;

import com.melo.melopost.model.CheeseLayer;
import com.melo.melopost.model.Hole;
import com.melo.melopost.model.Postmortem;
import com.melo.melopost.repository.PostmortemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PostmortemTagsTest {

    @Autowired
    private PostmortemRepository repository;

    @Test
    public void testPostmortemAndHoleTags() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Tag Test");
        pm.setIncidentDate(LocalDateTime.now());
        pm.setTags(Arrays.asList("tag1", "tag2"));

        CheeseLayer layer = new CheeseLayer();
        layer.setName("Run");
        
        Hole hole = new Hole();
        hole.setDescription("Hole with tags");
        hole.setTags(Arrays.asList("holeTag1", "holeTag2"));
        
        layer.getHoles().add(hole);
        pm.getLayers().add(layer);

        Postmortem saved = repository.save(pm);
        repository.flush();

        Postmortem found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
        
        Hole foundHole = found.getLayers().get(0).getHoles().get(0);
        assertThat(foundHole.getTags()).containsExactlyInAnyOrder("holeTag1", "holeTag2");
        
        // Test search by tag
        List<Postmortem> searchResult = repository.search("tag1");
        assertThat(searchResult).hasSize(1);
        
        List<Postmortem> holeSearchResult = repository.search("holeTag2");
        assertThat(holeSearchResult).hasSize(1);
    }
}
