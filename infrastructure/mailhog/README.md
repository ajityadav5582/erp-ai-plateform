# MailHog - Email Testing for Development

[MailHog](https://github.com/mailhog/MailHog) is an email testing tool for developers. It captures all outgoing emails from your application and provides a web interface to view them, making it perfect for development and testing environments.

## Features

- **SMTP Server**: Listens on port 1025 for incoming emails
- **Web UI**: View captured emails at http://localhost:8025
- **API**: RESTful API for programmatic access to emails
- **No Configuration**: Works out of the box with zero setup
- **Lightweight**: Single binary, no dependencies

## Quick Start

### Start MailHog

```bash
# Start with development profile
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up mailhog

# Or start all development services
docker compose -f compose.base.yml -f compose.infrastructure.yml -f compose.development.yml --profile development up
```

### Access MailHog UI

Open your browser and navigate to:

```
http://localhost:8025
```

You'll see the MailHog web interface where all captured emails are displayed.

## SMTP Configuration

### Spring Boot Application

Configure your Spring Boot application to use MailHog as the SMTP server:

```yaml
# application.yml
spring:
  mail:
    host: mailhog
    port: 1025
    username: ${MAIL_USERNAME:test}
    password: ${MAIL_PASSWORD:test}
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

### Environment Variables

```bash
# .env
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=test
MAIL_PASSWORD=test
MAIL_FROM=noreply@erpai.local
```

### Application Properties

```properties
# application.properties
spring.mail.host=mailhog
spring.mail.port=1025
spring.mail.username=test
spring.mail.password=test
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
```

## Usage Examples

### Sending an Email with Spring Boot

```java
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String to, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@erpai.local");
        message.setTo(to);
        message.setSubject("Welcome to ERP AI Platform");
        message.setText("""
            Dear %s,

            Welcome to the ERP AI Platform!

            Your account has been successfully created.

            Best regards,
            ERP AI Platform Team
            """.formatted(userName));

        mailSender.send(message);
    }
}
```

### Sending an Email with Thymeleaf Template

```java
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.MimeMessage;
import jakarta.mail.internet.MimeMessageHelper;

@Service
public class TemplateEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public TemplateEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendWelcomeEmail(String to, String userName) throws Exception {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("platformName", "ERP AI Platform");

        String htmlContent = templateEngine.process("emails/welcome", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("noreply@erpai.local");
        helper.setTo(to);
        helper.setSubject("Welcome to ERP AI Platform");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
```

### Thymeleaf Email Template

```html
<!-- src/main/resources/templates/emails/welcome.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Welcome</title>
</head>
<body>
    <h1 th:text="${platformName}">ERP AI Platform</h1>
    <p>Dear <span th:text="${userName}">User</span>,</p>
    <p>Welcome to the ERP AI Platform! Your account has been successfully created.</p>
    <p>Best regards,<br>ERP AI Platform Team</p>
</body>
</html>
```

### Using the Email Value Object

```java
import com.erp.platform.common.model.vo.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNotification(Email recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@erpai.local");
        message.setTo(recipient.value());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
```

## MailHog API

MailHog provides a RESTful API for programmatic access:

### List All Messages

```bash
curl http://localhost:8025/api/v2/messages
```

### Get a Specific Message

```bash
curl http://localhost:8025/api/v2/messages/{message_id}
```

### Delete All Messages

```bash
curl -X DELETE http://localhost:8025/api/v2/messages
```

### Release a Message (send to real SMTP)

```bash
curl -X POST http://localhost:8025/api/v2/messages/{message_id}/release
```

## Docker Compose Configuration

### Development Profile

```yaml
# compose.development.yml
services:
  mailhog:
    image: mailhog/mailhog:latest
    container_name: erpai-mailhog
    restart: unless-stopped
    ports:
      - "${MAILHOG_SMTP_PORT:-1025}:1025"  # SMTP
      - "${MAILHOG_UI_PORT:-8025}:8025"    # Web UI
    networks:
      - erpai-network
    profiles:
      - development
```

### Backward Compatibility

```yaml
# docker-compose.yml
services:
  mailhog:
    image: mailhog/mailhog:latest
    container_name: erpai-mailhog
    restart: unless-stopped
    ports:
      - "1025:1025"  # SMTP
      - "8025:8025"  # Web UI
    volumes:
      - mailhog_data:/data
    networks:
      - erpai-network
```

## Testing with MailHog

### Unit Test Example

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    void shouldSendEmail() {
        // Given
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@erpai.local");
        message.setTo("test@example.com");
        message.setSubject("Test Email");
        message.setText("This is a test email.");

        // When
        mailSender.send(message);

        // Then - Verify in MailHog UI at http://localhost:8025
        assertThat(message.getTo()).contains("test@example.com");
    }
}
```

### Integration Test with GreenMail (Alternative)

For integration tests that need to verify email content programmatically, consider using [GreenMail](https://www.icegreen.com/greenmail/):

```xml
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail</artifactId>
    <version>2.0.1</version>
    <scope>test</scope>
</dependency>
```

```java
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceIntegrationTest {

    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void shouldReceiveEmail() throws Exception {
        // Given
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@erpai.local");
        message.setTo("test@example.com");
        message.setSubject("Test Email");
        message.setText("This is a test email.");

        // When
        mailSender.send(message);

        // Then
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        assertThat(greenMail.getReceivedMessages()[0].getSubject())
            .isEqualTo("Test Email");
    }
}
```

## Troubleshooting

### MailHog Not Receiving Emails

1. **Check network connectivity**: Ensure your application can reach `mailhog:1025`
2. **Check firewall**: Ensure port 1025 is not blocked
3. **Check logs**: View MailHog logs with `docker compose logs mailhog`

### Emails Not Showing in UI

1. **Clear browser cache**: Sometimes the UI needs a refresh
2. **Check API**: Verify emails are being received via the API: `curl http://localhost:8025/api/v2/messages`
3. **Restart MailHog**: `docker compose restart mailhog`

### Port Conflicts

If ports 1025 or 8025 are already in use, modify the ports in your docker-compose file:

```yaml
ports:
  - "1026:1025"  # SMTP
  - "8026:8025"  # Web UI
```

## Production Considerations

MailHog is designed for **development and testing only**. For production:

- Use a real SMTP server (e.g., AWS SES, SendGrid, Mailgun)
- Configure proper SPF, DKIM, and DMARC records
- Use TLS/SSL for email transmission
- Implement email delivery tracking and bounce handling

## Documentation

- [MailHog GitHub](https://github.com/mailhog/MailHog)
- [MailHog Documentation](https://github.com/mailhog/MailHog/blob/master/README.md)
- [Spring Boot Email Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#io.email)
