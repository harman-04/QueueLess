package com.queueless.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("QueueLess | OTP for Password Reset");

            // Load HTML template from resources
            String htmlTemplate = loadHtmlTemplate("templates/otp-template.html");
            String processedHtml = htmlTemplate.replace("{{OTP}}", otp);

            helper.setText(processedHtml, true); // HTML content

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
        } catch (Exception e) {
            log.error("Error reading OTP HTML template", e);
        }
    }

    private String loadHtmlTemplate(String path) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public void sendUpcomingTokenEmail(String toEmail, String tokenId, String serviceName, int minutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("QueueLess – Your turn is coming up!");

            String htmlTemplate = loadHtmlTemplate("templates/upcoming-token-template.html");
            String processedHtml = htmlTemplate
                    .replace("{{TOKEN_ID}}", tokenId)
                    .replace("{{SERVICE_NAME}}", serviceName)
                    .replace("{{MINUTES}}", String.valueOf(minutes));

            helper.setText(processedHtml, true);
            mailSender.send(message);
            log.info("Upcoming token email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send upcoming token email to {}", toEmail, e);
        }
    }

    // In EmailService.java
    public void sendAlertEmail(String toEmail, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("QueueLess Alert – Queue threshold exceeded");
            helper.setText(message, false); // plain text
            mailSender.send(mimeMessage);
            log.info("Alert email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send alert email to {}", toEmail, e);
        }
    }

    // In EmailService.java
    public void sendVerificationOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("QueueLess – Verify Your Email");

            String htmlTemplate = loadHtmlTemplate("templates/verification-template.html");
            String processedHtml = htmlTemplate.replace("{{OTP}}", otp);

            helper.setText(processedHtml, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }
    }
}
