package org.jahia.test.modules.formidable.samples.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataLayerSnippetRenderFilterTest {

    @Test
    void writesTheScriptBeforeTheFormAndKeepsTheFormUntouched() {
        String out = new DataLayerSnippetRenderFilter().execute("<form></form>", null, null, null);

        assertTrue(out.startsWith("<script>"));
        assertEquals("<form></form>", out.substring(out.indexOf("</script>") + "</script>".length()));
    }

    @Test
    void listensToTheSubmittedEventAndPushesTheAnswersDataLayer() {
        assertTrue(DataLayerSnippetRenderFilter.SNIPPET.contains("formidable:submitted"));
        assertTrue(DataLayerSnippetRenderFilter.SNIPPET.contains("window.dataLayer.push(d)"));
    }
}
