package com.melomarit.melopost.service;

import com.melomarit.melopost.model.CheeseLayerUDT;
import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.model.TimelineEventUDT;
import com.samskivert.mustache.Mustache;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Checks that PDF rendering fetches nothing it should not and that report fields are sanitized. */
class ReportServiceSecurityTest {

    private static final String XHTML_DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";

    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(s -> s.stop(0));
    }

    private HttpServer server(AtomicInteger hits, String redirectTo) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        s.createContext("/", ex -> {
            hits.incrementAndGet();
            if (redirectTo != null) {
                ex.getResponseHeaders().add("Location", redirectTo);
                ex.sendResponseHeaders(302, -1);
            } else {
                ex.getResponseHeaders().add("Content-Type", "text/css");
                byte[] body = "body{color:red}".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            }
            ex.close();
        });
        s.start();
        servers.add(s);
        return s;
    }

    private static String base(HttpServer s) {
        return "http://127.0.0.1:" + s.getAddress().getPort();
    }

    private ReportService service(String dbTemplate, ReportResourcePolicy policy) {
        ReportTemplateService templates = mock(ReportTemplateService.class);
        if (dbTemplate == null) {
            when(templates.findDefault()).thenReturn(Optional.empty());
        } else {
            ReportTemplate t = new ReportTemplate();
            t.setName("t");
            t.setContent(dbTemplate);
            when(templates.findDefault()).thenReturn(Optional.of(t));
        }
        PostmortemService pms = mock(PostmortemService.class);
        when(pms.findVisibleByIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(pms.findMajorPostmortems(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        return new ReportService(Mustache.compiler(), new DefaultResourceLoader(), templates, pms, policy);
    }

    private static Postmortem postmortem() {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        pm.setTitle("T");
        return pm;
    }

    private static Postmortem withHole(Postmortem pm, String description, String action) {
        CheeseLayerUDT layer = new CheeseLayerUDT();
        layer.setName("L");
        layer.setDescription("layer description");
        HoleUDT h = new HoleUDT();
        h.setTeamName("team");
        h.setActionStatus("OPEN");
        h.setDescription(description);
        h.setRemedialAction(action);
        layer.getHoles().add(h);
        pm.getLayers().add(layer);
        return pm;
    }

    private static Postmortem withEvent(Postmortem pm) {
        TimelineEventUDT e = new TimelineEventUDT();
        e.setEventTime(LocalDateTime.of(2023, 10, 27, 10, 0));
        e.setDescription("Event 1");
        pm.getTimelineEvents().add(e);
        return pm;
    }

    private static String page(String head, String body) {
        return "<!DOCTYPE html><html><head><title>x</title>" + head + "</head><body>" + body + "</body></html>";
    }

    // ---- loopback / SSRF ----

    @Test
    void pdf_doesNotFetchLoopback_fromDefaultDbTemplate() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        String b = base(server(hits, null));
        String tpl = page("<link rel=\"stylesheet\" href=\"" + b + "/a.css\"/><style>div{background:url(" + b + "/b.png)}</style>",
                "<img src=\"" + b + "/c.png\"/><div>x</div>");
        byte[] pdf = service(tpl, new ReportResourcePolicy(false, "", "")).generatePostmortemPdf(postmortem());
        assertTrue(pdf.length > 0);
        assertEquals(0, hits.get(), "loopback server must not be contacted");
    }

    @Test
    void pdf_controlCase_allowListedHostIsFetched() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        String tpl = page("<link rel=\"stylesheet\" href=\"" + base(server(hits, null)) + "/a.css\"/>", "<p>x</p>");
        byte[] pdf = service(tpl, new ReportResourcePolicy(false, "", "127.0.0.1")).generatePostmortemPdf(postmortem());
        assertTrue(pdf.length > 0);
        assertTrue(hits.get() > 0, "allow-listed host should have been fetched");
    }

    @Test
    void pdf_redirectFromAllowListedHostIsNotFollowed() throws Exception {
        AtomicInteger hitsB = new AtomicInteger();
        AtomicInteger hitsA = new AtomicInteger();
        HttpServer b = server(hitsB, null);
        HttpServer a = server(hitsA, base(b) + "/secret.css");
        String tpl = page("<link rel=\"stylesheet\" href=\"" + base(a) + "/a.css\"/>", "<p>x</p>");
        byte[] pdf = service(tpl, new ReportResourcePolicy(false, "", "127.0.0.1")).generatePostmortemPdf(postmortem());
        assertTrue(pdf.length > 0);
        assertTrue(hitsA.get() > 0, "control: allow-listed server was contacted");
        assertEquals(0, hitsB.get(), "redirect target must not be contacted");
    }

    @Test
    void pdf_holeDescriptionImgNotFetched_withRealTemplate() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        String b = base(server(hits, null));
        Postmortem pm = withHole(postmortem(), "<img src=\"" + b + "/h.png\">line1<br>line2&nbsp;end", "<img src=\"" + b + "/r.png\">fix");
        byte[] pdf = service(null, new ReportResourcePolicy(false, "", "127.0.0.1")).generatePostmortemPdf(pm);
        assertTrue(pdf.length > 0);
        assertEquals(0, hits.get());
    }

    // ---- XML hardening ----

    @Test
    void parseXhtml_doesNotExpandExternalFileEntity() throws Exception {
        Path secret = Files.createTempFile("secret", ".txt");
        try {
            Files.writeString(secret, "SECRET-MARKER-8");
            String html = "<!DOCTYPE html [<!ENTITY x SYSTEM \"" + secret.toUri() + "\">]><html><body><p>&x;</p></body></html>";
            try {
                String text = ReportService.parseXhtml(html).getDocumentElement().getTextContent();
                assertFalse(text.contains("SECRET-MARKER-8"));
            } catch (IOException expected) {
                assertFalse(String.valueOf(expected.getMessage()).contains("SECRET-MARKER-8"));
            }
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void parseXhtml_standardXhtmlDoctypeResolvesNamedEntities() throws Exception {
        String html = XHTML_DOCTYPE + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body><p>&copy; x</p></body></html>";
        assertTrue(ReportService.parseXhtml(html).getDocumentElement().getTextContent().contains("©"));
    }

    @Test
    void pdf_xhtmlDoctypeWithNamedEntityStillRenders() throws Exception {
        String tpl = XHTML_DOCTYPE + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body><p>&copy; {{title}}</p></body></html>";
        byte[] pdf = service(tpl, new ReportResourcePolicy(false, "", "")).generatePostmortemPdf(postmortem());
        assertTrue(pdf.length > 0);
    }

    @Test
    void parseXhtml_resolvesFullXhtmlEntityCatalog() throws Exception {
        String html = XHTML_DOCTYPE + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body><p>&alpha;|&sum;|&copy;|&hellip;</p></body></html>";
        String text = ReportService.parseXhtml(html).getDocumentElement().getTextContent();
        assertTrue(text.contains("α|∑|©|…"), text);
    }

    @Test
    void pdf_richTextEntitiesRenderAsCharacters() throws Exception {
        Postmortem pm = withHole(postmortem(), "copy &copy; alpha &alpha; sum &sum;", "ok");
        ReportService rs = service("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head>"
                + "<body>{{#layers}}{{#holes}}<p>{{{description}}}</p>{{/holes}}{{/layers}}</body></html>", new ReportResourcePolicy(false, "", ""));
        byte[] pdf = rs.generatePostmortemPdf(pm);
        assertTrue(pdf.length > 0);
        String doc = XHTML_DOCTYPE + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body><p>"
                + ReportService.toPdfRichText("copy &copy; alpha &alpha; sum &sum;") + "</p></body></html>";
        String text = ReportService.parseXhtml(doc).getDocumentElement().getTextContent();
        assertTrue(text.contains("copy © alpha α sum ∑"), text);
    }

    @Test
    void parseXhtml_rejectsMalformedHtml_withIOException() {
        IOException e = assertThrows(IOException.class, () -> ReportService.parseXhtml("<html><body><p></body></html>"));
        assertTrue(e.getMessage().startsWith("Report HTML is not well-formed"));
    }

    // ---- rich text ----

    @Test
    void toPdfRichText_makesSanitizedHtmlXmlSafe() {
        assertEquals("<p>a<br/>b</p><hr/>", ReportService.toPdfRichText("<p>a<br>b</p><hr>"));
        assertEquals("x&#160;y", ReportService.toPdfRichText("x&nbsp;y"));
        assertEquals("a &amp; b", ReportService.toPdfRichText("a &amp; b"));
        // Recognized HTML entities become numeric references (the old expectation "&amp;copy;" printed the name literally)
        assertEquals("&#169;", ReportService.toPdfRichText("&copy;"));
        assertEquals("&#945;&#8721;", ReportService.toPdfRichText("&alpha;&sum;"));
        assertEquals("&amp;unknownname;", ReportService.toPdfRichText("&unknownname;"));
        assertEquals("", ReportService.toPdfRichText(null));
        assertFalse(ReportService.toPdfRichText("<img src=\"http://x/y.png\">a").contains("<img"));
    }

    // ---- context ----

    @Test
    @SuppressWarnings("unchecked")
    void buildReportContext_sanitizesHoleFields_mainAndLinked() {
        Postmortem local = withHole(postmortem(), "<img src=\"http://127.0.0.1/l.png\">local", "<script>x</script>fix");
        local.setIncidentRef("INC");
        Postmortem main = withHole(postmortem(), "<img src=\"http://127.0.0.1/m.png\">main", "ok");
        main.getLocalPostmortemUuids().add(local.getUuid());

        ReportTemplateService templates = mock(ReportTemplateService.class);
        PostmortemService pms = mock(PostmortemService.class);
        when(pms.findVisibleByIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(local));
        when(pms.findMajorPostmortems(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        ReportService rs = new ReportService(Mustache.compiler(), new DefaultResourceLoader(), templates, pms, new ReportResourcePolicy(false, "", ""));

        Map<String, Object> ctx = rs.buildReportContext(main);
        List<Map<String, Object>> layers = (List<Map<String, Object>>) ctx.get("layers");
        List<Map<String, Object>> holes = (List<Map<String, Object>>) layers.get(0).get("holes");
        assertEquals(2, holes.size());
        for (Map<String, Object> h : holes) {
            assertFalse(((String) h.get("description")).contains("<img"));
            assertFalse(((String) h.get("remedialAction")).contains("<script"));
        }
        List<Map<String, Object>> localPms = (List<Map<String, Object>>) ctx.get("localPostmortems");
        List<Map<String, Object>> lLayers = (List<Map<String, Object>>) localPms.get(0).get("layers");
        Map<String, Object> lHole = ((List<Map<String, Object>>) lLayers.get(0).get("holes")).get(0);
        assertEquals("local", lHole.get("description"));
    }

    @Test
    void buildReportContext_krokiOff_hasNoKrokiKeysButHtmlTimeline() {
        Map<String, Object> ctx = service(null, new ReportResourcePolicy(false, "", "")).buildReportContext(withEvent(postmortem()));
        assertFalse(ctx.containsKey("krokiTimelineUrl"));
        assertFalse(ctx.containsKey("mermaidSource"));
        assertEquals(1, ((List<?>) ctx.get("graphicalTimeline")).size());
    }

    @Test
    void buildReportContext_krokiOn_putsSelfHostedUrl() {
        Map<String, Object> ctx = service(null, new ReportResourcePolicy(true, "https://kroki.internal", "")).buildReportContext(withEvent(postmortem()));
        assertTrue(((String) ctx.get("krokiTimelineUrl")).startsWith("https://kroki.internal/mermaid/png/"));
        assertNotNull(ctx.get("mermaidSource"));
    }
}
