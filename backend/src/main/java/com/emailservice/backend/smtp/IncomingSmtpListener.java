package com.emailservice.backend.smtp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.server.SMTPServer;

@Component
public class IncomingSmtpListener {
    
    private SMTPServer smtpServer;
    private final SmtpMessageHandlerFactory handlerFactory;

    public IncomingSmtpListener(SmtpMessageHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    @PostConstruct
    public void start() {
        smtpServer = new SMTPServer(handlerFactory);
        smtpServer.setPort(25);
        smtpServer.start();
        System.out.println("✅ SubEthaSMTP Server started and listening on port 25");
    }

    @PreDestroy
    public void stop() {
        if (smtpServer != null) {
            smtpServer.stop();
            System.out.println("🛑 SubEthaSMTP Server stopped");
        }
    }
}
