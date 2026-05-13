package com.sni.bokaticowork.core.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RichTextSupport {

    private static final Safelist EMAIL_RICH_TEXT_SAFELIST = Safelist.relaxed()
            .addTags("span", "u", "s", "strike", "blockquote", "pre", "code", "h1", "h2", "h3", "h4", "h5", "h6")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("ol", "start", "type")
            .addAttributes("ul", "type")
            .addProtocols("a", "href", "http", "https", "mailto", "tel");

    private static final Document.OutputSettings EMAIL_OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public String toSafeHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        if (looksLikeJson(normalized)) {
            String rendered = tryRenderJsonHtml(normalized);
            if (StringUtils.hasText(rendered)) {
                return sanitizeHtml(rendered);
            }
        }
        if (looksLikeHtml(normalized)) {
            return sanitizeHtml(normalized);
        }
        return plainTextToHtml(normalized);
    }

    public String toPlainText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        if (looksLikeJson(normalized)) {
            String rendered = tryRenderJson(normalized);
            if (StringUtils.hasText(rendered)) {
                return cleanup(rendered);
            }
        }
        if (!looksLikeHtml(normalized)) {
            return cleanup(normalized);
        }

        StringBuilder out = new StringBuilder();
        renderChildren(Jsoup.parseBodyFragment(normalized).body(), out);
        return cleanup(out.toString());
    }

    private boolean looksLikeHtml(String value) {
        return value.matches("(?s).*<[a-zA-Z][^>]*>.*");
    }

    private boolean looksLikeJson(String value) {
        return value.startsWith("{") || value.startsWith("[");
    }

    private String tryRenderJson(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            StringBuilder out = new StringBuilder();
            renderJsonNode(root, out);
            return out.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String tryRenderJsonHtml(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            StringBuilder out = new StringBuilder();
            renderJsonNodeHtml(root, out);
            return out.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void renderJsonNode(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> renderJsonNode(child, out));
            return;
        }

        String type = text(node.get("type"));
        switch (type == null ? "" : type) {
            case "doc" -> renderJsonNode(node.get("content"), out);
            case "paragraph" -> renderJsonBlock(node, out);
            case "heading" -> renderJsonBlock(node, out);
            case "text" -> renderJsonText(node, out);
            case "hardBreak" -> out.append('\n');
            case "bulletList" -> renderJsonList(node, out, false);
            case "orderedList" -> renderJsonList(node, out, true);
            case "listItem" -> renderJsonNode(node.get("content"), out);
            case "blockquote" -> renderJsonQuote(node, out);
            case "codeBlock" -> {
                out.append("\n```\n");
                renderJsonNode(node.get("content"), out);
                out.append("\n```\n");
            }
            default -> {
                if (node.has("text")) {
                    out.append(text(node.get("text")));
                }
                renderJsonNode(node.get("content"), out);
            }
        }
    }

    private void renderJsonNodeHtml(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> renderJsonNodeHtml(child, out));
            return;
        }

        String type = text(node.get("type"));
        switch (type == null ? "" : type) {
            case "doc" -> renderJsonNodeHtml(node.get("content"), out);
            case "paragraph" -> renderJsonHtmlBlock(node, out, "p");
            case "heading" -> renderJsonHeadingHtml(node, out);
            case "text" -> renderJsonTextHtml(node, out);
            case "hardBreak" -> out.append("<br>");
            case "bulletList" -> renderJsonListHtml(node, out, false);
            case "orderedList" -> renderJsonListHtml(node, out, true);
            case "listItem" -> renderJsonHtmlBlock(node, out, "li");
            case "blockquote" -> renderJsonHtmlBlock(node, out, "blockquote");
            case "codeBlock" -> renderJsonHtmlBlock(node, out, "pre");
            default -> {
                if (node.has("text")) {
                    out.append(escapeHtml(text(node.get("text"))));
                }
                renderJsonNodeHtml(node.get("content"), out);
            }
        }
    }

    private void renderJsonHtmlBlock(JsonNode node, StringBuilder out, String tag) {
        out.append('<').append(tag).append('>');
        renderJsonNodeHtml(node.get("content"), out);
        out.append("</").append(tag).append('>');
    }

    private void renderJsonHeadingHtml(JsonNode node, StringBuilder out) {
        int level = node.path("attrs").path("level").asInt(2);
        if (level < 1 || level > 6) {
            level = 2;
        }
        renderJsonHtmlBlock(node, out, "h" + level);
    }

    private void renderJsonListHtml(JsonNode node, StringBuilder out, boolean ordered) {
        String tag = ordered ? "ol" : "ul";
        out.append('<').append(tag).append('>');
        renderJsonNodeHtml(node.get("content"), out);
        out.append("</").append(tag).append('>');
    }

    private void renderJsonTextHtml(JsonNode node, StringBuilder out) {
        String text = text(node.get("text"));
        if (text == null) {
            return;
        }
        String rendered = escapeHtml(text);
        JsonNode marks = node.get("marks");
        if (marks != null && marks.isArray()) {
            for (JsonNode mark : marks) {
                String type = text(mark.get("type"));
                if ("bold".equals(type)) {
                    rendered = "<strong>" + rendered + "</strong>";
                } else if ("italic".equals(type)) {
                    rendered = "<em>" + rendered + "</em>";
                } else if ("underline".equals(type)) {
                    rendered = "<u>" + rendered + "</u>";
                } else if ("strike".equals(type)) {
                    rendered = "<s>" + rendered + "</s>";
                } else if ("code".equals(type)) {
                    rendered = "<code>" + rendered + "</code>";
                } else if ("link".equals(type)) {
                    String href = text(mark.path("attrs").get("href"));
                    if (StringUtils.hasText(href)) {
                        rendered = "<a href=\"" + escapeHtml(href.trim()) + "\">" + rendered + "</a>";
                    }
                }
            }
        }
        out.append(rendered);
    }

    private void renderJsonBlock(JsonNode node, StringBuilder out) {
        ensureLineStart(out);
        renderJsonNode(node.get("content"), out);
        out.append("\n\n");
    }

    private void renderJsonList(JsonNode node, StringBuilder out, boolean ordered) {
        ensureLineStart(out);
        JsonNode content = node.get("content");
        if (content == null || !content.isArray()) {
            return;
        }
        int index = 1;
        for (JsonNode item : content) {
            out.append(ordered ? index++ + ". " : "- ");
            renderJsonListItem(item, out);
            out.append('\n');
        }
        out.append('\n');
    }

    private void renderJsonListItem(JsonNode item, StringBuilder out) {
        JsonNode content = item.get("content");
        if (content == null || !content.isArray()) {
            renderJsonNode(item, out);
            return;
        }
        for (JsonNode child : content) {
            String type = text(child.get("type"));
            if ("paragraph".equals(type) || "heading".equals(type)) {
                renderJsonNode(child.get("content"), out);
            } else {
                renderJsonNode(child, out);
            }
        }
    }

    private void renderJsonQuote(JsonNode node, StringBuilder out) {
        StringBuilder quoted = new StringBuilder();
        renderJsonNode(node.get("content"), quoted);
        String text = cleanup(quoted.toString());
        if (!text.isEmpty()) {
            ensureLineStart(out);
            for (String line : text.split("\\R")) {
                out.append("> ").append(line).append('\n');
            }
            out.append('\n');
        }
    }

    private void renderJsonText(JsonNode node, StringBuilder out) {
        String text = text(node.get("text"));
        if (text == null) {
            return;
        }
        String prefix = "";
        String suffix = "";
        JsonNode marks = node.get("marks");
        if (marks != null && marks.isArray()) {
            for (JsonNode mark : marks) {
                String type = text(mark.get("type"));
                if ("bold".equals(type)) {
                    prefix += "**";
                    suffix = "**" + suffix;
                } else if ("italic".equals(type)) {
                    prefix += "*";
                    suffix = "*" + suffix;
                } else if ("underline".equals(type)) {
                    prefix += "_";
                    suffix = "_" + suffix;
                } else if ("code".equals(type)) {
                    prefix += "`";
                    suffix = "`" + suffix;
                } else if ("link".equals(type)) {
                    String href = text(mark.path("attrs").get("href"));
                    suffix = StringUtils.hasText(href) ? " (" + href.trim() + ")" + suffix : suffix;
                }
            }
        }
        out.append(prefix).append(text).append(suffix);
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private void renderChildren(Element element, StringBuilder out) {
        for (Node child : element.childNodes()) {
            renderNode(child, out);
        }
    }

    private void renderNode(Node node, StringBuilder out) {
        if (node instanceof TextNode textNode) {
            out.append(textNode.text());
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }

        String tag = element.normalName();
        switch (tag) {
            case "br" -> out.append('\n');
            case "p", "div", "section", "article" -> renderBlock(element, out);
            case "h1", "h2", "h3", "h4", "h5", "h6" -> renderDecorated(element, out, "", "\n\n");
            case "ul" -> renderList(element, out, false);
            case "ol" -> renderList(element, out, true);
            case "blockquote" -> renderQuote(element, out);
            case "strong", "b" -> renderDecorated(element, out, "**", "**");
            case "em", "i" -> renderDecorated(element, out, "*", "*");
            case "u" -> renderDecorated(element, out, "_", "_");
            case "code" -> renderDecorated(element, out, "`", "`");
            case "pre" -> renderDecorated(element, out, "\n```\n", "\n```\n");
            case "a" -> renderLink(element, out);
            default -> renderChildren(element, out);
        }
    }

    private void renderBlock(Element element, StringBuilder out) {
        ensureLineStart(out);
        renderChildren(element, out);
        out.append("\n\n");
    }

    private void renderDecorated(Element element, StringBuilder out, String prefix, String suffix) {
        out.append(prefix);
        renderChildren(element, out);
        out.append(suffix);
    }

    private void renderList(Element element, StringBuilder out, boolean ordered) {
        ensureLineStart(out);
        int index = 1;
        for (Element child : element.children()) {
            if (!"li".equals(child.normalName())) {
                continue;
            }
            out.append(ordered ? index++ + ". " : "- ");
            renderChildren(child, out);
            out.append('\n');
        }
        out.append('\n');
    }

    private void renderQuote(Element element, StringBuilder out) {
        StringBuilder quoted = new StringBuilder();
        renderChildren(element, quoted);
        String text = cleanup(quoted.toString());
        if (!text.isEmpty()) {
            ensureLineStart(out);
            for (String line : text.split("\\R")) {
                out.append("> ").append(line).append('\n');
            }
            out.append('\n');
        }
    }

    private void renderLink(Element element, StringBuilder out) {
        StringBuilder label = new StringBuilder();
        renderChildren(element, label);
        String text = cleanup(label.toString());
        String href = element.attr("href");
        out.append(text);
        if (StringUtils.hasText(href) && !href.equals(text)) {
            out.append(" (").append(href.trim()).append(')');
        }
    }

    private void ensureLineStart(StringBuilder out) {
        int length = out.length();
        if (length > 0 && out.charAt(length - 1) != '\n') {
            out.append('\n');
        }
    }

    private String cleanup(String value) {
        return value
                .replace('\u00a0', ' ')
                .replaceAll("[ \\t]+\\R", "\n")
                .replaceAll("\\R[ \\t]+", "\n")
                .replaceAll("\\R{3,}", "\n\n")
                .trim();
    }

    private String sanitizeHtml(String value) {
        return Jsoup.clean(value, "", EMAIL_RICH_TEXT_SAFELIST, EMAIL_OUTPUT_SETTINGS).trim();
    }

    private String plainTextToHtml(String value) {
        return escapeHtml(value).replaceAll("\\R", "<br>");
    }

    private String escapeHtml(String value) {
        return value == null ? "" : Entities.escape(value);
    }
}
