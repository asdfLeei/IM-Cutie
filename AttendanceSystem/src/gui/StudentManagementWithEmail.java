package gui;

import database.DatabaseConnection;
import gui.EmailNotification;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudentManagementWithEmail extends StudentManagement {
    
    private JButton btnSendDailySummary;
    private Webcam webcam;

    public StudentManagementWithEmail() {
        super();
        
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSendDailySummary = new JButton("Send Daily Summary");
        footerPanel.add(btnSendDailySummary);
        add(footerPanel, BorderLayout.SOUTH);
        
        btnSendDailySummary.addActionListener(this::sendDailySummary);
        setupScanButtonWithEmail();
    }

    private void setupScanButtonWithEmail() {
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                findAndUpdateScanButton((JPanel) component);
            }
        }
    }

    private void findAndUpdateScanButton(JPanel panel) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JButton && ((JButton) component).getText().equals("Scan QR Code")) {
                JButton scanButton = (JButton) component;
                for (ActionListener al : scanButton.getActionListeners()) {
                    scanButton.removeActionListener(al);
                }
                scanButton.addActionListener(this::scanQRCodeWithEmail);
            } else if (component instanceof JPanel) {
                findAndUpdateScanButton((JPanel) component);
            }
        }
    }

    private void scanQRCodeWithEmail(ActionEvent e) {
        int option = JOptionPane.showOptionDialog(this, "Choose QR scanning method:", "QR Code Scan",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Use Webcam", "Upload QR Image"}, "Use Webcam");

        if (option == 0) {
            scanQRCodeFromWebcam();
        } else {
            scanQRCodeFromFile();
        }
    }

    private void scanQRCodeFromWebcam() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this, "No webcam detected!");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setMirrored(true);

        JFrame window = new JFrame("QR Code Scanner");
        window.add(panel);
        window.pack();
        window.setVisible(true);

        new Thread(() -> {
            while (window.isVisible()) {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    String qrContent = QRCodeScannerWithEmail.scanQRCodeFromImage(image);
                    if (qrContent != null) {
                        processQRCode(qrContent);
                        window.dispose();
                        webcam.close();
                        break;
                    }
                }
            }
        }).start();
    }

    private void scanQRCodeFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String qrContent = QRCodeScannerWithEmail.scanQRCodeFromFile(file.getAbsolutePath());
            if (qrContent != null) {
                processQRCode(qrContent);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to scan QR code or no student found for this code.");
            }
        }
    }

    private void processQRCode(String qrContent) {
        try (Connection conn = DatabaseConnection.connect()) {
            // Query to find a student by either id or sr_code
            String sql = "SELECT id, name, sr_code FROM students WHERE id = ? OR sr_code = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            // Try to parse the QR content as an integer (for student_id)
            try {
                int studentId = Integer.parseInt(qrContent);
                stmt.setInt(1, studentId);
                stmt.setString(2, qrContent); // Also check sr_code
            } catch (NumberFormatException e) {
                // If QR content is not a number, assume it's an sr_code
                stmt.setInt(1, -1); // Invalid ID to ensure no match
                stmt.setString(2, qrContent);
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String studentName = rs.getString("name");
                String srCode = rs.getString("sr_code");
                JOptionPane.showMessageDialog(this, "Attendance recorded for: " + studentName);

                // Send email notification
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                EmailNotification.sendAttendanceNotification(studentName, srCode, timestamp);
            } else {
                JOptionPane.showMessageDialog(this, "No student found for QR code: " + qrContent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing QR code: " + ex.getMessage());
        }
    }

    private void sendDailySummary(ActionEvent e) {
        try {
            QRCodeScannerWithEmail.sendDailyAttendanceSummary();
            JOptionPane.showMessageDialog(this, "Daily attendance summary email sent successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sending daily summary: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new StudentManagementWithEmail().setVisible(true));
    }
}