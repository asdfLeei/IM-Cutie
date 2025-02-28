package gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeScanner {
    private Webcam webcam;
    private ExecutorService executor;

    public QRCodeScanner() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("Error: No webcam detected!");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();
        executor = Executors.newSingleThreadExecutor();
    }

    public void startScanning() {
        if (webcam == null || !webcam.isOpen()) {
            System.err.println("Error: Webcam is not open or not found!");
            return;
        }

        // Create a JFrame to display the webcam feed
        JFrame frame = new JFrame("QR Code Scanner");
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setMirrored(true);
        frame.add(panel);
        frame.setSize(640, 480);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);

        executor.execute(() -> {
            while (frame.isVisible()) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        String result = decodeQRCode(image);
                        if (result != null) {
                            System.out.println("QR Code Scanned: " + result);
                            processAttendance(result);
                            frame.dispose(); // Close the window after scanning
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            webcam.close();
        });
    }

    private String decodeQRCode(BufferedImage image) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void processAttendance(String studentId) {
        System.out.println("Recording attendance for Student ID: " + studentId);
        // TODO: Add database logic to record attendance
    }

    public static String scanQRCodeFromFile(String filePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(filePath));
            if (bufferedImage == null) {
                System.err.println("Error: Unable to read QR Code image.");
                return null;
            }
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        QRCodeScanner scanner = new QRCodeScanner();
        scanner.startScanning();
    }
}