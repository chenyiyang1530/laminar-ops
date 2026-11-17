package com.laminar.ops.system.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void disabledMailDoesNotSend() {
        MailNotificationService service = new MailNotificationService(mailSender);
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "from", "ops@163.com");
        service.sendHtml(List.of("to@example.com"), "subject", "<b>body</b>", null, null);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void htmlMailWithAttachmentSendsMimeMessage() throws Exception {
        MailNotificationService service = new MailNotificationService(mailSender);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "from", "ops@163.com");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.sendHtml(List.of("to@example.com"), "subject", "<b>body</b>", "report".getBytes(), "report.xlsx");
        verify(mailSender).send(any(MimeMessage.class));
        message.saveChanges();
        assertTrue(containsText(message, "body"));
        assertTrue(containsAttachment(message, "report.xlsx"));
    }

    private boolean containsText(Part part, String text) throws Exception {
        if (part.isMimeType("text/html") || part.isMimeType("text/plain")) {
            return String.valueOf(part.getContent()).contains(text);
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                if (containsText(multipart.getBodyPart(i), text)) return true;
            }
        }
        return false;
    }

    private boolean containsAttachment(Part part, String name) throws Exception {
        if (part.getFileName() != null && part.getFileName().contains(name)) return true;
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                if (containsAttachment(multipart.getBodyPart(i), name)) return true;
            }
        }
        return false;
    }
}
