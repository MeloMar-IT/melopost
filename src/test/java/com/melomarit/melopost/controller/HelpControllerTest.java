package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.DatabaseTableDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HelpControllerTest {

    @Mock
    private DatabaseAdminRestController databaseAdminRestController;

    @InjectMocks
    private HelpController helpController;

    @Test
    void help_defaultSection() {
        Model model = new ConcurrentModel();
        String view = helpController.help("userguide", model);
        assertEquals("help", view);
        assertEquals("userguide", model.getAttribute("currentSection"));
    }

    @Test
    void help_apiSection() {
        Model model = new ConcurrentModel();
        helpController.help("api", model);
        assertEquals("api", model.getAttribute("currentSection"));
        assertEquals(true, model.containsAttribute("apiEndpoints"));
    }

    @Test
    void help_databaseSection() {
        Model model = new ConcurrentModel();
        when(databaseAdminRestController.getTablesInternal(false)).thenReturn(new ArrayList<DatabaseTableDTO>());
        
        helpController.help("database", model);
        
        assertEquals("database", model.getAttribute("currentSection"));
        verify(databaseAdminRestController).getTablesInternal(false);
    }
}
