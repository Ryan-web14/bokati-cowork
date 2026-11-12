package com.sni.bokaticowork.features.visitor.service.support;

import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * QR des laissez-passer visiteurs.
 *
 * <p>Delegue au generateur commun {@link QrCodeRenderer} · meme contenu, meme taille (280 px),
 * meme format PNG qu'avant. L'API est gardee pour le badge et le courriel d'invitation.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VisitorQrGenerator {

    static final int SIZE = 280;

    private final QrCodeRenderer qrCodeRenderer;

    public String generateBase64(String content) {
        return qrCodeRenderer.toBase64(content, SIZE);
    }

    public byte[] generateBytes(String content) {
        return qrCodeRenderer.toPng(content, SIZE);
    }
}
