package com.emailservice.backend.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AttachmentStorageService {

    private final Path rootLocation = Paths.get("/app/attachments");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            System.out.println("📁 Attachment storage initialized at: " + rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize attachment storage directory", e);
        }
    }

    /**
     * Stores an attachment file on disk under /app/attachments/{emailId}/{filename}
     */
    public void store(String emailId, String filename, byte[] data) throws IOException {
        Path emailDir = rootLocation.resolve(emailId);
        Files.createDirectories(emailDir);
        Path filePath = emailDir.resolve(filename);
        Files.write(filePath, data);
        System.out.println("💾 Stored attachment: " + filePath);
    }

    /**
     * Loads an attachment file as a Spring Resource for download
     */
    public Resource load(String emailId, String filename) throws MalformedURLException {
        Path filePath = rootLocation.resolve(emailId).resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        return null;
    }
}
