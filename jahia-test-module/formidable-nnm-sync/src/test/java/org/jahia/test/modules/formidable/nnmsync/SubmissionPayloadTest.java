package org.jahia.test.modules.formidable.nnmsync;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SubmissionPayloadTest {

    private static final FormDefinition DEFINITION = new FormDefinition("contact", List.of(
            new FormDefinition.Field("email", "email", true, List.of()),
            new FormDefinition.Field("country", "text", true, List.of("CH", "FR")),
            new FormDefinition.Field("interests", "multiselect", false, List.of("PB", "AU")),
            new FormDefinition.Field("phone", "text", false, List.of())));

    @Test
    void buildsTheDocumentedBody() {
        Map<String, List<String>> parameters = Map.of(
                "email", List.of("john@doe.com"),
                "country", List.of("CH"),
                "interests", List.of("PB"));

        JSONObject body = new JSONObject(SubmissionPayload.build("id-1", DEFINITION, 2,
                Instant.parse("2026-10-21T14:32:05.789Z"), "fr", "https://www.example.com/fr/contact", parameters));

        assertEquals("id-1", body.getString("submissionId"));
        assertEquals("contact", body.getString("configId"));
        assertEquals(2, body.getInt("configVersion"));
        assertEquals("2026-10-21T14:32:05Z", body.getString("timestamp"));
        assertEquals("fr", body.getString("locale"));
        assertEquals("https://www.example.com/fr/contact", body.getString("sourceUrl"));
        JSONArray fields = body.getJSONArray("fields");
        assertEquals(3, fields.length());
        assertEquals("john@doe.com", value(fields, "email"));
        assertEquals("CH", value(fields, "country"));
        assertEquals("PB", ((JSONArray) value(fields, "interests")).getString(0));
    }

    @Test
    void leavesOutWhatTheFormDoesNotDeclareAndWhatIsEmpty() {
        Map<String, List<String>> parameters = Map.of(
                "email", List.of("john@doe.com"),
                "phone", List.of(""),
                "captchaToken", List.of("secret"));

        JSONArray fields = new JSONObject(SubmissionPayload.build("id-1", DEFINITION, 1, Instant.EPOCH, "en", "", parameters))
                .getJSONArray("fields");

        assertEquals(1, fields.length());
        assertFalse(fields.toString().contains("secret"));
    }

    private static Object value(JSONArray fields, String key) {
        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).getString("key").equals(key)) {
                return fields.getJSONObject(i).get("value");
            }
        }
        throw new AssertionError("no field " + key);
    }
}
