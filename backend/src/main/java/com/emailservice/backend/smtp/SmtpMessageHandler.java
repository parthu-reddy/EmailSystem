package com.emailservice.backend.smtp;

import org.springframework.kafka.core.KafkaTemplate;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class SmtpMessageHandler implements MessageHandler {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private String from;
    private String recipient;

    public SmtpMessageHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void from(String from) throws RejectException {
        this.from = from;
        System.out.println("SMTP Client stated MAIL FROM: " + from);
    }

    @Override
    public void recipient(String recipient) throws RejectException {
        this.recipient = recipient;
        System.out.println("SMTP Client stated RCPT TO: " + recipient);
    }

    @Override
    public void data(InputStream data) throws RejectException {
        try {
            String rawEmailData = new BufferedReader(new InputStreamReader(data))
                    .lines().collect(Collectors.joining("\n"));
            
            // Minimal escaping for a basic JSON payload to Kafka
            String safeBody = rawEmailData.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            
            String jsonPayload = String.format("{\"from\":\"%s\", \"to\":\"%s\", \"body\":\"%s\"}", 
                this.from, this.recipient, safeBody);
                
            kafkaTemplate.send("incoming-emails", jsonPayload);
            System.out.println("🚀 Email parsed and published to Kafka topic 'incoming-emails'!");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RejectException(554, "Internal Server Error processing email data");
        }
    }

    @Override
    public void done() {
        // Transaction complete
    }
}
