package com.expense.tracking.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.expense.tracking.dto.EmailTemplateData;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "email.notifications.enabled", havingValue = "true")
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.notifications.from}")
    private String fromEmail;

    @Value("${email.notifications.from.name}")
    private String fromName;

    @Value("${email.notifications.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${email.notifications.retry.delay:1000}")
    private long retryDelay;

    /**
     * Send daily reminder email to user
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendDailyReminder(String email, EmailTemplateData.DailyReminderData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending daily reminder email to: {}", email);
                
                Context context = new Context();
                context.setVariable("userName", data.getUserName());
                context.setVariable("date", data.getDate());
                context.setVariable("streakDays", data.getStreakDays());
                context.setVariable("monthlyBudget", data.getMonthlyBudget());
                context.setVariable("currentSpending", data.getCurrentSpending());
                context.setVariable("topCategory", data.getTopCategory());

                String htmlContent = templateEngine.process("email/daily-reminder", context);
                
                sendHtmlEmail(
                    email,
                    "💰 Daily Expense Reminder - " + data.getDate(),
                    htmlContent
                );
                
                log.info("Daily reminder email sent successfully to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send daily reminder email to: {}", email, e);
                throw new RuntimeException("Failed to send daily reminder email", e);
            }
        });
    }

    /**
     * Send weekly summary email to user
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendWeeklySummary(String email, EmailTemplateData.WeeklySummaryData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending weekly summary email to: {}", email);
                
                Context context = new Context();
                context.setVariable("userName", data.getUserName());
                context.setVariable("weekStart", data.getWeekStart());
                context.setVariable("weekEnd", data.getWeekEnd());
                context.setVariable("totalSpent", data.getTotalSpent());
                context.setVariable("categoryBreakdown", data.getCategoryBreakdown());
                context.setVariable("topExpenses", data.getTopExpenses());
                context.setVariable("chartImageUrl", data.getChartImageUrl());

                String htmlContent = templateEngine.process("email/weekly-summary", context);
                
                sendHtmlEmail(
                    email,
                    "📊 Weekly Expense Summary - " + data.getWeekStart() + " to " + data.getWeekEnd(),
                    htmlContent
                );
                
                log.info("Weekly summary email sent successfully to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send weekly summary email to: {}", email, e);
                throw new RuntimeException("Failed to send weekly summary email", e);
            }
        });
    }

    /**
     * Send budget alert email to user
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendBudgetAlert(String email, EmailTemplateData.BudgetAlertData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending budget alert email to: {} ({}% of budget)", email, data.getPercentage());
                
                Context context = new Context();
                context.setVariable("userName", data.getUserName());
                context.setVariable("budgetAmount", data.getBudgetAmount());
                context.setVariable("currentSpending", data.getCurrentSpending());
                context.setVariable("percentage", data.getPercentage());
                context.setVariable("alertType", data.getAlertType());
                context.setVariable("topCategories", data.getTopCategories());

                String htmlContent = templateEngine.process("email/budget-alert", context);
                
                String subject = data.getAlertType().equals("exceeded") 
                    ? "🚨 Budget Exceeded Alert!" 
                    : "⚠️ Budget Warning Alert!";
                
                sendHtmlEmail(email, subject, htmlContent);
                
                log.info("Budget alert email sent successfully to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send budget alert email to: {}", email, e);
                throw new RuntimeException("Failed to send budget alert email", e);
            }
        });
    }

    /**
     * Send custom reminder email to user
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendCustomReminder(String email, String subject, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending custom reminder email to: {}", email);
                
                // Create a simple HTML template for custom messages
                String htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>%s</title>
                        <style>
                            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                            .container { background-color: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                            .header { text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #e9ecef; }
                            .message { font-size: 16px; margin: 20px 0; }
                            .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef; text-align: center; font-size: 12px; color: #6c757d; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>💰 Expense Tracker Reminder</h1>
                            </div>
                            <div class="message">%s</div>
                            <div class="footer">
                                <p>Best regards,<br>Your Expense Tracker Team</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """, subject, message);
                
                sendHtmlEmail(email, subject, htmlContent);
                
                log.info("Custom reminder email sent successfully to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send custom reminder email to: {}", email, e);
                throw new RuntimeException("Failed to send custom reminder email", e);
            }
        });
    }

    /**
     * Send HTML email using JavaMailSender
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new MessagingException("Failed to send email", e);
        }
    }

    /**
     * Check if email notifications are enabled
     */
    public boolean isEmailNotificationsEnabled() {
        return true; // This service is only created when email.notifications.enabled=true
    }

    /**
     * Validate email address format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation regex
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}