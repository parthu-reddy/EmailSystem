package com.emailservice.backend.storage;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends CassandraRepository<EmailEntity, String> {
}
