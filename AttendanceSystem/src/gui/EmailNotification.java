package gui;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailNotification {
    private static final String FROM_EMAIL = "dump083004@gmail.com"; 
    private static final String PASSWORD = "esmh zfwl lrft tsmo";
    private static final String TO_EMAIL = "23-72354@g.batstate-u.edu.ph";

    public static void sendAttendanceNotification(String studentName, String srCode, String timestamp, boolean isCheckIn) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject("Attendance Notification: " + studentName);

            String timeLabel = isCheckIn ? "Check-In Time" : "Check-Out Time";
            String htmlContent = "<h2>Attendance Recorded</h2>"
                                + "<p>A new attendance record has been created:</p>"
                                + "<table border='1' style='border-collapse: collapse; width: 60%;'>"
                                + "  <tr><th>Student Name</th><td>" + studentName + "</td></tr>"
                                + "  <tr><th>SR-Code</th><td>" + srCode + "</td></tr>"
                                + "  <tr><th>" + timeLabel + "</th><td>" + timestamp + "</td></tr>"
                                + "</table>"
                                + "<p>This is an automated message from your Attendance System.</p>";

            message.setContent(htmlContent, "text/html");
            Transport.send(message);

            System.out.println("Attendance notification email sent successfully to " + TO_EMAIL);
        } catch (MessagingException e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendDailySummary(String dailySummary, String date) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));
            message.setSubject("Daily Attendance Summary: " + date);
            message.setContent(dailySummary, "text/html");

            Transport.send(message);
            System.out.println("Daily summary email sent successfully to " + TO_EMAIL);
        } catch (MessagingException e) {
            System.err.println("Failed to send daily summary email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
