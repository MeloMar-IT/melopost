package com.melomarit.melopost.service;

import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.model.*;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostmortemServiceTest {

    @Mock
    private PostmortemRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PostmortemService service;

    private User adminUser;
    private User regularUser;
    private Postmortem pmInDept;
    private Postmortem pmOtherDept;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setRoles(Collections.singleton("ADMIN"));

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setRoles(Collections.singleton("USER"));
        regularUser.setAllowedDepartments(Collections.singleton("IT"));

        pmInDept = new Postmortem();
        pmInDept.setUuid(UUID.randomUUID());
        pmInDept.setDepartment("IT");
        pmInDept.setTitle("IT Incident");
        pmInDept.setCreatedAt(LocalDateTime.now());

        pmOtherDept = new Postmortem();
        pmOtherDept.setUuid(UUID.randomUUID());
        pmOtherDept.setDepartment("HR");
        pmOtherDept.setTitle("HR Incident");
        pmOtherDept.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    private void mockUser(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    @Test
    void findAll_admin_shouldReturnAll() {
        mockUser(adminUser);
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        
        List<Postmortem> result = service.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void findAll_regularUser_shouldFilter() {
        mockUser(regularUser);
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        
        List<Postmortem> result = service.findAll();
        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).getDepartment());
    }

    @Test
    void findRecent_shouldSortAndLimit() {
        mockUser(adminUser);
        when(repository.findAll()).thenReturn(Arrays.asList(pmOtherDept, pmInDept)); // Older first
        
        List<Postmortem> result = service.findRecent();
        assertEquals(2, result.size());
        assertEquals(pmInDept.getUuid(), result.get(0).getUuid()); // Newer should be first
    }

    @Test
    void findById_regularUser_accessDenied() {
        mockUser(regularUser);
        when(repository.findById(pmOtherDept.getUuid())).thenReturn(Optional.of(pmOtherDept));
        
        assertThrows(RuntimeException.class, () -> service.findById(pmOtherDept.getUuid()));
    }

    @Test
    void filter_shouldWorkWithMultipleParams() {
        mockUser(adminUser);
        pmInDept.setType("Local postmortem");
        pmInDept.setIncidentRef("INC-100");
        pmInDept.setIncidentDate(LocalDateTime.of(2023, 10, 1, 10, 0));
        pmInDept.setDueDate(LocalDateTime.of(2023, 10, 22, 10, 0));
        
        pmOtherDept.setType("Major Postmortem");
        pmOtherDept.setIncidentRef("INC-200");
        pmOtherDept.setIncidentDate(LocalDateTime.of(2023, 11, 1, 10, 0));

        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        
        // Filter by type
        List<PostmortemSearchResultDTO> results = service.filter(null, "Local postmortem", null, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals(pmInDept.getUuid(), results.get(0).getPostmortem().getUuid());
        
        // Filter by incident ref
        results = service.filter(null, null, null, null, null, "INC-200", null, null);
        assertEquals(1, results.size());
        assertEquals(pmOtherDept.getUuid(), results.get(0).getPostmortem().getUuid());
        
        // Filter by date "on"
        results = service.filter(null, null, null, "2023-10-01", "on", null, null, null);
        assertEquals(1, results.size());
        assertEquals(pmInDept.getUuid(), results.get(0).getPostmortem().getUuid());

        // Filter by date "before"
        results = service.filter(null, null, null, "2023-10-15", "before", null, null, null);
        assertEquals(1, results.size());
        assertEquals(pmInDept.getUuid(), results.get(0).getPostmortem().getUuid());

        // Filter by date "after"
        results = service.filter(null, null, null, "2023-10-15", "after", null, null, null);
        assertEquals(1, results.size());
        assertEquals(pmOtherDept.getUuid(), results.get(0).getPostmortem().getUuid());

        // Filter by keyword + type
        pmInDept.setTitle("Database failure");
        results = service.filter("database", "Local postmortem", null, null, null, null, null, null);
        assertEquals(1, results.size());
        
        // Filter by keyword + wrong type
        results = service.filter("database", "Major Postmortem", null, null, null, null, null, null);
        assertEquals(0, results.size());
    }

    @Test
    void search_shouldFindInVariousFields() {
        mockUser(adminUser);
        pmInDept.setNote("secret keyword");
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        
        List<PostmortemSearchResultDTO> results = service.search("keyword");
        assertEquals(1, results.size());
        assertEquals(pmInDept.getUuid(), results.get(0).getPostmortem().getUuid());
    }

    @Test
    void search_withUDTs_shouldFind() {
        mockUser(adminUser);
        CheeseLayerUDT layer = new CheeseLayerUDT();
        layer.setName("Network Layer");
        pmInDept.setLayers(Collections.singletonList(layer));
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept));
        
        List<PostmortemSearchResultDTO> results = service.search("network");
        assertEquals(1, results.size());
        assertTrue(results.get(0).getMatchHints().contains("Layer: Network Layer"));
    }

    @Test
    void findMajorPostmortems_shouldFilterByLocalUuid() {
        UUID localUuid = UUID.randomUUID();
        pmInDept.setLocalPostmortemUuids(Collections.singletonList(localUuid));
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        
        List<Postmortem> result = service.findMajorPostmortems(localUuid);
        assertEquals(1, result.size());
        assertEquals(pmInDept.getUuid(), result.get(0).getUuid());
    }

    @Test
    void save_shouldThrowException_whenIncidentRefExists() {
        Postmortem newPm = new Postmortem();
        newPm.setIncidentRef("INC-123");
        newPm.setUuid(UUID.randomUUID());

        Postmortem existingPm = new Postmortem();
        existingPm.setIncidentRef("INC-123");
        existingPm.setUuid(UUID.randomUUID());

        when(repository.findByIncidentRef("INC-123")).thenReturn(Collections.singletonList(existingPm));

        assertThrows(RuntimeException.class, () -> service.save(newPm));
    }

    @Test
    void save_shouldAllow_whenIncidentRefIsSamePostmortem() {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        pm.setIncidentRef("INC-123");

        when(repository.findByIncidentRef("INC-123")).thenReturn(Collections.singletonList(pm));
        when(repository.save(pm)).thenReturn(pm);

        Postmortem saved = service.save(pm);
        assertEquals(pm, saved);
        verify(repository).save(pm);
    }

    @Test
    void save_shouldAllow_whenIncidentRefIsEmpty() {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        pm.setIncidentRef("");

        when(repository.save(pm)).thenReturn(pm);

        Postmortem saved = service.save(pm);
        assertEquals(pm, saved);
        verify(repository, never()).findByIncidentRef(anyString());
    }
}
