package gui;

import database.DatabaseConnection;
import gui.EmailNotification;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QRCodeScannerWithEmail {
    
    public static boolean processScannedQRCode(String qrContent) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                System.err.println("Error: Could not connect to database.");
                return false;
            }

            String sql = "SELECT id, name FROM students WHERE id = ? OR ? LIKE CONCAT('SR-', LPAD(id, 6, '0'))";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, qrContent);
            stmt.setString(2, qrContent);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int studentId = rs.getInt("id");
                String studentName = rs.getString("name");
                String srCode = "SR-" + String.format("%06d", studentId);

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);

                String insertSql = "INSERT INTO attendance (student_id, timestamp) VALUES (?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, studentId);
                insertStmt.setTimestamp(2, timestamp);
                insertStmt.executeUpdate();

                // Send email notification for check-in
                EmailNotification.sendAttendanceNotification(studentName, srCode, formattedTimestamp, true); // true = check-in
                return true;
            } else {
                System.err.println("No student found for QR content: " + qrContent);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static void saveStudent(String name, Connection conn) {
        try {
            int studentId = generateStudentId(conn);
            String srCode = "SR-" + String.format("%06d", studentId);
            
            String sql = "INSERT INTO students (id, name, sr_code, qr_code) VALUES (?, ?, ?, '')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, studentId);
            stmt.setString(2, name);
            stmt.setString(3, srCode);
            stmt.executeUpdate();
            System.out.println("Student saved successfully with SR-Code: " + srCode);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error saving student: " + e.getMessage());
        }
    }
    
    public static int generateStudentId(Connection conn) throws Exception {
        String sql = "SELECT MAX(id) FROM students";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) + 1 : 1;
    }
    
    public static String scanQRCodeFromImage(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            
            return result != null ? result.getText() : null;
        } catch (NotFoundException e) {
            return null; // No QR code found
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String scanQRCodeFromFile(String filePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(filePath));
            if (bufferedImage == null) {
                System.err.println("Error: Unable to read QR Code image.");
                return null;
            }
            
            return scanQRCodeFromImage(bufferedImage);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void sendDailyAttendanceSummary() {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                System.err.println("Error: Could not connect to database.");
                return;
            }
            
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String sql = "SELECT s.name, s.id, a.timestamp FROM attendance a " +
                         "JOIN students s ON a.student_id = s.id " +
                         "WHERE DATE(a.timestamp) = ? ORDER BY a.timestamp";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, today);
            ResultSet rs = stmt.executeQuery();
            
            StringBuilder htmlSummary = new StringBuilder();
            htmlSummary.append("<h2>Daily Attendance Summary for ").append(today).append("</h2>");
            htmlSummary.append("<table border='1' style='border-collapse: collapse; width: 80%;'>");
            htmlSummary.append("<tr style='background-color: #4CAF50; color: white;'><th>Student Name</th><th>SR-Code</th><th>Time</th></tr>");
            
            int count = 0;
            while (rs.next()) {
                String studentName = rs.getString("name");
                int studentId = rs.getInt("id");
                String srCode = "SR-" + String.format("%06d", studentId);
                String time = new SimpleDateFormat("HH:mm:ss").format(rs.getTimestamp("timestamp"));
                
                htmlSummary.append("<tr><td>").append(studentName).append("</td><td>").append(srCode).append("</td><td>").append(time).append("</td></tr>");
                count++;
            }
            
            htmlSummary.append("</table>");
            if (count == 0) {
                htmlSummary.append("<p>No attendance records for today.</p>");
            }
            
            EmailNotification.sendDailySummary(htmlSummary.toString(), today);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
