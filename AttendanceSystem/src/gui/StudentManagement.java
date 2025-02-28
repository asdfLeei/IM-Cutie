package gui;

import database.DatabaseConnection;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.border.EmptyBorder;

public class StudentManagement extends JFrame {
    // UI Components
    private JTextField txtName, txtSRCode;
    private JButton btnGenerateQR, btnSave, btnScanQR, btnFindStudent, btnOpenQRFile;
    private JLabel lblQRCode;
    private JPanel qrDisplayPanel;
    private String currentQRPath;
    private Webcam webcam;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public StudentManagement() {
        setTitle("Student Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 240)); // Light gray background

        // Create form panel for student information
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Student Information"));
        formPanel.setBackground(Color.WHITE); // White background
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Student Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Student Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        txtName = new JTextField(25);
        txtName.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(txtName, gbc);

        // SR-Code (editable)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("SR-Code:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        txtSRCode = new JTextField(25);
        txtSRCode.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(txtSRCode, gbc);

        // Find Student Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        btnFindStudent = new JButton("Find Student");
        styleButton(btnFindStudent);
        formPanel.add(btnFindStudent, gbc);

        // Save Student Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        btnSave = new JButton("Save New Student");
        styleButton(btnSave);
        formPanel.add(btnSave, gbc);

        // QR Code Operations Panel
        JPanel qrPanel = new JPanel(new BorderLayout(10, 10));
        qrPanel.setBorder(BorderFactory.createTitledBorder("QR Code Operations"));
        qrPanel.setBackground(Color.WHITE); // White background

        // QR Code Buttons Panel
        JPanel qrButtonsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnGenerateQR = new JButton("Generate QR Code");
        btnScanQR = new JButton("Scan QR Code");
        btnOpenQRFile = new JButton("Open QR File");
        btnOpenQRFile.setEnabled(false);
        styleButton(btnGenerateQR);
        styleButton(btnScanQR);
        styleButton(btnOpenQRFile);
        qrButtonsPanel.add(btnGenerateQR);
        qrButtonsPanel.add(btnScanQR);
        qrButtonsPanel.add(btnOpenQRFile);
        qrPanel.add(qrButtonsPanel, BorderLayout.NORTH);

        // QR Code Display Area
        qrDisplayPanel = new JPanel(new BorderLayout());
        qrDisplayPanel.setBorder(BorderFactory.createEtchedBorder());
        lblQRCode = new JLabel("QR Code will be displayed here", SwingConstants.CENTER);
        lblQRCode.setFont(new Font("Arial", Font.PLAIN, 14));
        qrDisplayPanel.add(lblQRCode, BorderLayout.CENTER);
        qrPanel.add(qrDisplayPanel, BorderLayout.CENTER);

        // Add panels to main panel
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(qrPanel, BorderLayout.CENTER);

        // Add main panel to frame
        add(mainPanel);

        // Add action listeners
        btnSave.addActionListener(e -> saveStudent());
        btnGenerateQR.addActionListener(e -> generateQRCode());
        btnScanQR.addActionListener(e -> scanQRCode());
        btnFindStudent.addActionListener(e -> findStudent());
        btnOpenQRFile.addActionListener(e -> openQRCodeFile());
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(0, 102, 204)); // Blue background
        button.setForeground(Color.BLACK); // White text
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 153, 255)); // Lighter blue on hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 102, 204)); // Original blue on exit
            }
        });
    }

    private void openQRCodeFile() {
        if (currentQRPath != null && !currentQRPath.isEmpty()) {
            try {
                File file = new File(currentQRPath);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    JOptionPane.showMessageDialog(this, "QR Code file not found: " + currentQRPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error opening QR Code file: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "No QR Code file available.");
        }
    }

    private void findStudent() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a student name to search.");
            return;
        }

        try {
            Connection conn = DatabaseConnection.connect();
            String sql = "SELECT id, qr_code FROM students WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int studentId = rs.getInt("id");
                String qrCodePath = rs.getString("qr_code");

                // Get SR-Code from the database if available, or generate a placeholder
                txtSRCode.setText("SR-" + String.format("%06d", studentId));

                // Display QR code if available
                if (qrCodePath != null && !qrCodePath.isEmpty()) {
                    currentQRPath = qrCodePath;
                    displayQRCode(qrCodePath);
                    btnOpenQRFile.setEnabled(true);
                } else {
                    currentQRPath = null;
                    lblQRCode.setIcon(null);
                    lblQRCode.setText("No QR Code available for this student");
                    btnOpenQRFile.setEnabled(false);
                }

                JOptionPane.showMessageDialog(this, "Student found: " + name);
            } else {
                txtSRCode.setText("");
                currentQRPath = null;
                lblQRCode.setIcon(null);
                lblQRCode.setText("QR Code will be displayed here");
                btnOpenQRFile.setEnabled(false);
                JOptionPane.showMessageDialog(this, "Student not found: " + name);
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error finding student: " + e.getMessage());
        }
    }

    private void displayQRCode(String qrCodePath) {
        try {
            File qrFile = new File(qrCodePath);
            if (qrFile.exists()) {
                BufferedImage qrImage = ImageIO.read(qrFile);

                // Resize image if needed (maintain aspect ratio)
                int maxDisplayWidth = 200;
                int maxDisplayHeight = 200;

                double widthRatio = (double) maxDisplayWidth / qrImage.getWidth();
                double heightRatio = (double) maxDisplayHeight / qrImage.getHeight();
                double ratio = Math.min(widthRatio, heightRatio);

                int newWidth = (int) (qrImage.getWidth() * ratio);
                int newHeight = (int) (qrImage.getHeight() * ratio);

                // Create scaled image
                Image scaledImage = qrImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                ImageIcon qrIcon = new ImageIcon(scaledImage);

                lblQRCode.setText("");
                lblQRCode.setIcon(qrIcon);
            } else {
                lblQRCode.setIcon(null);
                lblQRCode.setText("QR Code file not found: " + qrCodePath);
                btnOpenQRFile.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblQRCode.setIcon(null);
            lblQRCode.setText("Error loading QR Code");
            btnOpenQRFile.setEnabled(false);
        }
    }

    private void generateQRCode() {
        String name = txtName.getText().trim();
        String srCode = txtSRCode.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name.");
            return;
        }

        if (srCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SR-Code.");
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

                // Generate QR code with the SR-Code
                String filePath = "qrcodes/" + name + ".png";
                // Use absolute path to ensure file is found
                File qrFile = new File(filePath);
                String absolutePath = qrFile.getAbsolutePath();

                // Generate the QR code with the SR-Code instead of the student ID
                QRCodeGenerator.generateQRCode(srCode, absolutePath);

                // Update the qr_code field in the database with the absolute path
                String updateSql = "UPDATE students SET qr_code = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, absolutePath);
                updateStmt.setInt(2, studentId);
                updateStmt.executeUpdate();

                // Display the QR code
                currentQRPath = absolutePath;
                displayQRCode(absolutePath);
                btnOpenQRFile.setEnabled(true);

                JOptionPane.showMessageDialog(this, "QR Code generated: " + absolutePath);
            } else {
                JOptionPane.showMessageDialog(this, "Student not found. Please save the student first.");
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating QR code: " + e.getMessage());
        }
    }

    private void saveStudent() {
        String name = txtName.getText().trim();
        String srCode = txtSRCode.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name.");
            return;
        }

        if (srCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SR-Code.");
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

            // Insert the new student with the SR-Code
            String sql = "INSERT INTO students (name, sr_code, qr_code) VALUES (?, ?, '')";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, srCode); // Add the SR-Code to the SQL statement
            int result = stmt.executeUpdate();

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Student saved successfully.");
                lblQRCode.setIcon(null);
                lblQRCode.setText("Generate QR code for this student");
                currentQRPath = null;
                btnOpenQRFile.setEnabled(false);
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
        window.setLocationRelativeTo(this);
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
                            // The QR content is now the SR-Code, so we need to handle it differently
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    // First, try to find the student with this SR-Code by assuming SR-Code format
                                    String srCode = qrContent;

                                    // Record attendance first - but we need to find the student ID
                                    Connection conn = DatabaseConnection.connect();
                                    String sql = "SELECT id, name FROM students";
                                    PreparedStatement stmt = conn.prepareStatement(sql);
                                    ResultSet rs = stmt.executeQuery();

                                    boolean found = false;
                                    while (rs.next()) {
                                        int studentId = rs.getInt("id");
                                        // If we find a student with this ID or SR-Code
                                        if (String.valueOf(studentId).equals(qrContent) ||
                                            ("SR-" + String.format("%06d", studentId)).equals(srCode)) {
                                            String studentName = rs.getString("name");

                                            // Record attendance
                                            String insertSql = "INSERT INTO attendance (student_id) VALUES (?)";
                                            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                                            insertStmt.setInt(1, studentId);
                                            insertStmt.executeUpdate();

                                            // Update UI with found student information
                                            txtName.setText(studentName);
                                            txtSRCode.setText(srCode);
                                            findStudent(); // This will also load the QR code

                                            JOptionPane.showMessageDialog(window, "Attendance recorded for: " + studentName);
                                            found = true;
                                            break;
                                        }
                                    }

                                    if (!found) {
                                        JOptionPane.showMessageDialog(window, "No student found with code: " + qrContent);
                                    }

                                    window.dispose();
                                    webcam.close();
                                    conn.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    JOptionPane.showMessageDialog(window, "Error processing QR code: " + e.getMessage());
                                }
                            });
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Just continue scanning if there's an error
                }
            }
        });
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new StudentManagement().setVisible(true));
    }
}