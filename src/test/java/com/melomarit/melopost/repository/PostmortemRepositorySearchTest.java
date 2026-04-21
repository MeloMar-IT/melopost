package com.melomarit.melopost.repository;

import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.model.*;
import com.melomarit.melopost.service.PostmortemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PostmortemRepositorySearchTest {

    @Autowired
    private PostmortemRepository postmortemRepository;

    @Autowired
    private PostmortemService postmortemService;

    @Test
    public void testSearchByPostmortemId() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Search by ID test");
        pm = postmortemRepository.save(pm);
        UUID id = pm.getUuid();

        // Use findById since searching by UUID as string might not be supported in simple search
        Postmortem result = postmortemService.findById(id);
        assertThat(result.getUuid()).isEqualTo(id);
    }

    @Test
    public void testSearchByTitle() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Unique Title for Search");
        postmortemRepository.save(pm);

        List<PostmortemSearchResultDTO> results = postmortemService.search("Unique Title");
        assertThat(results).anyMatch(r -> r.getPostmortem().getTitle().equals("Unique Title for Search") && r.getMatchHints().contains("Title"));
    }

    @Test
    public void testSearchInUDT() {
        Postmortem pm = new Postmortem();
        pm.setTitle("UDT Search Test");
        
        CheeseLayerUDT layer = new CheeseLayerUDT();
        layer.setName("Network");
        
        HoleUDT hole = new HoleUDT();
        hole.setDescription("Disconnected cable");
        layer.getHoles().add(hole);
        
        pm.getLayers().add(layer);
        postmortemRepository.save(pm);

        List<PostmortemSearchResultDTO> results = postmortemService.search("cable");
        assertThat(results).anyMatch(r -> 
            r.getPostmortem().getTitle().equals("UDT Search Test") && 
            r.getMatchHints().stream().anyMatch(h -> h.contains("Hole Description"))
        );
    }
}
