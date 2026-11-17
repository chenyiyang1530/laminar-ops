package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
        if (!enabled || from.isBlank() || to.isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to.split("\\s*,\\s*"));
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("[邮件通知] 已发送：{}", subject);
        } catch (Exception e) {
            log.warn("[邮件通知] 发送失败：{}", e.getMessage());
        }
    }
}
