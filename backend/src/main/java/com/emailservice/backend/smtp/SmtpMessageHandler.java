package com.emailservice.backend.smtp;

import com.emailservice.backend.storage.AttachmentStorageService;
import org.springframework.kafka.core.KafkaTemplate;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;

import javax.mail.BodyPart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class SmtpMessageHandler implements MessageHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AttachmentStorageService storageService;
    private String from;
    private String recipient;

    public SmtpMessageHandler(KafkaTemplate<String, String> kafkaTemplate, AttachmentStorageService storageService) {
        this.kafkaTemplate = kafkaTemplate;
        this.storageService = storageService;
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
            // Parse the raw SMTP data stream into a proper MimeMessage
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session, data);

            // Generate a unique ID for this email (used for attachment storage paths)
            String emailId = UUID.randomUUID().toString().substring(0, 8);

            // Extract subject from MIME headers
            String subject = mimeMessage.getSubject() != null ? mimeMessage.getSubject() : "";

            // Extract body text and attachments by walking the MIME tree
            StringBuilder bodyBuilder = new StringBuilder();
            List<String> attachmentJsonParts = new ArrayList<>();

            Object content = mimeMessage.getContent();
            if (content instanceof MimeMultipart) {
                // Multipart message — may contain text body + attachments
                processMultipart((MimeMultipart) content, bodyBuilder, attachmentJsonParts, emailId);
            } else {
                // Simple text-only message (no attachments)
                bodyBuilder.append(content.toString());
            }

            String body = bodyBuilder.toString().trim();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Build the JSON payload for Kafka
            String safeFrom = escapeJson(this.from);
            String safeTo = escapeJson(this.recipient);
            String safeSubject = escapeJson(subject);
            String safeBody = escapeJson(body);
            String attachmentsJson = "[" + String.join(",", attachmentJsonParts) + "]";

            String jsonPayload = String.format(
                "{\"id\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"subject\":\"%s\",\"body\":\"%s\",\"timestamp\":\"%s\",\"attachments\":%s}",
                emailId, safeFrom, safeTo, safeSubject, safeBody, timestamp, attachmentsJson
            );

            kafkaTemplate.send("incoming-emails", jsonPayload);
            System.out.println("🚀 Email parsed and published to Kafka topic 'incoming-emails'!");
            if (!attachmentJsonParts.isEmpty()) {
                System.out.println("📎 " + attachmentJsonParts.size() + " attachment(s) stored for email " + emailId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RejectException(554, "Internal Server Error processing email data");
        }
    }

    /**
     * Recursively walk a MimeMultipart structure, extracting text bodies and saving attachments.
     */
    private void processMultipart(MimeMultipart multipart, StringBuilder bodyBuilder,
                                   List<String> attachmentJsonParts, String emailId) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String disposition = bodyPart.getDisposition();
            String contentType = bodyPart.getContentType().toLowerCase();

            if (bodyPart.getContent() instanceof MimeMultipart) {
                // Nested multipart (e.g., multipart/alternative inside multipart/mixed)
                processMultipart((MimeMultipart) bodyPart.getContent(), bodyBuilder, attachmentJsonParts, emailId);

            } else if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
                // This part is an attachment
                saveAttachment(bodyPart, attachmentJsonParts, emailId);

            } else if (bodyPart.getFileName() != null) {
                // Has a filename but no explicit disposition — treat as attachment
                saveAttachment(bodyPart, attachmentJsonParts, emailId);

            } else if (contentType.contains("text/plain")) {
                // Plain text body
                bodyBuilder.append(bodyPart.getContent().toString());

            } else if (contentType.contains("text/html") && bodyBuilder.length() == 0) {
                // HTML body (only use if no plain text was found)
                bodyBuilder.append(bodyPart.getContent().toString());
            }
        }
    }

    /**
     * Save a single attachment to disk and add its metadata to the JSON parts list.
     */
    private void saveAttachment(BodyPart bodyPart, List<String> attachmentJsonParts, String emailId) throws Exception {
        String filename = bodyPart.getFileName() != null ? bodyPart.getFileName() : "unnamed_attachment";
        String contentType = bodyPart.getContentType().split(";")[0].trim();

        // Read attachment bytes
        InputStream attachStream = bodyPart.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = attachStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        byte[] fileBytes = buffer.toByteArray();

        // Store to filesystem
        storageService.store(emailId, filename, fileBytes);

        // Build the attachment metadata JSON object
        String attachJson = String.format(
            "{\"id\":\"%s\",\"filename\":\"%s\",\"contentType\":\"%s\",\"size\":%d}",
            emailId, escapeJson(filename), escapeJson(contentType), fileBytes.length
        );
        attachmentJsonParts.add(attachJson);
    }

    /**
     * Escape special characters for safe JSON string embedding.
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t");
    }

    @Override
    public void done() {
        // SMTP transaction complete
    }
}
