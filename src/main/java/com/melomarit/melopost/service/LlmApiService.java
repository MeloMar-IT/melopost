package com.melomarit.melopost.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class LlmApiService {

    private static final Logger logger = LoggerFactory.getLogger(LlmApiService.class);

    @Value("${melopost.llm-api.enabled:true}")
    private boolean enabled;

    @Value("${melopost.llm-api.python-executable:python3}")
    private String pythonExecutable;

    @Value("${melopost.llm-api.script-path:llm_api.py}")
    private String scriptPath;

    private Process process;

    @PostConstruct
    public void startLlmApi() {
        if (!enabled) {
            logger.info("LLM API is disabled by configuration.");
            return;
        }

        logger.info("Starting LLM API using {} and script {}", pythonExecutable, scriptPath);

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            process = processBuilder.start();
            
            // Log output in a separate thread to avoid blocking
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[LLM API] {}", line);
                    }
                } catch (IOException e) {
                    logger.error("Error reading LLM API output", e);
                }
            }).start();

            logger.info("LLM API process started successfully.");
        } catch (IOException e) {
            logger.error("Failed to start LLM API process", e);
        }
    }

    @PreDestroy
    public void stopLlmApi() {
        if (process != null && process.isAlive()) {
            logger.info("Stopping LLM API process...");
            process.destroy();
            try {
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("LLM API process did not exit gracefully, forcing termination...");
                    process.destroyForcibly();
                }
                logger.info("LLM API process stopped.");
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for LLM API process to stop", e);
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
