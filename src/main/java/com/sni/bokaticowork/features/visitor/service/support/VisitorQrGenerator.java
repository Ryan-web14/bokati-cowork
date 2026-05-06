package com.sni.bokaticowork.features.visitor.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
@Slf4j
public class VisitorQrGenerator {

    public String generateBase64(String content) {
        byte[] bytes = generateBytes(content);
        return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    public byte[] generateBytes(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 280, 280);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("QR code generation failed for content: {}", content, e);
            return null;
        }
    }
}