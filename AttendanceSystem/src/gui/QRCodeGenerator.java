package gui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.io.File;
import java.nio.file.Paths;

public class QRCodeGenerator {
    public static void generateQRCode(String text, String filePath) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 200, 200);
            MatrixToImageWriter.writeToPath(matrix, "PNG", Paths.get(filePath));
            System.out.println("QR Code Generated: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
