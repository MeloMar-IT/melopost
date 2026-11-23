package com.melomarit.melopost.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTypePolicyTest {

    @Test
    void contentTypeForFileName_knownAndUnknown() {
        assertEquals("application/pdf", DocumentTypePolicy.contentTypeForFileName("a.PDF").orElseThrow());
        assertEquals("image/jpeg", DocumentTypePolicy.contentTypeForFileName("x.jpeg").orElseThrow());
        assertEquals("image/jpeg", DocumentTypePolicy.contentTypeForFileName("x.jpg").orElseThrow());
        assertEquals("text/csv", DocumentTypePolicy.contentTypeForFileName("data.csv").orElseThrow());
        for (String n : new String[]{"archive.tar.gz", "page.html", "noext", ".", "", null}) {
            assertTrue(DocumentTypePolicy.contentTypeForFileName(n).isEmpty(), String.valueOf(n));
        }
    }

    @Test
    void requireAllowed_refusesBadExtensionOrDeclaredType() {
        UnsupportedDocumentTypeException e = assertThrows(UnsupportedDocumentTypeException.class,
                () -> DocumentTypePolicy.requireAllowed("evil.html", "text/html"));
        assertEquals(400, e.getStatusCode().value());
        assertThrows(UnsupportedDocumentTypeException.class,
                () -> DocumentTypePolicy.requireAllowed("x.png", "text/html"));
        assertThrows(UnsupportedDocumentTypeException.class,
                () -> DocumentTypePolicy.requireAllowed("x.png", "not a type"));
        assertThrows(UnsupportedDocumentTypeException.class,
                () -> DocumentTypePolicy.requireAllowed(null, "image/png"));
    }

    @Test
    void requireAllowed_acceptsCanonicalAliasAndMissingTypes() {
        assertEquals("image/png", DocumentTypePolicy.requireAllowed("x.png", "image/png"));
        assertEquals("image/png", DocumentTypePolicy.requireAllowed("x.png", null));
        assertEquals("image/png", DocumentTypePolicy.requireAllowed("x.png", "application/octet-stream"));
        assertEquals("text/csv", DocumentTypePolicy.requireAllowed("data.csv", "application/vnd.ms-excel"));
        assertEquals("text/plain", DocumentTypePolicy.requireAllowed("a.txt", "text/plain;charset=UTF-8"));
    }

    @Test
    void downloadMediaType_fallsBackForUnsafeValues() {
        for (String t : new String[]{null, "", "garbage", "*/*", "text/*", "text/html"}) {
            assertEquals(MediaType.APPLICATION_OCTET_STREAM, DocumentTypePolicy.downloadMediaType(t), String.valueOf(t));
        }
        assertEquals(MediaType.APPLICATION_PDF, DocumentTypePolicy.downloadMediaType("application/pdf"));
        assertNotNull(DocumentTypePolicy.downloadMediaType("text/plain;charset=UTF-8").getCharset());
    }

    @Test
    void cleanFileName_andDownloadFileName() {
        assertEquals("a.pdf", DocumentTypePolicy.cleanFileName("C:\\fakepath\\a.pdf"));
        assertEquals("x.png", DocumentTypePolicy.cleanFileName("../../x.png"));
        assertNull(DocumentTypePolicy.cleanFileName("  "));
        assertEquals("document", DocumentTypePolicy.downloadFileName(null));
        assertEquals("a.pdf", DocumentTypePolicy.downloadFileName("a.pdf"));
    }
}
