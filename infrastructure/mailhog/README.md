# MailHog Infrastructure

MailHog is an email testing tool that captures emails sent by the application during development. It provides a web UI to view, search, and release emails.

## Overview

| Property | Value |
|----------|-------|
| **Image** | `mailhog/mailhog:latest` |
| **Ports** | 1025 (SMTP), 8025 (Web UI) |
| **Container** | `erpai-mailhog` |
| **Purpose** | Email testing and development |
| **Owner** | Platform Team |

## Features

- **SMTP Server:** Captures all outgoing emails
- **Web UI:** View and search captured emails
- **API:** Programmatic access to emails
- **Release:** Forward emails to real recipients
- **Spam Filtering:** Basic spam detection

## Configuration

### Environment Variables

MailHog doesn't require configuration for basic usage. It starts with sensible defaults.

### SMTP Configuration for Spring Boot

```yaml
spring:
  mail:
    host: mailhog
    port: 1025
    username: test
    password: test
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

## Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                        MailHog                                  │
│                                                                 │
│  ▲                                                               │
│  │                                                               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  └──│   Services  │  │   Gateway   │  │   Web UI    │          │
│     │ (Send Email)│  │ (Send Email)│  │  (View)     │          │
│     └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Related Components

| Component | Relationship |
|-----------|--------------|
| **Microservices** | Send emails via SMTP to MailHog |
| **API Gateway** | May send notification emails |
| **Developers** | View captured emails in web UI |

## Quick Start

```bash
# Start MailHog with development profile
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up mailhog

# Access MailHog UI
open http://localhost:8025
```

## Usage

### Sending Emails

Any email sent to `localhost:1025` will be captured by MailHog:

```java
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to ERP AI Platform");
        message.setText("Hello " + userName + ", welcome!");
        mailSender.send(message);
    }
}
```

### Viewing Emails

1. Open http://localhost:8025
2. View all captured emails
3. Search by recipient, subject, or content
4. Click on an email to view full content
5. Release email to send to real recipient (if needed)

### API Access

```bash
# Get all emails
curl http://localhost:8025/api/v2/messages

# Get specific email
curl http://localhost:8025/api/v2/messages/{id}
```

## Email Templates

Example email templates are available in [`mailhog/`](.):

- [`welcome-email.html.example`](welcome-email.html.example) — Welcome email template
- [`EmailService.java.example`](EmailService.java.example) — Email service implementation
- [`EmailProperties.java.example`](EmailProperties.java.example) — Email configuration
- [`application.yml.example`](application.yml.example) — SMTP configuration

## Troubleshooting

### Emails Not Appearing

```bash
# Check if MailHog is running
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development ps mailhog

# Check SMTP port
docker compose exec mailhog nc -zv localhost 1025
```

### Connection Refused

Ensure the application is configured to use `mailhog:1025` as the SMTP host, not `localhost:1025`.
