package com.melomarit.melopost.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides which external resources the PDF renderer may load and holds the Kroki settings.
 * Only {@code data:} URIs and absolute http(s) URIs whose host is allow-listed are loaded.
 */
@Component
public class ReportResourcePolicy {

    private static final Logger log = LoggerFactory.getLogger(ReportResourcePolicy.class);

    private final boolean krokiEnabled;
    private final String krokiBaseUrl;
    private final Set<String> allowedHosts;

    public ReportResourcePolicy(@Value("${melopost.report.kroki.enabled:false}") boolean krokiEnabled,
                                @Value("${melopost.report.kroki.base-url:}") String krokiBaseUrl,
                                @Value("${melopost.report.allowed-resource-hosts:}") String allowedHosts) {
        Set<String> hosts = allowedHosts == null ? Set.of()
                : Arrays.stream(allowedHosts.split(","))
                .map(s -> s.strip().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        String base = null;
        if (krokiEnabled) {
            base = validBaseUrl(krokiBaseUrl);
            if (base == null) {
                log.warn("melopost.report.kroki.enabled is true but melopost.report.kroki.base-url is not a valid http(s) URL; Kroki stays off");
            } else {
                hosts.add(URI.create(base).getHost().toLowerCase(Locale.ROOT));
            }
        }
        this.krokiBaseUrl = base;
        this.krokiEnabled = base != null;
        this.allowedHosts = Set.copyOf(hosts);
    }

    private static String validBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.strip());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                return null;
            }
            String s = uri.toString();
            return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public boolean isKrokiEnabled() {
        return krokiEnabled;
    }

    public Optional<String> krokiTimelineUrl(String encodedSource) {
        if (!krokiEnabled || encodedSource == null) {
            return Optional.empty();
        }
        return Optional.of(krokiBaseUrl + "/mermaid/png/" + encodedSource);
    }

    Set<String> allowedHosts() {
        return allowedHosts;
    }

    /** True when the URI may be loaded (same decision as {@link #resolveUri}). */
    public boolean isAllowed(String uri) {
        return resolveUri(null, uri) != null;
    }

    /** FSUriResolver contract: the URI to load, or null to refuse. */
    public String resolveUri(String baseUri, String uri) {
        if (uri != null && !uri.isBlank()) {
            try {
                URI parsed = new URI(uri.strip());
                String scheme = parsed.getScheme();
                if (scheme != null) {
                    if (scheme.equalsIgnoreCase("data")) {
                        return uri;
                    }
                    if ((scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                            && parsed.getHost() != null
                            && allowedHosts.contains(parsed.getHost().toLowerCase(Locale.ROOT))) {
                        return uri.strip();
                    }
                }
            } catch (URISyntaxException e) {
                // refused below
            }
        }
        log.warn("Refused report resource: {}", abbreviate(uri));
        return null;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        String clean = s.replaceAll("[\\r\\n]", " ");
        return clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
    }
}
