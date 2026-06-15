package com.emailservice.backend.smtp;

import com.emailservice.backend.storage.AttachmentStorageService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

@Component
public class SmtpMessageHandlerFactory implements MessageHandlerFactory {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AttachmentStorageService storageService;

    public SmtpMessageHandlerFactory(KafkaTemplate<String, String> kafkaTemplate,
                                      AttachmentStorageService storageService) {
        this.kafkaTemplate = kafkaTemplate;
        this.storageService = storageService;
    }

    @Override
    public MessageHandler create(MessageContext ctx) {
        return new SmtpMessageHandler(kafkaTemplate, storageService);
    }
}
