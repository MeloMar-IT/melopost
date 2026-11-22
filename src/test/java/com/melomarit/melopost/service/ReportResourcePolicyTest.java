package com.melomarit.melopost.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReportResourcePolicyTest {

    private final ReportResourcePolicy defaults = new ReportResourcePolicy(false, "", "");

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd", "FILE:///etc/passwd", "http://127.0.0.1/x",
            "http://169.254.169.254/latest/meta-data/", "https://kroki.io/mermaid/png/abc",
            "jar:file:/x.jar!/a", "ftp://host/a", "classpath:x", "logo.png", "http://[::1", ""})
    void defaultPolicyRefuses(String uri) {
        assertNull(defaults.resolveUri("", uri));
    }

    @Test
    void refusesNull() {
        assertNull(defaults.resolveUri(null, null));
    }

    @Test
    void allowsDataUri() {
        String data = "data:image/png;base64,iVBORw0KGgo=";
        assertEquals(data, defaults.resolveUri("", data));
    }

    @Test
    void allowListMatchesHostCaseInsensitivelyAnyPort() {
        ReportResourcePolicy p = new ReportResourcePolicy(false, "", " Reports.Example , other.example");
        assertEquals("https://reports.example/logo.png", p.resolveUri("", "https://reports.example/logo.png"));
        assertNotNull(p.resolveUri("", "http://REPORTS.example:8443/x.css"));
        assertNull(p.resolveUri("", "http://reports.example@127.0.0.1/"));
        assertNull(p.resolveUri("", "http://reports.example.evil.com/"));
        assertNull(p.resolveUri("", "file://reports.example/etc/passwd"));
    }

    @Test
    void krokiIsOffByDefault() {
        assertFalse(defaults.isKrokiEnabled());
        assertTrue(defaults.krokiTimelineUrl("abc").isEmpty());
    }

    @Test
    void krokiOnBuildsUrlAndAllowsItsHost() {
        ReportResourcePolicy p = new ReportResourcePolicy(true, "https://kroki.internal/", "");
        assertTrue(p.isKrokiEnabled());
        assertEquals("https://kroki.internal/mermaid/png/abc", p.krokiTimelineUrl("abc").orElseThrow());
        assertNotNull(p.resolveUri("", "https://kroki.internal/mermaid/png/abc"));
        assertTrue(p.allowedHosts().contains("kroki.internal"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "file:///x", "not a url"})
    void krokiWithInvalidBaseUrlCountsAsOff(String base) {
        ReportResourcePolicy p = new ReportResourcePolicy(true, base, "");
        assertFalse(p.isKrokiEnabled());
        assertTrue(p.krokiTimelineUrl("abc").isEmpty());
        assertTrue(p.allowedHosts().isEmpty());
    }
}
