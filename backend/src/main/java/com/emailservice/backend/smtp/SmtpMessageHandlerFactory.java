package com.emailservice.backend.smtp;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

@Component
public class SmtpMessageHandlerFactory implements MessageHandlerFactory {
    
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SmtpMessageHandlerFactory(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public MessageHandler create(MessageContext ctx) {
        return new SmtpMessageHandler(kafkaTemplate);
    }
}
