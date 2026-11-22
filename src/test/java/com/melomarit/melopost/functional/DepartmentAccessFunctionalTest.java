package com.melomarit.melopost.functional;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.PostmortemDocumentRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.UserRepository;
import com.melomarit.melopost.support.AbstractCassandraIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc tests against a real Cassandra: department scoping and the Published lock on every route. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DepartmentAccessFunctionalTest extends AbstractCassandraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostmortemRepository postmortemRepository;
    @Autowired
    private PostmortemDocumentRepository documentRepository;
    @Autowired
    private UserRepository userRepository;

    private Postmortem pmA;
    private Postmortem pmAPub;
    private Postmortem pmB;
    private Postmortem pmNoDept;
    private PostmortemDocument docA;
    private PostmortemDocument docB;
    private PostmortemDocument docAPub;

    private final RequestPostProcessor deptA = user("dept_a_user").roles("USER");
    private final RequestPostProcessor admin = user("admin_test").roles("ADMIN");

    @BeforeEach
    void setUp() {
        User a = new User();
        a.setUsername("dept_a_user");
        a.setPassword("password");
        a.getRoles().add("USER");
        a.getAllowedDepartments().add("A");
        userRepository.save(a);

        User adm = new User();
        adm.setUsername("admin_test");
        adm.setPassword("password");
        adm.getRoles().add("ADMIN");
        userRepository.save(adm);

        pmA = savePm("PM A", "A", "Triggered", null);
        pmAPub = savePm("PM A published", "A", "Published", null);
        pmB = savePm("PM B", "B", "Triggered", "Local postmortem");
        pmNoDept = savePm("PM no dept", null, "Triggered", null);

        docA = saveDoc(pmA.getUuid());
        docB = saveDoc(pmB.getUuid());
        docAPub = saveDoc(pmAPub.getUuid());
    }

    @AfterEach
    void tearDown() {
        postmortemRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Postmortem savePm(String title, String dept, String status, String type) {
        Postmortem pm = new Postmortem();
        pm.setTitle(title);
        pm.setDepartment(dept);
        pm.setStatus(status);
        if (type != null) {
            pm.setType(type);
        }
        return postmortemRepository.save(pm);
    }

    private PostmortemDocument saveDoc(UUID pmId) {
        PostmortemDocument doc = new PostmortemDocument();
        doc.setFileName("f.txt");
        doc.setContentType("text/plain");
        doc.setData("secret".getBytes());
        doc.setSize(6);
        doc.setPostmortemUuid(pmId);
        return documentRepository.save(doc);
    }

    private String titleOf(UUID id) {
        return postmortemRepository.findById(id).orElseThrow().getTitle();
    }

    private int docCount(UUID pmId) {
        return documentRepository.findByPostmortemUuid(pmId).size();
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "new.txt", "text/plain", "hello".getBytes());
    }

    private String locked(UUID id) {
        return "/postmortems/details/" + id + "?error=locked";
    }

    // ---- postmortems: department ----

    @Test
    void api_otherDepartment_getPutDelete_forbidden_andRowUnchanged() throws Exception {
        mockMvc.perform(get("/api/postmortems/" + pmB.getUuid()).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/postmortems/" + pmB.getUuid()).with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/postmortems/" + pmB.getUuid()).with(deptA).with(csrf())).andExpect(status().isForbidden());
        assertThat(titleOf(pmB.getUuid())).isEqualTo("PM B");
    }

    @Test
    void ui_otherDepartment_detailsEditSave_forbidden_andRowUnchanged() throws Exception {
        mockMvc.perform(get("/postmortems/details/" + pmB.getUuid()).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/postmortems/edit/" + pmB.getUuid()).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(post("/postmortems/save").with(deptA).with(csrf())
                .param("uuid", pmB.getUuid().toString()).param("title", "hacked").param("department", "A"))
                .andExpect(status().isForbidden());
        assertThat(titleOf(pmB.getUuid())).isEqualTo("PM B");
        assertThat(postmortemRepository.findById(pmB.getUuid()).orElseThrow().getDepartment()).isEqualTo("B");
    }

    @Test
    void reportRoutes_otherDepartment_forbidden() throws Exception {
        mockMvc.perform(get("/api/postmortems/" + pmB.getUuid() + "/report").with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/postmortems/" + pmB.getUuid() + "/report").with(deptA)).andExpect(status().isForbidden());
    }

    @Test
    void noDepartmentPostmortem_forbiddenForNonAdmin_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/postmortems/" + pmNoDept.getUuid()).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/postmortems/" + pmNoDept.getUuid()).with(admin)).andExpect(status().isOk());
    }

    @Test
    void newForm_localPostmortemsContainsOnlyVisible() throws Exception {
        MvcResult result = mockMvc.perform(get("/postmortems/new").with(deptA)).andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        List<Postmortem> local = (List<Postmortem>) result.getModelAndView().getModel().get("localPostmortems");
        assertThat(local).extracting(Postmortem::getUuid).doesNotContain(pmB.getUuid());
    }

    @Test
    void api_create_ignoresClientUuid_andLeavesExistingUntouched() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/postmortems").with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"uuid\":\"" + pmA.getUuid() + "\",\"title\":\"new one\",\"department\":\"A\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(pmA.getUuid().toString());
        assertThat(titleOf(pmA.getUuid())).isEqualTo("PM A");
    }

    @Test
    void api_create_inOtherDepartment_forbidden() throws Exception {
        mockMvc.perform(post("/api/postmortems").with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"department\":\"B\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void api_create_withForgedLinkToInvisiblePostmortem_forbidden() throws Exception {
        mockMvc.perform(post("/api/postmortems").with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"department\":\"A\",\"localPostmortemUuids\":[\"" + pmB.getUuid() + "\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ui_saveWithUuidOfDeletedPostmortem_isNotFound_andDoesNotExposeSurvivingDocuments() throws Exception {
        UUID gone = pmB.getUuid();
        postmortemRepository.deleteById(gone);
        mockMvc.perform(post("/postmortems/save").with(deptA).with(csrf())
                .param("uuid", gone.toString()).param("title", "recreated").param("department", "A"))
                .andExpect(status().isNotFound());
        assertThat(postmortemRepository.findById(gone)).isEmpty();
        mockMvc.perform(get("/api/postmortem-documents/" + docB.getUuid()).with(deptA)).andExpect(status().isNotFound());
    }

    @Test
    void ui_saveNewPostmortemWithoutUuid_createsWithServerUuid() throws Exception {
        mockMvc.perform(post("/postmortems/save").with(deptA).with(csrf())
                .param("title", "brand new").param("department", "A"))
                .andExpect(status().is3xxRedirection());
        assertThat(postmortemRepository.findAll()).extracting(Postmortem::getTitle).contains("brand new");
    }

    // ---- postmortems: positive controls ----

    @Test
    void ownDepartment_getAndPut_allowed() throws Exception {
        mockMvc.perform(get("/api/postmortems/" + pmA.getUuid()).with(deptA)).andExpect(status().isOk());
        mockMvc.perform(put("/api/postmortems/" + pmA.getUuid()).with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"changed\"}")).andExpect(status().isOk());
        assertThat(titleOf(pmA.getUuid())).isEqualTo("changed");
    }

    @Test
    void admin_canReadOtherDepartment_andUpdatePublished() throws Exception {
        mockMvc.perform(get("/api/postmortems/" + pmB.getUuid()).with(admin)).andExpect(status().isOk());
        mockMvc.perform(put("/api/postmortems/" + pmAPub.getUuid()).with(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"by admin\"}")).andExpect(status().isOk());
        assertThat(titleOf(pmAPub.getUuid())).isEqualTo("by admin");
    }

    // ---- documents: department ----

    @Test
    void api_documentsOfOtherDepartment_forbidden_andNothingChanges() throws Exception {
        UUID doc = docB.getUuid();
        UUID pm = pmB.getUuid();
        mockMvc.perform(get("/api/postmortem-documents/" + doc).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/postmortem-documents/" + doc + "/download").with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/postmortem-documents/postmortem/" + pm).with(deptA)).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/postmortem-documents/upload/" + pm).file(file()).with(deptA).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/postmortem-documents/" + doc).with(deptA).with(csrf())).andExpect(status().isForbidden());
        assertThat(documentRepository.findById(doc)).isPresent();
        assertThat(docCount(pm)).isEqualTo(1);
        assertThat(new String(documentRepository.findById(doc).orElseThrow().getData())).isEqualTo("secret");
    }

    @Test
    void api_documentList_excludesOtherDepartment() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/postmortem-documents").with(deptA))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(docA.getUuid().toString()).doesNotContain(docB.getUuid().toString());
    }

    @Test
    void ui_documentsOfOtherDepartment_downloadUploadDelete_forbidden() throws Exception {
        UUID pm = pmB.getUuid();
        mockMvc.perform(get("/postmortems/" + pm + "/documents/" + docB.getUuid() + "/download").with(deptA))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/postmortems/" + pm + "/documents/upload").file(file()).with(deptA).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/postmortems/" + pm + "/documents/" + docB.getUuid() + "/delete").with(deptA).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(documentRepository.findById(docB.getUuid())).isPresent();
        assertThat(docCount(pm)).isEqualTo(1);
    }

    @Test
    void ui_documentOfAnotherPostmortem_isNotFound_andStillExists() throws Exception {
        mockMvc.perform(get("/postmortems/" + pmA.getUuid() + "/documents/" + docB.getUuid() + "/download").with(deptA))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/postmortems/" + pmA.getUuid() + "/documents/" + docB.getUuid() + "/delete").with(deptA).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(documentRepository.findById(docB.getUuid())).isPresent();
    }

    // ---- documents: positive controls ----

    @Test
    void ownDepartment_documentOperations_allowed() throws Exception {
        mockMvc.perform(get("/api/postmortem-documents/" + docA.getUuid() + "/download").with(deptA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/postmortems/" + pmA.getUuid() + "/documents/" + docA.getUuid() + "/download").with(deptA))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/postmortem-documents/upload/" + pmA.getUuid()).file(file()).with(deptA).with(csrf()))
                .andExpect(status().isOk());
        assertThat(docCount(pmA.getUuid())).isEqualTo(2);
        mockMvc.perform(delete("/api/postmortem-documents/" + docA.getUuid()).with(deptA).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(documentRepository.findById(docA.getUuid())).isEmpty();
    }

    // ---- Published lock ----

    @Test
    void api_publishedOwnDepartment_putDeleteAndDocuments_forbidden_nothingPersisted() throws Exception {
        UUID pm = pmAPub.getUuid();
        mockMvc.perform(put("/api/postmortems/" + pm).with(deptA).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/postmortems/" + pm).with(deptA).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/postmortem-documents/upload/" + pm).file(file()).with(deptA).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/postmortem-documents/" + docAPub.getUuid()).with(deptA).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(titleOf(pm)).isEqualTo("PM A published");
        assertThat(docCount(pm)).isEqualTo(1);
        assertThat(documentRepository.findById(docAPub.getUuid())).isPresent();
    }

    @Test
    void ui_publishedOwnDepartment_allChangeRoutes_redirectLocked_nothingPersisted() throws Exception {
        UUID pm = pmAPub.getUuid();
        String expected = locked(pm);

        mockMvc.perform(post("/postmortems/save").with(deptA).with(csrf())
                .param("uuid", pm.toString()).param("title", "x").param("department", "A"))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/note").with(deptA).with(csrf()).param("note", "n"))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/tags/add").with(deptA).with(csrf()).param("tag", "t"))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/tags/remove").with(deptA).with(csrf()).param("tag", "t"))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/questions/add").with(deptA).with(csrf())
                .param("cheeseLayer", "Build").param("question", "q"))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/questions/" + UUID.randomUUID() + "/remove").with(deptA).with(csrf()))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/link").with(deptA).with(csrf()).param("linkedId", pmA.getUuid().toString()))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/unlink").with(deptA).with(csrf()).param("linkedId", pmA.getUuid().toString()))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/incident-notes/import").with(deptA).with(csrf())
                .param("noteId", UUID.randomUUID().toString()))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(multipart("/postmortems/" + pm + "/documents/upload").file(file()).with(deptA).with(csrf()))
                .andExpect(redirectedUrl(expected));
        mockMvc.perform(post("/postmortems/" + pm + "/documents/" + docAPub.getUuid() + "/delete").with(deptA).with(csrf()))
                .andExpect(redirectedUrl(expected));

        Postmortem stored = postmortemRepository.findById(pm).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("PM A published");
        assertThat(stored.getNote()).isNull();
        assertThat(stored.getTags()).isEmpty();
        assertThat(stored.getLocalPostmortemUuids()).isEmpty();
        assertThat(docCount(pm)).isEqualTo(1);
        assertThat(documentRepository.findById(docAPub.getUuid())).isPresent();
    }

    @Test
    void ui_ownDepartment_notPublished_noteAndUpload_allowed() throws Exception {
        UUID pm = pmA.getUuid();
        mockMvc.perform(post("/postmortems/" + pm + "/note").with(deptA).with(csrf()).param("note", "hello"))
                .andExpect(redirectedUrl("/postmortems/details/" + pm));
        mockMvc.perform(multipart("/postmortems/" + pm + "/documents/upload").file(file()).with(deptA).with(csrf()))
                .andExpect(redirectedUrl("/postmortems/details/" + pm));
        assertThat(postmortemRepository.findById(pm).orElseThrow().getNote()).isEqualTo("hello");
        assertThat(docCount(pm)).isEqualTo(2);
    }

    @Test
    void ui_link_toInvisiblePostmortem_forbidden() throws Exception {
        mockMvc.perform(post("/postmortems/" + pmA.getUuid() + "/link").with(deptA).with(csrf())
                .param("linkedId", pmB.getUuid().toString()))
                .andExpect(status().isForbidden());
        assertThat(postmortemRepository.findById(pmA.getUuid()).orElseThrow().getLocalPostmortemUuids()).isEmpty();
    }
}
