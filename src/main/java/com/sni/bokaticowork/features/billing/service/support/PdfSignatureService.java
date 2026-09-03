package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.config.PdfSignatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

/**
 * Appose une signature PAdES sur un PDF.
 *
 * <p>C'est le seul mecanisme qui fait qu'Adobe Reader, Foxit ou un navigateur affichent d'eux-memes
 * « ce document a ete modifie depuis sa signature ». Tout le reste · gel, empreinte, page de
 * verification · exige une demarche du destinataire ; celui-ci non.
 *
 * <p>La signature est apposee <b>avant</b> le gel : ce sont les octets signes qui sont stockes et
 * dont l'empreinte est conservee. Un fichier telecharge verifie donc dans le lecteur PDF <i>et</i>
 * correspond a l'empreinte de reference.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfSignatureService {

    private final PdfSignatureProperties properties;

    private PrivateKey privateKey;
    private Certificate[] chain;

    /**
     * Charge la cle au demarrage plutot qu'a chaque signature · un keystore illisible doit se
     * signaler tout de suite, pas au premier telechargement de facture.
     *
     * <p>Un echec de chargement desarme la signature sans empecher le demarrage : un document non
     * signe reste couvert par le gel et l'empreinte, alors qu'une application qui refuse de
     * demarrer bloque toute la facturation.
     */
    @PostConstruct
    void loadKeystore() {
        if (!properties.isEnabled()) {
            log.info("Signature PDF desactivee · bokati.pdf.signature.enabled=false");
            return;
        }
        try {
            byte[] keystoreBytes = readKeystore();
            if (keystoreBytes == null) {
                log.warn("Signature PDF activee mais aucun keystore fourni · les PDF ne seront pas signes. "
                        + "Renseigner bokati.pdf.signature.keystore-base64 ou keystore-path.");
                return;
            }
            char[] password = properties.getKeystorePassword() == null
                    ? new char[0]
                    : properties.getKeystorePassword().toCharArray();

            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (InputStream in = new ByteArrayInputStream(keystoreBytes)) {
                keystore.load(in, password);
            }
            String alias = resolveAlias(keystore);
            this.privateKey = (PrivateKey) keystore.getKey(alias, password);
            this.chain = keystore.getCertificateChain(alias);

            if (privateKey == null || chain == null || chain.length == 0) {
                log.warn("Alias '{}' introuvable ou sans cle privee · les PDF ne seront pas signes", alias);
                this.privateKey = null;
                this.chain = null;
                return;
            }
            X509Certificate certificate = (X509Certificate) chain[0];
            log.info("Signature PDF active · sujet={} · expire le {}",
                    certificate.getSubjectX500Principal().getName(), certificate.getNotAfter());
        } catch (Exception ex) {
            // Volontairement non fatal · voir le commentaire de methode.
            log.error("Keystore de signature illisible · les PDF ne seront pas signes : {}", ex.getMessage());
            this.privateKey = null;
            this.chain = null;
        }
    }

    /** La signature est-elle operationnelle ? */
    public boolean isActive() {
        return privateKey != null && chain != null && chain.length > 0;
    }

    /**
     * Signe le document. En cas d'echec, renvoie les octets d'origine : la signature est une
     * garantie supplementaire, pas une condition de l'emission d'une facture.
     */
    public byte[] sign(byte[] pdfBytes) {
        if (!isActive() || pdfBytes == null || pdfBytes.length == 0) {
            return pdfBytes;
        }
        try (PDDocument document = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(subjectCommonName());
            signature.setReason(properties.getReason());
            signature.setLocation(properties.getLocation());
            if (StringUtils.hasText(properties.getContact())) {
                signature.setContactInfo(properties.getContact());
            }
            signature.setSignDate(Calendar.getInstance());

            document.addSignature(signature, cmsSigner());
            // saveIncremental et non save : une signature couvre l'etat du fichier au moment ou
            // elle est posee · reecrire le document entier invaliderait ce qu'elle protege.
            document.saveIncremental(out);
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Signature du PDF impossible · document servi non signe : {}", ex.getMessage());
            return pdfBytes;
        }
    }

    // =================================================================================

    /**
     * Produit la structure CMS detachee qui occupe le champ de signature. PDFBox reserve la place
     * et fournit le contenu a signer ; c'est BouncyCastle qui fabrique l'enveloppe.
     */
    private SignatureInterface cmsSigner() {
        return content -> {
            try {
                X509Certificate certificate = (X509Certificate) chain[0];
                Store<?> certificateStore = new JcaCertStore(Arrays.asList(chain));

                CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
                ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
                generator.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                                new JcaDigestCalculatorProviderBuilder().build())
                                .build(signer, certificate));
                generator.addCertificates(certificateStore);

                CMSTypedData typed = new CmsInputStreamData(content);
                CMSSignedData signed = generator.generate(typed, false);
                return signed.getEncoded();
            } catch (Exception ex) {
                throw new java.io.IOException("Signature CMS impossible", ex);
            }
        };
    }

    private byte[] readKeystore() throws Exception {
        if (StringUtils.hasText(properties.getKeystoreBase64())) {
            return Base64.getDecoder().decode(properties.getKeystoreBase64().trim());
        }
        if (StringUtils.hasText(properties.getKeystorePath())) {
            Path path = Path.of(properties.getKeystorePath().trim());
            if (!Files.exists(path)) {
                log.warn("Keystore introuvable : {}", path.toAbsolutePath());
                return null;
            }
            return Files.readAllBytes(path);
        }
        return null;
    }

    /** L'alias configure, ou le premier du keystore lorsqu'il ne correspond a rien. */
    private String resolveAlias(KeyStore keystore) throws Exception {
        String configured = properties.getAlias();
        if (StringUtils.hasText(configured) && keystore.containsAlias(configured)) {
            return configured;
        }
        List<String> aliases = java.util.Collections.list(keystore.aliases());
        if (aliases.isEmpty()) {
            return configured;
        }
        if (StringUtils.hasText(configured)) {
            log.warn("Alias '{}' absent du keystore · utilisation de '{}'", configured, aliases.getFirst());
        }
        return aliases.getFirst();
    }

    private String subjectCommonName() {
        try {
            return ((X509Certificate) chain[0]).getSubjectX500Principal().getName();
        } catch (Exception ex) {
            return "Bokati Cowork";
        }
    }

    /** Adaptateur minimal · CMSTypedData sur le flux fourni par PDFBox. */
    private record CmsInputStreamData(InputStream content) implements CMSTypedData {

        @Override
        public org.bouncycastle.asn1.ASN1ObjectIdentifier getContentType() {
            return org.bouncycastle.asn1.cms.CMSObjectIdentifiers.data;
        }

        @Override
        public Object getContent() {
            return content;
        }

        @Override
        public void write(java.io.OutputStream out) throws java.io.IOException {
            content.transferTo(out);
        }
    }
}
