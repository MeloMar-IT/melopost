package com.melomarit.melopost.controller;

import com.melomarit.melopost.service.PostmortemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HomeControllerTest {

    @Mock
    private PostmortemService postmortemService;

    @InjectMocks
    private HomeController homeController;

    @Test
    void index_shouldReturnHomeView() {
        Model model = new ConcurrentModel();
        when(postmortemService.findRecent()).thenReturn(new ArrayList<>());
        when(postmortemService.findAll()).thenReturn(new ArrayList<>());
        
        String view = homeController.index(model);
        
        assertEquals("index", view);
        assertEquals(true, model.containsAttribute("recentPostmortems"));
        assertEquals(true, model.containsAttribute("postmortemCount"));
    }
}
