package gui;

import database.DatabaseConnection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportsInterface extends JFrame {
    private JButton btnDailyReport, btnWeeklyReport, btnMonthlyReport, btnSendSummary;
    private JPanel chartPanel;

    public ReportsInterface() {
        setTitle("Attendance Reports");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Main panel with light background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(0x748D92)); // Match the system theme

        // Button panel for report options
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(0x748D92)); // Match the system theme

        btnDailyReport = new JButton("Daily Report");
        btnWeeklyReport = new JButton("Weekly Report");
        btnMonthlyReport = new JButton("Monthly Report");
        btnSendSummary = new JButton("Send Summary");

        styleButton(btnDailyReport);
        styleButton(btnWeeklyReport);
        styleButton(btnMonthlyReport);
        styleButton(btnSendSummary);

        buttonPanel.add(btnDailyReport);
        buttonPanel.add(btnWeeklyReport);
        buttonPanel.add(btnMonthlyReport);
        buttonPanel.add(btnSendSummary);

        // Chart panel to display the graph
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(new Color(0x748D92)); // Match the system theme

        // Add components to the main panel
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);

        // Add main panel to the frame
        add(mainPanel);

        // Add action listeners
        btnDailyReport.addActionListener(e -> showDailyReport());
        btnWeeklyReport.addActionListener(e -> showWeeklyReport());
        btnMonthlyReport.addActionListener(e -> showMonthlyReport());
        btnSendSummary.addActionListener(e -> sendDailySummary());
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

    private void showDailyReport() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate today = LocalDate.now();
        String date = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try (Connection conn = DatabaseConnection.connect()) {
            String sql = "SELECT COUNT(*) AS attendance_count, HOUR(check_in_time) AS hour " +
                         "FROM attendance WHERE DATE(check_in_time) = ? " +
                         "GROUP BY HOUR(check_in_time)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, date);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int hour = rs.getInt("hour");
                int count = rs.getInt("attendance_count");
                dataset.addValue(count, "Attendance", hour + ":00");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching daily attendance data: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Daily Attendance Report - " + date, "Hour", "Attendance Count", dataset);
        customizeChartColors(chart); // Customize bar colors
        updateChartPanel(chart);
    }

    private void showWeeklyReport() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate startOfWeek = LocalDate.now().minusDays(6); // Last 7 days
        LocalDate endOfWeek = LocalDate.now();

        try (Connection conn = DatabaseConnection.connect()) {
            String sql = "SELECT COUNT(*) AS attendance_count, DATE(check_in_time) AS date " +
                         "FROM attendance WHERE DATE(check_in_time) BETWEEN ? AND ? " +
                         "GROUP BY DATE(check_in_time)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, startOfWeek.toString());
            stmt.setString(2, endOfWeek.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String date = rs.getString("date");
                int count = rs.getInt("attendance_count");
                dataset.addValue(count, "Attendance", date);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching weekly attendance data: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Weekly Attendance Report", "Date", "Attendance Count", dataset);
        customizeChartColors(chart); // Customize bar colors
        updateChartPanel(chart);
    }

    private void showMonthlyReport() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now();

        try (Connection conn = DatabaseConnection.connect()) {
            String sql = "SELECT COUNT(*) AS attendance_count, DATE(check_in_time) AS date " +
                         "FROM attendance WHERE DATE(check_in_time) BETWEEN ? AND ? " +
                         "GROUP BY DATE(check_in_time)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, startOfMonth.toString());
            stmt.setString(2, endOfMonth.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String date = rs.getString("date");
                int count = rs.getInt("attendance_count");
                dataset.addValue(count, "Attendance", date);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching monthly attendance data: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Attendance Report", "Date", "Attendance Count", dataset);
        customizeChartColors(chart); // Customize bar colors
        updateChartPanel(chart);
    }

    private void customizeChartColors(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        // Set custom bar colors
        renderer.setSeriesPaint(0, new Color(0x124E66)); // Dark blue-green for the bars
        plot.setBackgroundPaint(new Color(0x748D92)); // Match the system theme for the background
        plot.setRangeGridlinePaint(Color.WHITE); // White grid lines
    }

    private void updateChartPanel(JFreeChart chart) {
        chartPanel.removeAll();
        ChartPanel newChartPanel = new ChartPanel(chart);
        newChartPanel.setPreferredSize(new Dimension(700, 450));
        chartPanel.add(newChartPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void sendDailySummary() {
        try {
            QRCodeScannerWithEmail.sendDailyAttendanceSummary();
            JOptionPane.showMessageDialog(this, "Daily attendance summary email sent successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sending daily summary: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReportsInterface().setVisible(true));
    }
}