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

    @Test
    void save_incidentRefClash_throwsDuplicateIncidentRefException() {
        Postmortem newPm = new Postmortem();
        newPm.setIncidentRef("INC-9");
        Postmortem other = new Postmortem();
        other.setIncidentRef("INC-9");
        when(repository.findByIncidentRef("INC-9")).thenReturn(Collections.singletonList(other));
        assertThrows(DuplicateIncidentRefException.class, () -> service.save(newPm));
        verify(repository, never()).save(any());
    }

    // ---- authorization ----

    @Test
    void findById_otherDepartment_throwsAccessDenied() {
        mockUser(regularUser);
        when(repository.findById(pmOtherDept.getUuid())).thenReturn(Optional.of(pmOtherDept));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.findById(pmOtherDept.getUuid()));
    }

    @Test
    void findById_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        org.springframework.web.server.ResponseStatusException e = assertThrows(
                org.springframework.web.server.ResponseStatusException.class, () -> service.findById(id));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void findById_noDepartment_deniedForRegularUserAllowedForAdmin() {
        pmInDept.setDepartment(null);
        when(repository.findById(pmInDept.getUuid())).thenReturn(Optional.of(pmInDept));
        mockUser(regularUser);
        assertThrows(PostmortemAccessDeniedException.class, () -> service.findById(pmInDept.getUuid()));

        pmInDept.setDepartment("   ");
        assertThrows(PostmortemAccessDeniedException.class, () -> service.findById(pmInDept.getUuid()));
    }

    @Test
    void findById_noDepartment_allowedForAdmin() {
        pmInDept.setDepartment(null);
        when(repository.findById(pmInDept.getUuid())).thenReturn(Optional.of(pmInDept));
        mockUser(adminUser);
        assertSame(pmInDept, service.findById(pmInDept.getUuid()));
    }

    @Test
    void authorize_publishedOwnDepartment_updateAndDeleteLocked_viewAllowed() {
        mockUser(regularUser);
        pmInDept.setStatus("Published");
        assertThrows(PostmortemLockedException.class, () -> service.authorize(pmInDept, PostmortemAccessPolicy.Action.UPDATE));
        assertThrows(PostmortemLockedException.class, () -> service.authorize(pmInDept, PostmortemAccessPolicy.Action.DELETE));
        assertDoesNotThrow(() -> service.authorize(pmInDept, PostmortemAccessPolicy.Action.VIEW));
    }

    @Test
    void authorize_publishedOwnDepartment_adminMayUpdate() {
        mockUser(adminUser);
        pmInDept.setStatus("Published");
        assertDoesNotThrow(() -> service.authorize(pmInDept, PostmortemAccessPolicy.Action.UPDATE));
    }

    @Test
    void authorize_roleAdminAuthorityWithoutDbUser_isUnrestricted() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        assertDoesNotThrow(() -> service.authorize(pmOtherDept, PostmortemAccessPolicy.Action.DELETE));
    }

    @Test
    void authenticatedUserMissingInDb_seesNothingAndIsDenied() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        assertTrue(service.findAll().isEmpty());
        assertThrows(PostmortemAccessDeniedException.class,
                () -> service.authorize(pmInDept, PostmortemAccessPolicy.Action.VIEW));
    }

    @Test
    void save_overExistingOtherDepartmentRow_isDeniedAndNothingSaved() {
        mockUser(regularUser);
        Postmortem incoming = new Postmortem();
        incoming.setUuid(pmOtherDept.getUuid());
        incoming.setDepartment("IT");
        when(repository.findById(pmOtherDept.getUuid())).thenReturn(Optional.of(pmOtherDept));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.save(incoming));
        verify(repository, never()).save(any());
    }

    @Test
    void save_overExistingPublishedRow_isLockedAndNothingSaved() {
        mockUser(regularUser);
        pmInDept.setStatus("Published");
        Postmortem incoming = new Postmortem();
        incoming.setUuid(pmInDept.getUuid());
        incoming.setDepartment("IT");
        when(repository.findById(pmInDept.getUuid())).thenReturn(Optional.of(pmInDept));
        assertThrows(PostmortemLockedException.class, () -> service.save(incoming));
        verify(repository, never()).save(any());
    }

    @Test
    void save_movingToNonAllowedDepartment_isDenied() {
        mockUser(regularUser);
        Postmortem incoming = new Postmortem();
        incoming.setUuid(pmInDept.getUuid());
        incoming.setDepartment("HR");
        when(repository.findById(pmInDept.getUuid())).thenReturn(Optional.of(pmInDept));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.save(incoming));
        verify(repository, never()).save(any());
    }

    @Test
    void save_newPostmortemInNonAllowedOrBlankDepartment_isDenied() {
        mockUser(regularUser);
        Postmortem hr = new Postmortem();
        hr.setDepartment("HR");
        Postmortem blank = new Postmortem();
        blank.setDepartment("");
        assertThrows(PostmortemAccessDeniedException.class, () -> service.save(hr));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.save(blank));
        verify(repository, never()).save(any());
    }

    @Test
    void save_newPostmortemInOwnDepartment_isSaved() {
        mockUser(regularUser);
        Postmortem fresh = new Postmortem();
        fresh.setDepartment("IT");
        when(repository.save(fresh)).thenReturn(fresh);
        assertSame(fresh, service.save(fresh));
    }

    @Test
    void save_newLinkToInvisiblePostmortem_isDenied() {
        mockUser(regularUser);
        Postmortem fresh = new Postmortem();
        fresh.setDepartment("IT");
        fresh.getLocalPostmortemUuids().add(pmOtherDept.getUuid());
        when(repository.findById(fresh.getUuid())).thenReturn(Optional.empty());
        when(repository.findById(pmOtherDept.getUuid())).thenReturn(Optional.of(pmOtherDept));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.save(fresh));
        verify(repository, never()).save(any());
    }

    @Test
    void save_existingInvisibleLinkIsPreserved() {
        mockUser(regularUser);
        Postmortem stored = new Postmortem();
        stored.setDepartment("IT");
        stored.getLocalPostmortemUuids().add(pmOtherDept.getUuid());
        Postmortem incoming = new Postmortem();
        incoming.setUuid(stored.getUuid());
        incoming.setDepartment("IT");
        incoming.getLocalPostmortemUuids().add(pmOtherDept.getUuid());
        when(repository.findById(stored.getUuid())).thenReturn(Optional.of(stored));
        when(repository.save(incoming)).thenReturn(incoming);
        assertSame(incoming, service.save(incoming));
    }

    @Test
    void create_replacesSuppliedUuid() {
        Postmortem incoming = new Postmortem();
        UUID supplied = pmOtherDept.getUuid();
        incoming.setUuid(supplied);
        when(repository.save(incoming)).thenReturn(incoming);
        Postmortem created = service.create(incoming);
        assertNotEquals(supplied, created.getUuid());
    }

    @Test
    void deleteById_otherDepartment_isDeniedAndNothingDeleted() {
        mockUser(regularUser);
        when(repository.findById(pmOtherDept.getUuid())).thenReturn(Optional.of(pmOtherDept));
        assertThrows(PostmortemAccessDeniedException.class, () -> service.deleteById(pmOtherDept.getUuid()));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteById_publishedOwnDepartment_isLocked() {
        mockUser(regularUser);
        pmInDept.setStatus("Published");
        when(repository.findById(pmInDept.getUuid())).thenReturn(Optional.of(pmInDept));
        assertThrows(PostmortemLockedException.class, () -> service.deleteById(pmInDept.getUuid()));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteById_missing_isNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.deleteById(id));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void findByType_regularUser_isFiltered() {
        mockUser(regularUser);
        when(repository.findByType("Local postmortem")).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        List<Postmortem> result = service.findByType("Local postmortem");
        assertEquals(List.of(pmInDept), result);
    }

    @Test
    void findMajorPostmortems_regularUser_isFiltered() {
        mockUser(regularUser);
        UUID localUuid = UUID.randomUUID();
        pmInDept.setLocalPostmortemUuids(Collections.singletonList(localUuid));
        pmOtherDept.setLocalPostmortemUuids(Collections.singletonList(localUuid));
        when(repository.findAll()).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        assertEquals(List.of(pmInDept), service.findMajorPostmortems(localUuid));
    }

    @Test
    void findVisibleByIds_regularUser_isFiltered() {
        mockUser(regularUser);
        List<UUID> ids = Arrays.asList(pmInDept.getUuid(), pmOtherDept.getUuid());
        when(repository.findAllById(ids)).thenReturn(Arrays.asList(pmInDept, pmOtherDept));
        assertEquals(List.of(pmInDept), service.findVisibleByIds(ids));
        assertTrue(service.findVisibleByIds(Collections.emptyList()).isEmpty());
        assertTrue(service.findVisibleByIds(null).isEmpty());
    }
}
