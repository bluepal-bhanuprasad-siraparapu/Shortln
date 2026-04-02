package com.urlshortener.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.urlshortener.service.AuditLogService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final com.urlshortener.repository.UserRepository userRepository;
    private final AuditLogService auditLogService;

    public com.urlshortener.entity.User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void sendOtp(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Your One-Time Password (OTP) for Password Reset");
            
            String resetUrl = "http://localhost:5173/forgot-password?email=" + to + "&otp=" + otp;
            
            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                    + "<h2 style=\"color: #4F46E5; text-align: center;\">Password Reset Request</h2>"
                    + "<p style=\"font-size: 16px; color: #333;\">You recently requested to reset your password for your ShortenIt account.</p>"
                    + "<p style=\"font-size: 16px; color: #333;\">Your 6-digit OTP is: <strong style=\"font-size: 20px;\">" + otp + "</strong></p>"
                    + "<p style=\"font-size: 16px; color: #333;\">This code is valid for 5 minutes. You can enter it manually, or simply click the button below to automatically proceed:</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<a href=\"" + resetUrl + "\" style=\"background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;\">Reset Password Now</a>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #777; text-align: center;\">If you did not request a password reset, please ignore this email.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // true indicates HTML content
            
            mailSender.send(message);
            auditLogService.log("EMAIL_SENT: OTP", "OTP email sent successfully to " + to);
            log.info("OTP safely transmitted in HTML template to: {}", to);
        } catch (Exception e) {
            log.error("Failed to transmit OTP in HTML template to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmail(String to, String name, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Welcome to ShortenIt URL Shortener!");
            
            String loginUrl = "http://localhost:5173/auto-login?token=" + token;
            
            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                    + "<h2 style=\"color: #4F46E5; text-align: center;\">Welcome to ShortenIt, " + name + "!</h2>"
                    + "<p style=\"font-size: 16px; color: #333;\">Thank you for registering with ShortenIt Advanced URL Shortener & Analytics Platform.</p>"
                    + "<p style=\"font-size: 16px; color: #333;\">We're so thrilled to have you on board! You can now start transforming your long URLs into concise, branded links.</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<a href=\"" + loginUrl + "\" style=\"background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;\">Click Here to Login</a>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #777; text-align: center;\">If you have any questions, feel free to contact our support team.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // true indicates HTML content
            
            mailSender.send(message);
            auditLogService.log("EMAIL_SENT: WELCOME", "Welcome email sent successfully to " + to);
            log.info("Welcome email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendLinkExpiryEmail(String to, String name, String linkTitle, String shortCode, String expiryDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Action Required: Your link is expiring soon!");
            
            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                    + "<h2 style=\"color: #EF4444; text-align: center;\">Link Expiry Alert</h2>"
                    + "<p style=\"font-size: 16px; color: #333;\">Hello " + name + ",</p>"
                    + "<p style=\"font-size: 16px; color: #333;\">This is a friendly reminder that your shortened link <strong>" + linkTitle + "</strong> (/" + shortCode + ") is scheduled to expire on:</p>"
                    + "<p style=\"font-size: 18px; color: #EF4444; font-weight: bold; text-align: center; margin: 20px 0;\">" + expiryDate + "</p>"
                    + "<p style=\"font-size: 16px; color: #333;\">After this date, the link will no longer redirect users to your target destination.</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<a href=\"http://localhost:5173/dashboard\" style=\"background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;\">Manage My Links</a>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #777; text-align: center;\">If you need this link to stay active, please log in and update the expiration date.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            auditLogService.log("EMAIL_SENT: EXPIRY", "Expiry alert sent successfully to " + to + " for link " + shortCode);
            log.info("Expiry notification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send expiry email to {}: {}", to, e.getMessage(), e);
        }
    }
}
