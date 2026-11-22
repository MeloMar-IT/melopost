package com.melomarit.melopost.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Optional local LLM helper (llm_api.py). Only created when
 * {@code melopost.llm-api.enabled=true}. It listens on 127.0.0.1 only and requires a
 * shared secret, which is passed to the child process through its environment.
 */
@Service
@ConditionalOnProperty(name = "melopost.llm-api.enabled", havingValue = "true")
public class LlmApiService {

    private static final Logger logger = LoggerFactory.getLogger(LlmApiService.class);

    static final String TOKEN_ENV = "LLM_API_TOKEN";
    static final String PORT_ENV = "LLM_API_PORT";
    static final String CONFIG_TOKEN_ENV = "MELOPOST_LLM_API_TOKEN";
    static final int MIN_TOKEN_LENGTH = 32;
    static final String HOST = "127.0.0.1";

    private final String pythonExecutable;
    private final String scriptPath;
    private final int port;
    private final String apiToken;

    private volatile Process process;

    /** The configured token is read only from the OS environment, never from Spring properties. */
    @Autowired
    public LlmApiService(
            @Value("${melopost.llm-api.python-executable:python3}") String pythonExecutable,
            @Value("${melopost.llm-api.script-path:llm_api.py}") String scriptPath,
            @Value("${melopost.llm-api.port:8000}") int port) {
        this(pythonExecutable, scriptPath, port, System.getenv(CONFIG_TOKEN_ENV));
    }

    LlmApiService(String pythonExecutable, String scriptPath, int port, String suppliedToken) {
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("melopost.llm-api.port must be between 1 and 65535");
        }
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.port = port;
        if (suppliedToken == null || suppliedToken.isBlank()) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            this.apiToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } else if (suppliedToken.length() < MIN_TOKEN_LENGTH) {
            throw new IllegalStateException(CONFIG_TOKEN_ENV + " must be at least " + MIN_TOKEN_LENGTH + " characters");
        } else {
            this.apiToken = suppliedToken;
        }
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getBaseUrl() {
        return "http://" + HOST + ":" + port;
    }

    ProcessBuilder createProcessBuilder() {
        ProcessBuilder pb = new ProcessBuilder(List.of(pythonExecutable, scriptPath));
        pb.redirectErrorStream(true);
        pb.environment().put(TOKEN_ENV, apiToken);
        pb.environment().put(PORT_ENV, String.valueOf(port));
        return pb;
    }

    @PostConstruct
    public void startLlmApi() {
        logger.info("Starting LLM API using {} and script {} on port {}", pythonExecutable, scriptPath, port);
        try {
            final Process started = createProcessBuilder().start();
            process = started;

            Thread reader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        logger.info("[LLM API] {}", line);
                    }
                } catch (IOException e) {
                    logger.error("Error reading LLM API output", e);
                }
            }, "llm-api-log");
            reader.setDaemon(true);
            reader.start();

            logger.info("LLM API process started successfully.");
        } catch (IOException e) {
            logger.error("Failed to start LLM API process", e);
        }
    }

    @PreDestroy
    public void stopLlmApi() {
        Process p = process;
        if (p != null && p.isAlive()) {
            logger.info("Stopping LLM API process...");
            p.destroy();
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("LLM API process did not exit gracefully, forcing termination...");
                    p.destroyForcibly();
                }
                logger.info("LLM API process stopped.");
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for LLM API process to stop", e);
                p.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
