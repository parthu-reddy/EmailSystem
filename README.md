# Distributed Email Service

This project is a highly scalable, distributed email service engineered to rival enterprise platforms like Gmail or Outlook. It leverages an event-driven microservices architecture, highly optimized polyglot persistence, and real-time bidirectional communication channels to deliver robust performance under extreme scale.

## Architecture Overview

The system explicitly decouples the mechanisms of receiving, processing, storing, and delivering messages. Rather than relying on legacy monolithic IMAP/POP3 structures, this architecture utilizes RESTful HTTP/HTTPS APIs and WebSockets for client-server communication, reserving SMTP strictly for external server-to-server email exchange.

### High-Level Design (HLD)
- **API Gateway & Load Balancing**: Client traffic passes through a Layer 7 load balancer into an API Gateway. The gateway handles rate limiting, JWT authentication, and request routing to downstream microservices.
- **SMTP Reception Microservice**: Operates on TCP port 25 to accept incoming external SMTP connections, parses MIME content, and immediately offloads payloads to a distributed queue, preventing synchronous bottlenecks.
- **Apache Kafka**: The central distributed message broker (the nervous system). The SMTP receiver acts as a Kafka Producer, publishing `EmailReceivedEvent` payloads to partitioned topics.
- **Consumer Microservices**: Asynchronous consumers read from Kafka to execute spam detection, virus scanning, and rule-based filtering before dispatching data to the storage layer.
- **Real-Time Push Notification Service**: Manages persistent WebSocket connections with active users using STOMP. When an email is persisted, an event triggers a real-time push notification to the recipient.
- **Outbound Email Flow**: Users send emails via RESTful POST requests. The message is persisted, and an `EmailDispatchEvent` is pushed to Kafka. A dedicated outbound SMTP agent consumes this event and transmits the email over standard SMTP.

### Low-Level Design (LLD) & Polyglot Persistence
- **Apache Cassandra**: A wide-column NoSQL database utilizing Log-Structured Merge-Tree (LSM) architecture. It manages the core email metadata, folder structures, and temporal queries with extreme write throughput.
- **Elasticsearch**: Powers full-text search capabilities across email subjects and bodies. It is updated asynchronously via a Kafka consumer.
- **Amazon S3 / MinIO**: Distributed object storage used to store raw MIME payloads and massive binary attachments, avoiding I/O bottlenecks in the metadata database.

## Screenshots

### Empty Inbox
The landing state when no emails have been received yet. Shows the three-panel layout: sidebar navigation, email list, and reading pane with live WebSocket connection status.

![Empty Inbox](screenshots/inbox_empty.png)

### Inbox with Emails
Incoming emails appear in real-time via WebSocket/STOMP as they are consumed from Kafka. Unread emails are visually distinguished with bold styling, and the sidebar badge reflects the unread count.

![Inbox with Emails](screenshots/inbox_with_emails.png)

### Email Detail View
Clicking an email reveals the full message in the reading pane — including subject, sender avatar, recipient, timestamp, and body. The email is automatically marked as read.

![Email Detail View](screenshots/email_detail.png)

## Technical Stack

### Backend
- **Java 17 & Spring Boot**: Robust framework for event-driven microservices.
- **SubEthaSMTP**: Embedded SMTP server to intercept raw SMTP conversations.
- **Spring Kafka**: For producing and consuming events.
- **Spring Data Cassandra**: Interface-driven database access.
- **Spring WebSocket (STOMP)**: Real-time UI updates.

### Frontend
- **Vue.js 3**: Modern, highly reactive Single Page Application (SPA) framework.
- **Pinia**: State management library.
- **SockJS & Webstomp-Client**: WebSocket communication and fallback mechanisms.
- **Vite & Nginx**: Optimized frontend building and serving.

### Infrastructure
- **Apache Kafka & Zookeeper**: Distributed streaming and coordination.
- **Apache Cassandra**: NoSQL metadata database.
- **Elasticsearch**: Search indexing engine.

## Deployment Strategy

The orchestration of this distributed infrastructure relies on Docker Compose for local/staging environments. In production, stateless components should scale on Kubernetes, while stateful data clusters should ideally be offloaded to managed cloud services (e.g., Confluent Cloud, DataStax Astra, Elastic Cloud).

### Local Deployment via Docker Compose
Ensure you have Docker and Docker Compose installed.

1. Navigate to the project root directory.
2. Execute the orchestration command:

```bash
docker-compose up -d --build
```

This single command will orchestrate:
1. Zookeeper (Port 2181)
2. Apache Kafka (Port 9092)
3. Apache Cassandra (Port 9042)
4. Elasticsearch (Port 9200)
5. Spring Boot Backend (Ports 8080 & 25)
6. Vue.js Frontend (Port 80)
