package gui;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailNotification {
    private static final String FROM_EMAIL = "dump083004@gmail.com"; // Change to your sender email
    private static final String PASSWORD = "esmh zfwl lrft tsmo"; // Use an app password for Gmail
    private static final String TO_EMAIL = "estano.jake.r@gmail.com";
    
    /**
     * Sends an email notification when a student's attendance is recorded
     * 
     * @param studentName The name of the student
     * @param srCode The SR-Code of the student
     * @param timestamp The timestamp when attendance was recorded
     * @param isCheckIn A boolean indicating whether this is a check-in (true) or check-out (false) event
     */
    public static void sendAttendanceNotification(String studentName, String srCode, String timestamp, boolean isCheckIn) {
        try {
            // Set mail properties
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");

            // Create session with authenticator
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject("Attendance Notification: " + studentName);

            // Determine if this is a check-in or check-out event
            String timeLabel = isCheckIn ? "Check-In Time" : "Check-Out Time";

            // Create HTML content
            String htmlContent = "<h2>Attendance Recorded</h2>" +
                                "<p>A new attendance record has been created:</p>" +
                                "<table border='1' style='border-collapse: collapse; width: 60%;'>" +
                                "  <tr style='background-color: #f2f2f2;'>" +
                                "    <th style='padding: 8px; text-align: left;'>Student Name</th>" +
                                "    <td style='padding: 8px;'>" + studentName + "</td>" +
                                "  </tr>" +
                                "  <tr>" +
                                "    <th style='padding: 8px; text-align: left;'>SR-Code</th>" +
                                "    <td style='padding: 8px;'>" + srCode + "</td>" +
                                "  </tr>" +
                                "  <tr style='background-color: #f2f2f2;'>" +
                                "    <th style='padding: 8px; text-align: left;'>" + timeLabel + "</th>" +
                                "    <td style='padding: 8px;'>" + timestamp + "</td>" +
                                "  </tr>" +
                                "</table>" +
                                "<p>This is an automated message from your Attendance System.</p>";

            // Set content
            message.setContent(htmlContent, "text/html");

            // Send message
            Transport.send(message);

            System.out.println("Attendance notification email sent successfully to " + TO_EMAIL);
        } catch (MessagingException e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a daily summary email with all attendance records for the day
     * 
     * @param dailySummary A formatted string containing the daily summary
     * @param date The date for which the summary is generated
     */
    public static void sendDailySummary(String dailySummary, String date) {
        // Set mail properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        
        // Create session with authenticator
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
        
        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject("Daily Attendance Summary: " + date);
            
            // Set content
            message.setContent(dailySummary, "text/html");
            
            // Send message
            Transport.send(message);
            
            System.out.println("Daily summary email sent successfully to " + TO_EMAIL);
            
        } catch (MessagingException e) {
            System.err.println("Failed to send daily summary email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}