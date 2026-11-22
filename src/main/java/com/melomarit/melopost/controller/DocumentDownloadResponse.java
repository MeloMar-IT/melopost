package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.service.DocumentTypePolicy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Builds safe download responses for stored documents. */
final class DocumentDownloadResponse {
    private DocumentDownloadResponse() {
    }

    /**
     * RFC 6266 header: a quoted ASCII fallback (quotes and backslashes escaped, other characters
     * replaced) plus an RFC 5987 UTF-8 {@code filename*} carrying the exact name.
     */
    static String contentDisposition(String storedName) {
        String name = DocumentTypePolicy.downloadFileName(storedName);
        StringBuilder fallback = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == '"' || c == '\\') {
                fallback.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                fallback.append('_');
            } else {
                fallback.append(c);
            }
        }
        StringBuilder encoded = new StringBuilder();
        for (byte b : name.getBytes(StandardCharsets.UTF_8)) {
            int v = b & 0xff;
            if ((v >= 'a' && v <= 'z') || (v >= 'A' && v <= 'Z') || (v >= '0' && v <= '9')
                    || "!#$&+-.^_`|~".indexOf(v) >= 0) {
                encoded.append((char) v);
            } else {
                encoded.append('%').append(String.format("%02X", v));
            }
        }
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }

    static ResponseEntity<byte[]> of(PostmortemDocument doc) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(doc.getFileName()))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(DocumentTypePolicy.downloadMediaType(doc.getContentType()))
                .body(doc.getData());
    }
}
