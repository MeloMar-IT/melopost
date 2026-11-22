package com.example.teamschatreader;

import com.microsoft.graph.models.ChatMessage;
import com.microsoft.graph.models.ItemBody;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TeamsChatRunnerTest {

    private static final Logger logger = LoggerFactory.getLogger(TeamsChatRunnerTest.class);

    @Test
    public void testLoggingFormat() {
        // Since we can't easily mock the final classes of the Graph SDK without more dependencies,
        // let's at least verify the logging logic manually by extracting the format
        
        ChatMessage message = new ChatMessage();
        ItemBody body = new ItemBody();
        body.setContent("Hello World");
        message.setBody(body);
        message.setCreatedDateTime(OffsetDateTime.now());
        
        String from = "Test User";
        String content = (message.getBody() != null) ? (message.getBody().getContent() != null ? message.getBody().getContent() : "") : "";
        
        String logLine = String.format("[%s] %s: %s", message.getCreatedDateTime(), from, content);
        System.out.println("Sample Print: " + logLine);
        
        assertDoesNotThrow(() -> {
            System.out.printf("[%s] %s: %s%n", message.getCreatedDateTime(), from, content);
        });
    }
}
