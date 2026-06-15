package com.emailservice.backend.controller;

import com.emailservice.backend.storage.EmailEntity;
import com.emailservice.backend.storage.EmailRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailRepository emailRepository;

    public EmailController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @GetMapping
    public List<EmailEntity> getHistoricalEmails() {
        return StreamSupport.stream(emailRepository.findAll().spliterator(), false)
                // Cassandra won't auto-sort unless it's a clustering key, 
                // so we sort in-memory for this simple demo (newest first).
                // Assuming timestamp format allows lexical or standard sorting.
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }
}
