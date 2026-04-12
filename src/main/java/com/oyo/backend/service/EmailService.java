package com.oyo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Async
  public void sendOtpEmail(String to, String name, String otp) {
    sendHtmlEmail(to, "OYO - Your OTP Verification Code", buildOtpEmailHtml(name, otp));
  }

  @Async
  public void sendPasswordResetEmail(String to, String name, String otp) {
    sendHtmlEmail(to, "OYO - Password Reset OTP", buildPasswordResetEmailHtml(name, otp));
  }

  @Async
  public void sendBookingConfirmationEmail(String to, String name, String bookingId,
      String hotelName, String checkIn, String checkOut, double totalAmount) {
    sendHtmlEmail(to,
        "OYO - Booking Confirmed! #" + bookingId.substring(0, 8).toUpperCase(),
        buildBookingConfirmationHtml(name, bookingId, hotelName, checkIn, checkOut, totalAmount));
  }

  @Async
  public void sendBookingCancellationEmail(String to, String name, String bookingId, String hotelName) {
    sendHtmlEmail(to,
        "OYO - Booking Cancelled #" + bookingId.substring(0, 8).toUpperCase(),
        buildCancellationHtml(name, bookingId, hotelName));
  }

  private void sendHtmlEmail(String to, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      // From address MUST match the authenticated Gmail account
      helper.setFrom(senderEmail, "OYO Rooms");
      mailSender.send(message);
      log.info("Email sent successfully to {}", to);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
  }

  private String buildOtpEmailHtml(String name, String otp) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px;">
          <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 40px;">
            <div style="text-align: center; margin-bottom: 30px;">
              <h1 style="color: #E31837; font-size: 36px; margin: 0;">OYO</h1>
            </div>
            <h2 style="color: #333;">Hello, %s! 👋</h2>
            <p style="color: #666; font-size: 16px;">Your OTP verification code is:</p>
            <div style="background: #f8f8f8; border: 2px dashed #E31837; border-radius: 8px; text-align: center; padding: 20px; margin: 20px 0;">
              <h1 style="color: #E31837; font-size: 48px; letter-spacing: 12px; margin: 0;">%s</h1>
            </div>
            <p style="color: #888; font-size: 14px;">This OTP is valid for 10 minutes. Do not share it with anyone.</p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
            <p style="color: #aaa; font-size: 12px; text-align: center;">OYO Rooms | Making Travel Easy</p>
          </div>
        </body>
        </html>
        """
        .formatted(name, otp);
  }

  private String buildPasswordResetEmailHtml(String name, String otp) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px;">
          <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 40px;">
            <h1 style="color: #E31837; text-align: center;">OYO</h1>
            <h2 style="color: #333;">Password Reset - %s</h2>
            <p style="color: #666;">Use the OTP below to reset your password:</p>
            <div style="background: #f8f8f8; border: 2px dashed #E31837; border-radius: 8px; text-align: center; padding: 20px; margin: 20px 0;">
              <h1 style="color: #E31837; font-size: 48px; letter-spacing: 12px; margin: 0;">%s</h1>
            </div>
            <p style="color: #888; font-size: 14px;">If you didn't request this, please ignore this email.</p>
          </div>
        </body>
        </html>
        """
        .formatted(name, otp);
  }

  private String buildBookingConfirmationHtml(String name, String bookingId, String hotelName,
      String checkIn, String checkOut, double totalAmount) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px;">
          <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 40px;">
            <h1 style="color: #E31837; text-align: center;">OYO</h1>
            <div style="background: #4CAF50; color: white; border-radius: 8px; padding: 15px; text-align: center;">
              <h2 style="margin: 0;">✅ Booking Confirmed!</h2>
            </div>
            <p>Hello <strong>%s</strong>, your booking is confirmed!</p>
            <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
              <tr><td style="padding: 8px; border: 1px solid #eee; background: #f9f9f9;"><strong>Booking ID</strong></td><td style="padding: 8px; border: 1px solid #eee;">%s</td></tr>
              <tr><td style="padding: 8px; border: 1px solid #eee; background: #f9f9f9;"><strong>Hotel</strong></td><td style="padding: 8px; border: 1px solid #eee;">%s</td></tr>
              <tr><td style="padding: 8px; border: 1px solid #eee; background: #f9f9f9;"><strong>Check-in</strong></td><td style="padding: 8px; border: 1px solid #eee;">%s</td></tr>
              <tr><td style="padding: 8px; border: 1px solid #eee; background: #f9f9f9;"><strong>Check-out</strong></td><td style="padding: 8px; border: 1px solid #eee;">%s</td></tr>
              <tr><td style="padding: 8px; border: 1px solid #eee; background: #f9f9f9;"><strong>Total Amount</strong></td><td style="padding: 8px; border: 1px solid #eee; color: #E31837;"><strong>₹%.2f</strong></td></tr>
            </table>
            <p style="color: #aaa; font-size: 12px; text-align: center;">OYO Rooms | Making Travel Easy</p>
          </div>
        </body>
        </html>
        """
        .formatted(name, bookingId.substring(0, 8).toUpperCase(), hotelName, checkIn, checkOut, totalAmount);
  }

  private String buildCancellationHtml(String name, String bookingId, String hotelName) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 20px;">
          <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 40px;">
            <h1 style="color: #E31837; text-align: center;">OYO</h1>
            <div style="background: #ff5722; color: white; border-radius: 8px; padding: 15px; text-align: center;">
              <h2 style="margin: 0;">❌ Booking Cancelled</h2>
            </div>
            <p>Hello <strong>%s</strong>, your booking has been cancelled.</p>
            <p><strong>Booking ID:</strong> %s</p>
            <p><strong>Hotel:</strong> %s</p>
            <p>We're sorry to see you go. If you have any questions, contact our support team.</p>
          </div>
        </body>
        </html>
        """
        .formatted(name, bookingId.substring(0, 8).toUpperCase(), hotelName);
  }
}
