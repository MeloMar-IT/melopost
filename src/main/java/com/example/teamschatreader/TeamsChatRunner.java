package com.example.teamschatreader;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.graph.models.ChatMessage;
import com.microsoft.graph.models.ChatMessageCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamsChatRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(TeamsChatRunner.class);

    @Value("${teams.chat.id:19:68d094f7fa904da097d8650bc7cfa081@thread.v2}")
    private String chatId;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Teams Chat Reader...");
        logger.info("Target Chat ID: {}", chatId);

        try {
            // Using DefaultAzureCredential which looks for environment variables:
            // AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID
            final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();

            // Define the scopes required for reading chat messages
            // For Service Principal authentication (Client Credentials flow), 
            // the scope must be "https://graph.microsoft.com/.default".
            // The actual permissions (like Chat.Read.All) MUST be granted in the Azure Portal
            // as "Application" permissions and admin-consented.
            final String[] scopes = new String[] {"https://graph.microsoft.com/.default"};

            final GraphServiceClient graphClient = new GraphServiceClient(credential, scopes);

            logger.info("Fetching messages from chat...");

            ChatMessageCollectionResponse messagesResponse = graphClient.chats().byChatId(chatId).messages().get();
            
            List<ChatMessage> messages = messagesResponse.getValue();

            if (messages != null && !messages.isEmpty()) {
                logger.info("Retrieved {} messages:", messages.size());
                System.out.println("--- Teams Chat Content ---");
                for (ChatMessage message : messages) {
                    String from = (message.getFrom() != null && message.getFrom().getUser() != null) 
                                  ? message.getFrom().getUser().getDisplayName() 
                                  : "System/Unknown";
                    String content = (message.getBody() != null) ? (message.getBody().getContent() != null ? message.getBody().getContent() : "") : "";
                    System.out.printf("[%s] %s: %s%n", message.getCreatedDateTime(), from, content);
                }
                System.out.println("--- End of Teams Chat Content ---");
            } else {
                logger.warn("No messages found or access denied.");
            }

        } catch (Exception e) {
            logger.error("Error fetching Teams chat: {}", e.getMessage());
            System.err.println("--- Teams Chat Fetch Error ---");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Ensure AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, and AZURE_TENANT_ID environment variables are set correctly.");
            System.err.println("Make sure the Azure App Registration has the required API Permissions:");
            System.err.println("- Application Permissions: 'Chat.Read.All' (Recommended for Service Principals)");
            System.err.println("- Delegated Permissions: 'Chat.Read' (If using user-based authentication)");
            System.err.println("IMPORTANT: Ensure you have clicked 'Grant admin consent' in the Azure Portal after adding permissions.");
            System.err.println("Also verify that the Chat ID is correct and accessible by the authenticated user/service principal.");
            System.err.println("--- End of Teams Chat Fetch Error ---");
        }
    }
}
