package org.jahia.test.modules.formidable.nnmsync;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormDefinitionTest {

    private static FormDefinition sample() {
        return new FormDefinition("contact", List.of(
                new FormDefinition.Field("email", "email", true, List.of()),
                new FormDefinition.Field("country", "text", true, List.of("CH", "FR")),
                new FormDefinition.Field("interests", "multiselect", false, List.of("PB", "AU"))));
    }

    @Test
    void theBodyIsTheOneTheSystemDocumentsIt() {
        JSONObject body = new JSONObject(sample().json(3));

        assertEquals("contact", body.getString("id"));
        assertEquals(3, body.getInt("version"));
        JSONArray fields = body.getJSONArray("fields");
        assertEquals(3, fields.length());
        assertEquals("email", fields.getJSONObject(0).getString("key"));
        assertTrue(fields.getJSONObject(0).getBoolean("isMandatory"));
        assertFalse(fields.getJSONObject(0).has("options"));
        assertEquals("CH", fields.getJSONObject(1).getJSONArray("options").getJSONObject(0).getString("code"));
        assertEquals("multiselect", fields.getJSONObject(2).getString("type"));
    }

    @Test
    void theHashIgnoresTheVersionAndFollowsTheContent() {
        assertEquals(sample().hash(), sample().hash());

        FormDefinition changed = new FormDefinition("contact", List.of(new FormDefinition.Field("email", "email", false, List.of())));
        assertNotEquals(sample().hash(), changed.hash());
    }

    @Test
    void aValueThatNeedsEscapingStaysValidJson() {
        FormDefinition tricky = new FormDefinition("a\"b", List.of(new FormDefinition.Field("k\\", "text", false, List.of("x\"y"))));

        assertEquals("a\"b", new JSONObject(tricky.json(1)).getString("id"));
    }
}
