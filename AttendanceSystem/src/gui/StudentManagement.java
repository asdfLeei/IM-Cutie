package gui;

import database.DatabaseConnection;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentManagement extends JFrame {
    private JTextField txtName;
    private JButton btnGenerateQR, btnSave, btnScanQR;
    private Webcam webcam;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public StudentManagement() {
        setTitle("Student Management");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        JLabel lblName = new JLabel("Student Name:");
        txtName = new JTextField(20);
        btnGenerateQR = new JButton("Generate QR");
        btnSave = new JButton("Save Student");
        btnScanQR = new JButton("Scan QR");

        add(lblName);
        add(txtName);
        add(btnGenerateQR);
        add(btnSave);
        add(btnScanQR);

        btnSave.addActionListener(e -> saveStudent());
        btnGenerateQR.addActionListener(e -> generateQRCode());
        btnScanQR.addActionListener(e -> scanQRCode());
    }

    private void generateQRCode() {
        String name = txtName.getText();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter a name.");
            return;
        }
        
        // Get the student ID from the database
        try {
            Connection conn = DatabaseConnection.connect();
            String sql = "SELECT id FROM students WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int studentId = rs.getInt("id");
                
                // Create directory if it doesn't exist
                File qrDirectory = new File("qrcodes");
                if (!qrDirectory.exists()) {
                    qrDirectory.mkdirs();
                }
                
                // Generate QR code with the student ID
                String filePath = "qrcodes/" + name + ".png";
                QRCodeGenerator.generateQRCode(String.valueOf(studentId), filePath);
                
                // Update the qr_code field in the database
                String updateSql = "UPDATE students SET qr_code = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, filePath);
                updateStmt.setInt(2, studentId);
                updateStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(null, "QR Code generated: " + filePath);
            } else {
                JOptionPane.showMessageDialog(null, "Student not found. Please save the student first.");
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error generating QR code: " + e.getMessage());
        }
    }

    private void saveStudent() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name.");
            return;
        }
        
        try {
            Connection conn = DatabaseConnection.connect();
            
            // First check if the student already exists
            String checkSql = "SELECT id FROM students WHERE name = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Student with this name already exists!");
                return;
            }
            
            // Insert the new student - we don't need to specify the ID column as it's auto-incremented
            String sql = "INSERT INTO students (name, qr_code) VALUES (?, '')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Student saved successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save student.");
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving student: " + e.getMessage());
        }
    }

    private void scanQRCode() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this, "No webcam detected.");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setMirrored(true);
        
        JFrame window = new JFrame("QR Code Scanner");
        window.add(panel);
        window.setSize(640, 480);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setVisible(true);

        executor.submit(() -> {
            while (window.isVisible()) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        Result result = new MultiFormatReader().decode(bitmap);
                        if (result != null) {
                            String qrContent = result.getText();
                            if (recordAttendance(qrContent)) {
                                // Get the student name for the confirmation message
                                Connection conn = DatabaseConnection.connect();
                                String sql = "SELECT name FROM students WHERE id = ?";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setInt(1, Integer.parseInt(qrContent));
                                ResultSet rs = stmt.executeQuery();
                                
                                String studentName = rs.next() ? rs.getString("name") : "Unknown";
                                
                                JOptionPane.showMessageDialog(window, "Attendance recorded for: " + studentName);
                                window.dispose();
                                webcam.close();
                                conn.close();
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Just continue scanning if there's an error
                }
            }
        });
    }

    private boolean recordAttendance(String studentId) {
        try {
            // First check if the studentId is a valid integer
            int id;
            try {
                id = Integer.parseInt(studentId);
            } catch (NumberFormatException e) {
                // If the QR contains a name instead of ID, try to find the ID by name
                Connection conn = DatabaseConnection.connect();
                String sql = "SELECT id FROM students WHERE name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, studentId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    id = rs.getInt("id");
                } else {
                    JOptionPane.showMessageDialog(null, "Student not found: " + studentId);
                    conn.close();
                    return false;
                }
                conn.close();
            }
            
            // Now use the valid student ID to record attendance
            Connection conn = DatabaseConnection.connect();
            String sql = "INSERT INTO attendance (student_id) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int result = stmt.executeUpdate();
            conn.close();
            
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error recording attendance: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentManagement().setVisible(true));
    }
}