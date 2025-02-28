package gui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AttendanceScanner extends JFrame {
    private JTextField txtQRCodePath;
    private JButton btnScan;

    public AttendanceScanner() {
        setTitle("QR Code Scanner");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(2, 2, 10, 10));

        JLabel lblPath = new JLabel("QR Code Path:");
        txtQRCodePath = new JTextField(20);
        btnScan = new JButton("Scan");

        add(lblPath);
        add(txtQRCodePath);
        add(btnScan);

        btnScan.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scanAndRecordAttendance();
            }
        });
    }

    private void scanAndRecordAttendance() {
        String filePath = txtQRCodePath.getText();
        String scannedText = QRCodeScanner.scanQRCodeFromFile(filePath);
        
        if (scannedText != null) {
            try {
                Connection conn = DatabaseConnection.connect();
                String sql = "INSERT INTO attendance (student_id) VALUES (?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, scannedText);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Attendance Recorded for: " + scannedText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Error scanning QR code.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceScanner().setVisible(true));
    }
}
