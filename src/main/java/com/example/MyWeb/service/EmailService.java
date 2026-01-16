package com.example.MyWeb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@myweb.com}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        if (mailSender == null) {
            System.out.println("---- MOCK EMAIL ----");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + text);
            System.out.println("--------------------");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            System.out.println("Email sent to " + to);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendOrderConfirmation(String to, Long orderId, Double amount) {
        String subject = "Order Confirmation #" + orderId;
        String text = "Thank you for your order!\n\nOrder ID: " + orderId + "\nTotal Amount: $" + amount
                + "\n\nWe will notify you when it ships.";
        sendSimpleMessage(to, subject, text);
    }

    public void sendOrderStatusUpdate(String to, Long orderId, String status) {
        String subject = "Order Update #" + orderId;
        String text = "Your order #" + orderId + " status has been updated to: " + status;
        sendSimpleMessage(to, subject, text);
    }
}
