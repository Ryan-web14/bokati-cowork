package com.sni.bokaticowork.features.inventory.importer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class TabularInventoryFileReader {

    public List<Map<String, String>> read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Import file is required");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                return readCsv(file);
            }
            if (name.endsWith(".xlsx")) {
                return readXlsx(file);
            }
        } catch (Exception ex) {
            throw new BadRequestException("Unable to read import file: " + ex.getMessage());
        }
        throw new BadRequestException("Unsupported import file. Use CSV or XLSX");
    }

    private List<Map<String, String>> readCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) return List.of();
            List<String> headers = parseCsvLine(stripBom(headerLine)).stream().map(this::normalizeHeader).toList();
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) continue;
                List<String> values = parseCsvLine(line);
                rows.add(toRow(headers, values));
            }
            return rows;
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private List<Map<String, String>> readXlsx(MultipartFile file) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && (entry.getName().equals("xl/sharedStrings.xml") || entry.getName().equals("xl/worksheets/sheet1.xml"))) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        byte[] sheetBytes = entries.get("xl/worksheets/sheet1.xml");
        if (sheetBytes == null) {
            throw new BadRequestException("XLSX first worksheet not found");
        }
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        List<List<String>> table = parseSheet(sheetBytes, sharedStrings);
        if (table.isEmpty()) return List.of();
        List<String> headers = table.getFirst().stream().map(this::normalizeHeader).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < table.size(); i++) {
            rows.add(toRow(headers, table.get(i)));
        }
        return rows;
    }

    private List<String> parseSharedStrings(byte[] bytes) throws Exception {
        if (bytes == null) return List.of();
        Document document = parseXml(bytes);
        NodeList nodes = document.getElementsByTagName("t");
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            values.add(nodes.item(i).getTextContent());
        }
        return values;
    }

    private List<List<String>> parseSheet(byte[] bytes, List<String> sharedStrings) throws Exception {
        Document document = parseXml(bytes);
        NodeList rowNodes = document.getElementsByTagName("row");
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            NodeList cells = row.getElementsByTagName("c");
            List<String> values = new ArrayList<>();
            int expectedColumn = 0;
            for (int j = 0; j < cells.getLength(); j++) {
                Element cell = (Element) cells.item(j);
                int columnIndex = columnIndex(cell.getAttribute("r"));
                while (expectedColumn < columnIndex) {
                    values.add("");
                    expectedColumn++;
                }
                values.add(cellValue(cell, sharedStrings));
                expectedColumn++;
            }
            if (values.stream().anyMatch(StringUtils::hasText)) {
                rows.add(values);
            }
        }
        return rows;
    }

    private String cellValue(Element cell, List<String> sharedStrings) {
        NodeList valueNodes = cell.getElementsByTagName("v");
        if (valueNodes.getLength() == 0) return "";
        String raw = valueNodes.item(0).getTextContent();
        if ("s".equals(cell.getAttribute("t"))) {
            int index = Integer.parseInt(raw);
            return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
        }
        return raw;
    }

    private int columnIndex(String ref) {
        if (!StringUtils.hasText(ref)) return 0;
        int result = 0;
        for (char c : ref.toCharArray()) {
            if (!Character.isLetter(c)) break;
            result = result * 26 + (Character.toUpperCase(c) - 'A' + 1);
        }
        return Math.max(0, result - 1);
    }

    private Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(bytes));
    }

    private Map<String, String> toRow(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i).trim() : "");
        }
        return row;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_").replaceAll("^_|_$", "").toLowerCase(Locale.ROOT);
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
