package com.melomarit.melopost.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static smoke checks that the XSS fixes in the templates stay in place. */
class TemplateXssGuardTest {

    private static String template(String path) throws IOException {
        try (var in = new ClassPathResource("templates/" + path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void markdownItDoesNotPassRawHtml() throws IOException {
        String layout = template("fragments/layout.html");
        assertTrue(layout.contains("html: false"));
        assertFalse(layout.contains("html: true"));
    }

    @Test
    void everyUtextOfHoleTextGoesThroughSanitizer() throws IOException {
        Pattern utext = Pattern.compile("th:utext=\"([^\"]*)\"");
        for (String path : new String[]{"postmortems/details.html", "holes/list.html", "stories/list.html"}) {
            Matcher m = utext.matcher(template(path));
            int count = 0;
            while (m.find()) {
                count++;
                assertTrue(m.group(1).contains("richText.sanitize("), path + ": " + m.group(1));
            }
            assertTrue(count > 0, path + " has no th:utext");
        }
    }

    @Test
    void reportTemplateHasNoHardcodedKrokiAndOnlyHoleFieldsAreUnescaped() throws IOException {
        String report = template("reports/postmortem-report.mustache");
        assertFalse(report.contains("kroki.io"));
        Matcher m = Pattern.compile("\\{\\{\\{(\\w+)\\}\\}\\}").matcher(report);
        int count = 0;
        while (m.find()) {
            count++;
            assertTrue(m.group(1).equals("description") || m.group(1).equals("remedialAction"), m.group());
        }
        assertTrue(count > 0);
    }

    @Test
    void databasePageHasNoInlineNameHandlersOrHtmlCells() throws IOException {
        String db = template("admin/database.html");
        assertFalse(db.contains("onclick=\"selectTable("));
        assertFalse(db.contains("showTableInfo('${"));
        assertFalse(Pattern.compile("innerHTML\\s*=[^;]*\\$\\{(cell|col|row)\\b").matcher(db).find());
        assertTrue(db.contains("function escapeHtml"));
    }

    @Test
    void mermaidFallbackDoesNotUseInnerHtmlForErrorOrSource() throws IOException {
        String details = template("postmortems/details.html");
        assertFalse(Pattern.compile("innerHTML\\s*=[^;]*(err\\.message|mermaidSource)").matcher(details).find());
    }
}
