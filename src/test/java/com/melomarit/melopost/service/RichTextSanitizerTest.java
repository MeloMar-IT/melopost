package com.melomarit.melopost.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextSanitizerTest {

    @Test
    void nullAndEmptyGiveEmptyString() {
        assertEquals("", RichTextSanitizer.sanitize(null));
        assertEquals("", RichTextSanitizer.sanitize(""));
    }

    @Test
    void scriptElementIsRemovedWithContent() {
        String out = RichTextSanitizer.sanitize("<script>alert(1)</script>ok");
        assertEquals("ok", out);
    }

    @Test
    void imgWithOnerrorIsRemoved() {
        String out = RichTextSanitizer.sanitize("<img src=x onerror=\"alert(1)\">after");
        assertFalse(out.contains("<img"));
        assertFalse(out.contains("onerror"));
        assertEquals("after", out);
    }

    @Test
    void eventHandlerAttributesAreStripped() {
        assertEquals("<p>t</p>", RichTextSanitizer.sanitize("<p onclick=\"x()\">t</p>"));
    }

    @Test
    void styleAttributeIsStripped() {
        assertEquals("<p>t</p>", RichTextSanitizer.sanitize("<p style=\"background:url(x)\">t</p>"));
    }

    @Test
    void javascriptHrefIsRemovedButTextKept() {
        String out = RichTextSanitizer.sanitize("<a href=\"javascript:alert(1)\">c</a>");
        assertTrue(out.contains("c"));
        assertFalse(out.contains("javascript:"));
        assertFalse(out.contains("href"));
    }

    @Test
    void obfuscatedJavascriptAndDataHrefsAreRemoved() {
        for (String h : new String[]{
                "JaVaScRiPt:alert(1)", "&#106;avascript:alert(1)", "java&#x09;script:alert(1)",
                "javascript&colon;alert(1)", " javascript:alert(1)", "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==", "vbscript:x"}) {
            String out = RichTextSanitizer.sanitize("<a href=\"" + h + "\">c</a>");
            assertFalse(out.contains("href"), "href kept for: " + h + " -> " + out);
        }
    }

    @Test
    void rawLessThanInsideHrefYieldsNoLinkElement() {
        // The tag does not parse (raw '<' in the attribute), so it is escaped to inert text, not a link.
        String out = RichTextSanitizer.sanitize("<a href=\"data:text/html,<b>x</b>\">c</a>");
        assertFalse(out.contains("<a"), out);
    }

    @Test
    void httpLinkKeepsHrefTargetAndGetsRel() {
        String out = RichTextSanitizer.sanitize("<a href=\"https://example.com\" target=\"_blank\">x</a>");
        assertTrue(out.contains("href=\"https://example.com\""));
        assertTrue(out.contains("target=\"_blank\""));
        assertTrue(out.contains("rel=\"noopener noreferrer\""));
        assertTrue(RichTextSanitizer.sanitize("<a href=\"mailto:a@b.c\">m</a>").contains("href=\"mailto:a@b.c\""));
    }

    @Test
    void attributeBreakoutInHrefIsEscaped() {
        String out = RichTextSanitizer.sanitize("<a href='https://e.com/\" onmouseover=\"alert(1)'>x</a>");
        assertFalse(out.contains("\" onmouseover"));
        assertTrue(out.contains("&quot;"));
    }

    @Test
    void allowedFormattingIsUnchanged() {
        String in = "<strong>b</strong><em>i</em><u>u</u><s>s</s><ul><li>a</li></ul><blockquote>q</blockquote>";
        assertEquals(in, RichTextSanitizer.sanitize(in));
    }

    @Test
    void svgAndIframeAreRemoved() {
        String out = RichTextSanitizer.sanitize("<svg onload=alert(1)></svg><iframe src=\"//evil\">x</iframe>y");
        assertEquals("y", out);
    }

    @Test
    void plainLessThanIsEscaped() {
        assertEquals("a &lt; b", RichTextSanitizer.sanitize("a < b"));
    }

    @Test
    void splitTagTrickDoesNotProduceMarkup() {
        String out = RichTextSanitizer.sanitize("<scr<script>ipt>alert(1)</scr</script>ipt>");
        assertFalse(out.contains("<"), out);
    }

    @Test
    void veryLongAttributeDoesNotOverflowStack() {
        String longAttr = "x".repeat(50_000);
        String out = RichTextSanitizer.sanitize("<a title=\"" + longAttr + "\">hello</a>");
        assertEquals("<a>hello</a>", out);
    }

    @Test
    void longQuotedHrefIsStillValidated() {
        String out = RichTextSanitizer.sanitize("<a href=\"javascript:" + "x".repeat(20_000) + "\">c</a>");
        assertFalse(out.contains("href"));
        assertTrue(out.contains("c"));
    }

    @Test
    void malformedMarkupDoesNotThrowOrLeakTags() {
        String[] inputs = {
                "<a href=\"https://example.com\" title=\"unterminated",
                "<b",
                "</b",
                "<<<>>>",
                "<!-- unterminated comment <script>alert(1)</script>",
                "<script>never closed",
                "<style>body{}",
                "<a href=\"https://x.com/a>b\">text</a>",
                "&amp; &#x; &#99999999; &",
                "<p><b>x</p></b>",
        };
        for (String in : inputs) {
            String out = RichTextSanitizer.sanitize(in);
            assertFalse(out.toLowerCase().contains("<script"), in + " -> " + out);
            assertFalse(out.toLowerCase().contains("<style"), in + " -> " + out);
            assertFalse(out.toLowerCase().contains("onerror"), in + " -> " + out);
            assertFalse(out.toLowerCase().contains("javascript:"), in + " -> " + out);
        }
    }

    @Test
    void unclosedTagsAreClosed() {
        assertEquals("<b>x</b>", RichTextSanitizer.sanitize("<b>x"));
    }
}
