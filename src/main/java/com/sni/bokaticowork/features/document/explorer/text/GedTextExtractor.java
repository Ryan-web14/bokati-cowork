package com.sni.bokaticowork.features.document.explorer.text;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Tire le texte d'un fichier de la GED, pour la recherche.
 *
 * <ul>
 *   <li>PDF : la couche texte, page par page ; une page qui n'en a pas (scan) passe a l'OCR ;</li>
 *   <li>image (PNG, JPEG, TIFF multipage, GIF) : OCR de chaque page ;</li>
 *   <li>texte et CSV : le contenu ;</li>
 *   <li>Word, Excel, PowerPoint et OpenDocument recents : le texte de leur XML interne ;</li>
 *   <li>le reste (anciens formats Office binaires, archives) n'est pas lu.</li>
 * </ul>
 *
 * <p>Le texte est borne a {@code maxChars} et l'OCR a {@code maxOcrPages} pages : un document de
 * 300 pages scannees occuperait le serveur un quart d'heure pour un gain de recherche faible.</p>
 */
public class GedTextExtractor {

    public static final String DONE = "DONE";
    public static final String EMPTY = "EMPTY";
    public static final String UNSUPPORTED = "UNSUPPORTED";
    public static final String FAILED = "FAILED";

    public static final String TEXT_LAYER = "TEXT_LAYER";
    public static final String OCR = "OCR";
    public static final String MIXED = "MIXED";
    public static final String PLAIN = "PLAIN";
    public static final String OFFICE = "OFFICE";

    /** En deca, une page PDF est consideree sans couche texte : c'est un scan. */
    private static final int MIN_PAGE_CHARS = 20;
    private static final float OCR_DPI = 300f;
    /** Garde contre les archives piegees : une entree XML ne se lit pas au-dela. */
    private static final int MAX_XML_ENTRY = 20 * 1024 * 1024;
    private static final Pattern XML_BREAKS = Pattern.compile(
            "</(?:w:p|a:p|text:p|text:h|si|row|table:table-row)>|<(?:w:tab|w:br|text:tab|text:line-break)\\b[^>]*/>");
    private static final Pattern XML_CELLS = Pattern.compile("</(?:c|table:table-cell)>");
    private static final Pattern XML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern SLIDE = Pattern.compile("ppt/slides/slide(\\d+)\\.xml");

    private final GedOcrEngine ocr;
    private final int maxOcrPages;
    private final int maxChars;

    public GedTextExtractor(GedOcrEngine ocr, int maxOcrPages, int maxChars) {
        this.ocr = ocr;
        this.maxOcrPages = Math.max(0, maxOcrPages);
        this.maxChars = Math.max(1000, maxChars);
    }

    /** Le resultat d'une extraction ; {@code error} explique un echec ou une lecture partielle. */
    public record Extraction(String status, String method, Integer pageCount, int ocrPages,
                             String content, boolean truncated, String error) {

        static Extraction unsupported(String reason) {
            return new Extraction(UNSUPPORTED, null, null, 0, null, false, reason);
        }

        static Extraction failed(String reason) {
            return new Extraction(FAILED, null, null, 0, null, false, reason);
        }
    }

    public Extraction extract(byte[] content, String mimeType, String fileName) {
        if (content == null || content.length == 0) {
            return Extraction.unsupported("Fichier vide");
        }
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String extension = extension(fileName);
        try {
            if (mime.equals("application/pdf") || extension.equals("pdf")) {
                return pdf(content);
            }
            if (mime.startsWith("image/") || List.of("png", "jpg", "jpeg", "tif", "tiff", "gif", "bmp").contains(extension)) {
                return image(content);
            }
            if (mime.startsWith("text/") || List.of("txt", "csv").contains(extension)) {
                return finish(PLAIN, null, 0, decode(content), null);
            }
            if (List.of("docx", "xlsx", "pptx", "odt", "ods", "odp").contains(extension)
                    || mime.contains("officedocument") || mime.contains("opendocument")) {
                return office(content, extension, mime);
            }
            return Extraction.unsupported("Format non lu pour la recherche : " + (extension.isEmpty() ? mime : extension));
        } catch (GedOcrEngine.OcrUnavailableException ex) {
            return Extraction.failed(ex.getMessage());
        } catch (IOException | RuntimeException ex) {
            return Extraction.failed(abbreviate("Lecture impossible : " + ex.getMessage()));
        }
    }

    // PDF ---------------------------------------------------------------------------------------

