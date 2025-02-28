package gui;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.Dimension;
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
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();
        executor = Executors.newSingleThreadExecutor();
    }

    public void startScanning() {
        if (webcam == null || !webcam.isOpen()) {
            System.err.println("Error: Webcam is not open or not found!");
            return;
        }

        executor.execute(() -> {
            while (true) {
                try {
                    BufferedImage image;
                    do {
                        image = webcam.getImage();
                        if (image == null) {
                            System.out.println("Waiting for webcam image...");
                            Thread.sleep(500);
                        }
                    } while (image == null);

                    String result = decodeQRCode(image);
                    if (result != null) {
                        System.out.println("QR Code Scanned: " + result);
                        processAttendance(result);
                        break;
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
