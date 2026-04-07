package com.melo.melopost;

import com.melo.melopost.model.CheeseLayer;
import com.melo.melopost.model.Hole;
import com.melo.melopost.model.Postmortem;
import com.melo.melopost.model.Story;
import com.melo.melopost.service.PostmortemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SwissCheeseModelTest {

    @Autowired
    private PostmortemService service;

    @Test
    @Transactional
    void testSavePostmortemWithFullSwissCheeseModel() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Swiss Cheese Test");
        pm.setDescription("Testing multiple holes and stories per slice");

        CheeseLayer layer1 = new CheeseLayer();
        layer1.setName("Design");

        Hole hole1 = new Hole();
        hole1.setDescription("Missing input validation");
        Story story1 = new Story();
        story1.setStoryNumber("STORY-101");
        story1.setPriority("1");
        story1.setWhatToFix("Add validation to the user registration form");
        hole1.setStory(story1);
        layer1.getHoles().add(hole1);

        Hole hole2 = new Hole();
        hole2.setDescription("Weak password policy");
        layer1.getHoles().add(hole2);

        pm.addLayer(layer1);

        Postmortem saved = service.save(pm);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLayers()).hasSize(1);
        
        CheeseLayer savedLayer = saved.getLayers().get(0);
        assertThat(savedLayer.getName()).isEqualTo("Design");
        assertThat(savedLayer.getHoles()).hasSize(2);
        assertThat(savedLayer.getHoles().get(0).getStory()).isNotNull();
        assertThat(savedLayer.getHoles().get(0).getStory().getStoryNumber()).isEqualTo("STORY-101");
        assertThat(savedLayer.getHoles().get(0).getStory().getPriority()).isEqualTo("1");

        // Verify we can find it back
        Postmortem found = service.findById(saved.getId());
        assertThat(found.getLayers().get(0).getHoles()).hasSize(2);
        assertThat(found.getLayers().get(0).getHoles().get(0).getStory().getStoryNumber()).isEqualTo("STORY-101");
    }
}
