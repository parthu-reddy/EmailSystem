package com.emailservice.backend.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    
    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "incoming-emails", groupId = "email-consumer-group")
    public void consume(String message) {
        System.out.println("📬 Consumed from Kafka: " + message);
        // Push the JSON string directly to the WebSocket topic
        messagingTemplate.convertAndSend("/topic/emails", message);
    }
}
