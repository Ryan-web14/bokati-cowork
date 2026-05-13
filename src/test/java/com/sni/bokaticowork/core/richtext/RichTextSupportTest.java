package com.sni.bokaticowork.core.richtext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RichTextSupportTest {

    private final RichTextSupport richTextSupport = new RichTextSupport();

    @Test
    void shouldRenderRichTextHtmlAsFormattedPlainText() {
        String input = """
                <h2>Résumé</h2>
                <p>Bonjour <strong>client</strong>, consultez <a href="https://example.test/ticket">le ticket</a>.</p>
                <ul><li>Première action</li><li><em>Deuxième</em> action</li></ul>
                """;

        String result = richTextSupport.toPlainText(input);

        assertEquals("""
                Résumé

                Bonjour **client**, consultez le ticket (https://example.test/ticket).

                - Première action
                - *Deuxième* action""", result);
    }

    @Test
    void shouldRenderTipTapJsonAsFormattedPlainText() {
        String input = """
                {
                  "type": "doc",
                  "content": [
                    {"type": "paragraph", "content": [
                      {"type": "text", "text": "Bonjour", "marks": [{"type": "bold"}]},
                      {"type": "text", "text": " client"}
                    ]},
                    {"type": "bulletList", "content": [
                      {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Document reçu"}]}]},
                      {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "À vérifier", "marks": [{"type": "italic"}]}]}]}
                    ]}
                  ]
                }
                """;

        String result = richTextSupport.toPlainText(input);

        assertEquals("""
                **Bonjour** client

                - Document reçu
                - *À vérifier*""", result);
    }

    @Test
    void shouldRenderRichTextHtmlAsSafeHtmlForEmailTemplates() {
        String input = """
                <p>Bonjour <strong>client</strong></p>
                <script>alert('xss')</script>
                <ul><li>Document reçu</li></ul>
                """;

        String result = richTextSupport.toSafeHtml(input);

        assertEquals("""
                <p>Bonjour <strong>client</strong></p>

                <ul><li>Document reçu</li></ul>""", result);
    }

    @Test
    void shouldRenderTipTapJsonAsSafeHtmlForEmailTemplates() {
        String input = """
                {
                  "type": "doc",
                  "content": [
                    {"type": "paragraph", "content": [
                      {"type": "text", "text": "Bonjour ", "marks": [{"type": "italic"}]},
                      {"type": "text", "text": "client", "marks": [{"type": "bold"}]}
                    ]},
                    {"type": "orderedList", "content": [
                      {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Relancer"}]}]}
                    ]}
                  ]
                }
                """;

        String result = richTextSupport.toSafeHtml(input);

        assertEquals("<p><em>Bonjour </em><strong>client</strong></p><ol><li><p>Relancer</p></li></ol>", result);
    }
}
