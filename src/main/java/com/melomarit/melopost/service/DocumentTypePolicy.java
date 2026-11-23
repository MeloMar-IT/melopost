package com.melomarit.melopost.service;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Allow-list for uploaded documents and safe media-type resolution for downloads. */
public final class DocumentTypePolicy {
    /** Media type used for downloads whose stored type is missing, malformed or not on the allow-list. */
    public static final String FALLBACK_TYPE = "application/octet-stream";
    /** Value for the {@code accept} attribute of the upload inputs; matches the allow-list. */
    public static final String ACCEPT_ATTRIBUTE = ".pdf,.docx,.xlsx,.png,.jpg,.jpeg,.txt,.csv";

    private static final Map<String, String> TYPES_BY_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "txt", "text/plain",
            "csv", "text/csv");

    private static final Set<String> CANONICAL_TYPES = Set.copyOf(TYPES_BY_EXTENSION.values());

    private static final Map<String, Set<String>> ALIASES_BY_EXTENSION = Map.of(
            "csv", Set.of("application/csv", "application/vnd.ms-excel", "text/comma-separated-values"),
            "jpg", Set.of("image/jpg", "image/pjpeg"),
            "jpeg", Set.of("image/jpg", "image/pjpeg"));

    private DocumentTypePolicy() {
    }

    /** Canonical content type for a file name's extension, or empty if the extension is not allowed. */
    public static Optional<String> contentTypeForFileName(String fileName) {
        return extension(fileName).map(TYPES_BY_EXTENSION::get);
    }

    private static Optional<String> extension(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return Optional.empty();
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return TYPES_BY_EXTENSION.containsKey(ext) ? Optional.of(ext) : Optional.empty();
    }

    /** Returns the canonical type for the file, or throws if the extension or declared type is not allowed. */
    public static String requireAllowed(String fileName, String declaredType) {
        String ext = extension(fileName).orElseThrow(UnsupportedDocumentTypeException::new);
        String canonical = TYPES_BY_EXTENSION.get(ext);
        if (declaredType != null && !declaredType.isBlank()) {
            String declared;
            try {
                MediaType parsed = MediaType.parseMediaType(declaredType);
                declared = (parsed.getType() + "/" + parsed.getSubtype()).toLowerCase(Locale.ROOT);
            } catch (InvalidMediaTypeException e) {
                throw new UnsupportedDocumentTypeException();
            }
            boolean ok = declared.equals(FALLBACK_TYPE)
                    || declared.equals(canonical)
                    || ALIASES_BY_EXTENSION.getOrDefault(ext, Set.of()).contains(declared);
            if (!ok) {
                throw new UnsupportedDocumentTypeException();
            }
        }
        return canonical;
    }

    /** Media type for a download: the stored type if it is a known allowed type, otherwise octet-stream. */
    public static MediaType downloadMediaType(String storedType) {
        if (storedType == null || storedType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            MediaType parsed = MediaType.parseMediaType(storedType);
            String base = (parsed.getType() + "/" + parsed.getSubtype()).toLowerCase(Locale.ROOT);
            if (parsed.isWildcardType() || parsed.isWildcardSubtype() || !CANONICAL_TYPES.contains(base)) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return parsed;
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /** Last path segment of the client-supplied name, trimmed; null if blank. */
    public static String cleanFileName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.replace('\\', '/');
        String last = StringUtils.getFilename(StringUtils.cleanPath(normalized));
        if (last == null || last.isBlank()) {
            return null;
        }
        return last.trim();
    }

    /** Stored name to send back in a download; {@code "document"} if the stored name is null or blank. */
    public static String downloadFileName(String stored) {
        return stored == null || stored.isBlank() ? "document" : stored;
    }
}
