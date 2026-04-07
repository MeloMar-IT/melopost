package com.melo.melopost;

import com.melo.melopost.model.Postmortem;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PostmortemDateTest {

    @Test
    void testDueDateCalculation() {
        Postmortem pm = new Postmortem();
        LocalDateTime incidentDate = LocalDateTime.of(2026, 4, 1, 10, 0);
        pm.setIncidentDate(incidentDate);
        
        assertThat(pm.getDueDate()).isEqualTo(incidentDate.plusWeeks(3));
    }

    @Test
    void testDueDateClearedWhenIncidentDateIsCleared() {
        Postmortem pm = new Postmortem();
        pm.setIncidentDate(LocalDateTime.now());
        assertThat(pm.getDueDate()).isNotNull();
        
        pm.setIncidentDate(null);
        assertThat(pm.getDueDate()).isNull();
    }

    @Test
    void testDateFields() {
        Postmortem pm = new Postmortem();
        LocalDateTime now = LocalDateTime.now();
        
        pm.setStartDate(now);
        pm.setPostMortemMeetingDate(now.plusDays(7));
        
        assertThat(pm.getStartDate()).isEqualTo(now);
        assertThat(pm.getPostMortemMeetingDate()).isEqualTo(now.plusDays(7));
    }
}