    private Extraction pdf(byte[] content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            int pages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = null;
            StringBuilder text = new StringBuilder();
            int textPages = 0;
            int ocrPages = 0;
            int skippedScans = 0;
            String ocrError = null;
            for (int page = 1; page <= pages && text.length() < maxChars; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (meaningful(pageText) >= MIN_PAGE_CHARS) {
                    textPages++;
                    text.append(pageText).append('\n');
                    continue;
                }
                if (ocrError != null || ocrPages >= maxOcrPages) {
                    skippedScans++;
                    continue;
                }
                if (renderer == null) {
                    renderer = new PDFRenderer(document);
                }
                BufferedImage image = renderer.renderImageWithDPI(page - 1, OCR_DPI, ImageType.GRAY);
                try {
                    text.append(ocr.read(image)).append('\n');
                    ocrPages++;
                } catch (GedOcrEngine.OcrUnavailableException ex) {
                    ocrError = ex.getMessage();
                    skippedScans++;
                }
            }
            String method = ocrPages == 0 ? TEXT_LAYER : textPages == 0 ? OCR : MIXED;
            String note = skippedScans == 0 ? null
                    : skippedScans + " page(s) scannée(s) non lue(s)" + (ocrError == null ? " (limite d'OCR atteinte)" : " : " + ocrError);
            if (ocrError != null && textPages == 0) {
                return new Extraction(FAILED, null, pages, 0, null, false, abbreviate(ocrError));
            }
            return finish(method, pages, ocrPages, text.toString(), note);
        }
    }

    // Images ------------------------------------------------------------------------------------

    private Extraction image(byte[] content) throws IOException {
        List<BufferedImage> pages = readImages(content);
        if (pages.isEmpty()) {
            return Extraction.failed("Image illisible");
        }
        StringBuilder text = new StringBuilder();
        int read = 0;
        for (BufferedImage page : pages) {
            if (read >= Math.max(1, maxOcrPages) || text.length() >= maxChars) {
                break;
            }
            text.append(ocr.read(page)).append('\n');
            read++;
        }
        String note = read < pages.size() ? (pages.size() - read) + " page(s) non lue(s) (limite d'OCR atteinte)" : null;
        return finish(OCR, pages.size(), read, text.toString(), note);
    }

    /** Toutes les pages d'une image : un TIFF de scanner en contient souvent plusieurs. */
    static List<BufferedImage> readImages(byte[] content) throws IOException {
        List<BufferedImage> pages = new ArrayList<>();
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                return pages;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return pages;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                int count = reader.getNumImages(true);
                for (int index = 0; index < count; index++) {
                    pages.add(reader.read(index));
                }
            } finally {
                reader.dispose();
            }
        }
        return pages;
    }

    // Bureautique -------------------------------------------------------------------------------

    private Extraction office(byte[] content, String extension, String mime) throws IOException {
        Map<String, String> xml = new TreeMap<>(slideOrder());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (wanted(name, extension, mime)) {
                    xml.put(name, readEntry(zip));
                }
            }
        }
        if (xml.isEmpty()) {
            return Extraction.failed("Document bureautique sans contenu lisible");
        }
        StringBuilder text = new StringBuilder();
        for (String part : xml.values()) {
            text.append(xmlToText(part)).append('\n');
            if (text.length() >= maxChars) break;
        }
        return finish(OFFICE, null, 0, text.toString(), null);
    }

    private static boolean wanted(String name, String extension, String mime) {
        return switch (extension) {
            case "docx" -> name.equals("word/document.xml") || name.matches("word/(header|footer|footnotes)\\d*\\.xml");
            // Les textes des cellules ; les feuilles elles-memes ne portent que des nombres et des renvois
            case "xlsx" -> name.equals("xl/sharedStrings.xml");
            case "pptx" -> SLIDE.matcher(name).matches();
            case "odt", "ods", "odp" -> name.equals("content.xml");
            default -> mime.contains("opendocument") ? name.equals("content.xml")
                    : name.equals("word/document.xml") || SLIDE.matcher(name).matches() || name.equals("xl/sharedStrings.xml");
        };
    }

    /** Les diapositives dans l'ordre de la presentation : slide2 avant slide10. */
    private static Comparator<String> slideOrder() {
        return (left, right) -> {
            Matcher a = SLIDE.matcher(left);
            Matcher b = SLIDE.matcher(right);
            if (a.matches() && b.matches()) {
                return Integer.compare(Integer.parseInt(a.group(1)), Integer.parseInt(b.group(1)));
            }
            return left.compareTo(right);
        };
    }

    private static String readEntry(InputStream zip) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = zip.read(buffer)) > 0) {
            total += read;
            if (total > MAX_XML_ENTRY) {
                throw new IOException("partie du document trop volumineuse");
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    static String xmlToText(String xml) {
        String text = XML_BREAKS.matcher(xml).replaceAll("\n");
        text = XML_CELLS.matcher(text).replaceAll(" ");
        text = XML_TAGS.matcher(text).replaceAll("");
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }

    // Aides -------------------------------------------------------------------------------------

    private Extraction finish(String method, Integer pages, int ocrPages, String raw, String note) {
        String clean = normalizeWhitespace(raw);
        boolean truncated = clean.length() > maxChars;
        if (truncated) {
            clean = clean.substring(0, maxChars);
        }
        if (clean.isBlank()) {
            return new Extraction(EMPTY, method, pages, ocrPages, null, false, note);
        }
        return new Extraction(DONE, method, pages, ocrPages, clean, truncated, note == null ? null : abbreviate(note));
    }

    static String normalizeWhitespace(String raw) {
        if (raw == null) return "";
        return raw.replace('\u0000', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" ?\\n ?", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static int meaningful(String text) {
        if (text == null) return 0;
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (Character.isLetterOrDigit(text.charAt(index))) count++;
        }
        return count;
    }

    /** UTF-8, ou Windows-1252 pour un vieux CSV exporte d'Excel. */
    static String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException ex) {
            return new String(content, java.nio.charset.Charset.forName("windows-1252"));
        }
    }

    private static String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String abbreviate(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }
}
