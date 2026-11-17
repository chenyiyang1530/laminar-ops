package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 真实 SMTP 可选测试；默认跳过。
 * 运行：SMTP_IT=true MAIL_HOST=... MAIL_USERNAME=... MAIL_PASSWORD=... MAIL_TEST_TO=... mvn test -Dtest=MailSmtpIntegrationTest
 */
@EnabledIfEnvironmentVariable(named = "SMTP_IT", matches = "true")
class MailSmtpIntegrationTest {

    @Test
    void sendsRealHtmlMail() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(env("MAIL_HOST", "smtp.163.com"));
        sender.setPort(Integer.parseInt(env("MAIL_PORT", "465")));
        sender.setUsername(env("MAIL_USERNAME", ""));
        sender.setPassword(env("MAIL_PASSWORD", ""));
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");

        MailNotificationService service = new MailNotificationService(sender);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "from", env("MAIL_USERNAME", ""));
        ReflectionTestUtils.setField(service, "to", env("MAIL_TEST_TO", ""));
        assertDoesNotThrow(() -> service.sendHtml(List.of(env("MAIL_TEST_TO", "")),
                "文件巡检 SMTP 集成测试", "<b>SMTP integration test</b>", null, null));
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
