package com.melomarit.melopost.service;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Opens http(s) resources for the PDF renderer without following redirects, and only for
 * URLs the {@link ReportResourcePolicy} allows. Any refused, redirected or failing load
 * yields an empty stream.
 */
final class NoRedirectStreamFactory implements FSStreamFactory {

    private static final Logger log = LoggerFactory.getLogger(NoRedirectStreamFactory.class);
    private static final int TIMEOUT_MS = 10_000;

    private final ReportResourcePolicy policy;

    NoRedirectStreamFactory(ReportResourcePolicy policy) {
        this.policy = policy;
    }

    @Override
    public FSStream getUrl(String url) {
        return new FSStream() {
            @Override
            public InputStream getStream() {
                return open(url);
            }

            @Override
            public Reader getReader() {
                return new InputStreamReader(open(url), StandardCharsets.UTF_8);
            }
        };
    }

    private InputStream open(String url) {
        if (!policy.isAllowed(url)) {
            return empty();
        }
        HttpURLConnection conn = null;
        try {
            URL u = URI.create(url.strip()).toURL();
            conn = (HttpURLConnection) u.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("Report resource {} answered HTTP {}; not loaded (redirects are not followed)", url, status);
                conn.disconnect();
                return empty();
            }
            byte[] body;
            try (InputStream in = conn.getInputStream()) {
                body = in.readAllBytes();
            }
            return new ByteArrayInputStream(body);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to load report resource {}: {}", url, e.toString());
            return empty();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static InputStream empty() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
