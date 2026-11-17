package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

/**
 * 可选邮件通知，网易邮箱使用 smtp.163.com:465 + 授权码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${laminar.file-check.email-enabled:false}")
    private boolean enabled;

    @Value("${laminar.file-check.email-to:}")
    private String to;

    public void send(String subject, String text) {
        sendTo(List.of(), subject, text, null, null);
    }

    public boolean isEnabled() {
        return enabled && !from.isBlank();
    }

    /**
     * 发送 HTML 邮件；to 为空时使用 FILE_CHECK_EMAIL_TO。
     */
    public void sendHtml(List<String> recipients, String subject, String html,
                         byte[] attachment, String attachmentName) {
        sendTo(recipients, subject, html, attachment, attachmentName);
    }

    private void sendTo(List<String> recipients, String subject, String text,
                        byte[] attachment, String attachmentName) {
        if (!enabled || from.isBlank()) return;
        List<String> toList = recipients == null || recipients.isEmpty()
                ? (to.isBlank() ? List.of() : List.of(to.split("\\s*,\\s*")))
                : recipients;
        if (toList.isEmpty()) return;
        try {
            if (attachment == null || attachmentName == null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(toList.toArray(String[]::new));
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
            } else {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(toList.toArray(String[]::new));
                helper.setSubject(subject);
                helper.setText(text, true);
                helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachment));
                mailSender.send(message);
            }
            log.info("[邮件通知] 已发送：{} -> {}", subject, toList);
        } catch (Exception e) {
            log.warn("[邮件通知] 发送失败：{}", e.getMessage());
        }
    }
}
