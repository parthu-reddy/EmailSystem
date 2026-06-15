package com.emailservice.backend.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.emailservice.backend.storage.EmailEntity;
import com.emailservice.backend.storage.EmailRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class KafkaConsumerService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailRepository emailRepository;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate, 
                                EmailRepository emailRepository,
                                ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.emailRepository = emailRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "incoming-emails", groupId = "email-consumer-group")
    public void consume(String message) {
        System.out.println("📬 Consumed from Kafka: " + message);
        
        try {
            JsonNode root = objectMapper.readTree(message);
            
            EmailEntity entity = new EmailEntity();
            entity.setId(root.has("id") ? root.get("id").asText() : UUID.randomUUID().toString());
            entity.setSender(root.has("from") ? root.get("from").asText() : "");
            entity.setRecipient(root.has("to") ? root.get("to").asText() : "");
            entity.setSubject(root.has("subject") ? root.get("subject").asText() : "");
            entity.setBody(root.has("body") ? root.get("body").asText() : "");
            
            if (root.has("timestamp")) {
                entity.setTimestamp(root.get("timestamp").asText());
            } else {
                entity.setTimestamp(java.time.LocalTime.now().toString());
            }
            
            if (root.has("attachments")) {
                entity.setAttachmentsJson(root.get("attachments").toString());
            } else {
                entity.setAttachmentsJson("[]");
            }
            
            emailRepository.save(entity);
            System.out.println("💾 Saved email to Cassandra: " + entity.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to save email to Cassandra: " + e.getMessage());
        }

        // Push the JSON string directly to the WebSocket topic
        messagingTemplate.convertAndSend("/topic/emails", message);
    }
}
