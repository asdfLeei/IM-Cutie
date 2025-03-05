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

    private JButton btnViewReports;
    private Webcam webcam;

    public StudentManagementWithEmail() {
        super();

        // Footer panel for the "View Reports" button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(new Color(0x748D92)); // Match LoginPage background

        btnViewReports = new JButton("View Reports");
        styleButton(btnViewReports); // Apply custom button style
        footerPanel.add(btnViewReports);
        add(footerPanel, BorderLayout.SOUTH);

        btnViewReports.addActionListener(this::viewReports);
        setupScanButtonWithEmail();
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(0x124E66)); // Dark blue-green background
        button.setForeground(Color.WHITE); // White text
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true); // Ensure the button is opaque
        button.setBorderPainted(false); // Disable border painting
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
            String sql = "SELECT id, name, sr_code, profile_picture FROM students WHERE id = ? OR sr_code = ?";
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
                int studentId = rs.getInt("id");
                String studentName = rs.getString("name");
                String srCode = rs.getString("sr_code");
                String profilePicturePath = rs.getString("profile_picture");

                // Display the student's profile
                displayStudentProfile(studentName, srCode, profilePicturePath);

                // Check if the student has already checked in today
                String checkSql = "SELECT id, check_in_time, check_out_time FROM attendance " +
                                  "WHERE student_id = ? AND DATE(check_in_time) = CURDATE()";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, studentId);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    // Student has already checked in
                    Timestamp checkOutTime = checkRs.getTimestamp("check_out_time");
                    if (checkOutTime == null) {
                        // Update the check-out time
                        String updateSql = "UPDATE attendance SET check_out_time = CURRENT_TIMESTAMP WHERE id = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setInt(1, checkRs.getInt("id"));
                        updateStmt.executeUpdate();

                        JOptionPane.showMessageDialog(this, "Check-out recorded for: " + studentName);

                        // Send email notification for check-out
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        EmailNotification.sendAttendanceNotification(studentName, srCode, timestamp, false); // false = check-out
                    } else {
                        // Student has already checked out
                        JOptionPane.showMessageDialog(this, "You have already checked out for today.");
                    }
                } else {
                    // Insert a new check-in record
                    String insertSql = "INSERT INTO attendance (student_id, check_in_time) VALUES (?, CURRENT_TIMESTAMP)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, studentId);
                    insertStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Check-in recorded for: " + studentName);

                    // Send email notification for check-in
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    EmailNotification.sendAttendanceNotification(studentName, srCode, timestamp, true); // true = check-in
                }
            } else {
                JOptionPane.showMessageDialog(this, "No student found for QR code: " + qrContent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing QR code: " + ex.getMessage());
        }
    }
    
    private void displayStudentProfile(String studentName, String srCode, String profilePicturePath) {
        JDialog profileDialog = new JDialog(this, "Student Profile", true);
        profileDialog.setSize(300, 400);
        profileDialog.setLocationRelativeTo(this);
        profileDialog.setLayout(new BorderLayout(10, 10));

        // Panel to hold the profile picture
        JPanel picturePanel = new JPanel(new BorderLayout());
        picturePanel.setBackground(new Color(0x748D92)); // Match the system theme

        // Load the profile picture
        if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
            try {
                ImageIcon profileIcon = new ImageIcon(profilePicturePath);
                Image image = profileIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                JLabel pictureLabel = new JLabel(new ImageIcon(image));
                pictureLabel.setHorizontalAlignment(JLabel.CENTER);
                picturePanel.add(pictureLabel, BorderLayout.CENTER);
            } catch (Exception e) {
                e.printStackTrace();
                JLabel errorLabel = new JLabel("Error loading profile picture", SwingConstants.CENTER);
                picturePanel.add(errorLabel, BorderLayout.CENTER);
            }
        } else {
            JLabel noPictureLabel = new JLabel("No profile picture available", SwingConstants.CENTER);
            picturePanel.add(noPictureLabel, BorderLayout.CENTER);
        }

        // Panel to hold the name and SR-Code
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        infoPanel.setBackground(new Color(0x748D92)); // Match the system theme

        JLabel nameLabel = new JLabel("Name: " + studentName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        JLabel srCodeLabel = new JLabel("SR-Code: " + srCode, SwingConstants.CENTER);
        srCodeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        srCodeLabel.setForeground(Color.WHITE);

        infoPanel.add(nameLabel);
        infoPanel.add(srCodeLabel);

        // Add panels to the dialog
        profileDialog.add(picturePanel, BorderLayout.CENTER);
        profileDialog.add(infoPanel, BorderLayout.SOUTH);

        // Show the dialog
        profileDialog.setVisible(true);
    }

    private void viewReports(ActionEvent e) {
        new ReportsInterface().setVisible(true);
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