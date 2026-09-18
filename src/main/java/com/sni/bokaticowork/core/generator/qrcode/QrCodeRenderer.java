package com.sni.bokaticowork.core.generator.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Rend un contenu en image QR, encodee en base64.
 *
 * <p>Le code vivait en methode privee du decorateur de reservation. Le rendre partageable evite
 * qu'une seconde copie apparaisse des qu'un autre module a besoin d'un QR · deux implementations
 * divergeraient sur la taille, la correction d'erreur ou le format, et personne ne remarquerait
 * laquelle a change avant qu'un lecteur ne refuse de lire.</p>
 *
 * <p>Un echec de rendu rend {@code null} plutot que de lever. Un QR est un confort d'affichage : ne
 * pas pouvoir le dessiner ne doit pas faire echouer la creation de ce qu'il represente.</p>
 */
@Slf4j
@Component
public class QrCodeRenderer {

    private static final int DEFAULT_SIZE = 250;
    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    public String toDataUri(String content) {
        return toDataUri(content, DEFAULT_SIZE);
    }

    public String toDataUri(String content, int size) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return DATA_URI_PREFIX + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            log.warn("Rendu QR impossible pour un contenu de {} caracteres : {}",
                    content.length(), ex.getMessage());
            return null;
        }
    }
}
