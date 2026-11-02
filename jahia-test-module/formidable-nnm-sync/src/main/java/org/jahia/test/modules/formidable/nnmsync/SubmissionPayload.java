package org.jahia.test.modules.formidable.nnmsync;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The body the NNM system receives for one submission. Only the fields the form's configuration
 * declares go out, under the keys it declared: the pipeline's parameter map also carries what is not
 * the visitor's answer (a captcha token, a hidden technical field), and the system would refuse a key
 * it does not know. A field the visitor left empty is left out.
 */
final class SubmissionPayload {

    private SubmissionPayload() {
    }

    /**
     * @param submissionId unique id of this submission, made on the server: the system answers 409 to a
     *                     second request with the same one, which is what makes a resend harmless
     * @param definition   the form's fields as read now; their keys select and shape the values
     * @param version      the configuration version the system last accepted for the form
     */
    static String build(String submissionId, FormDefinition definition, int version, Instant at, String locale,
                        String sourceUrl, Map<String, List<String>> parameters) {
        JSONArray fields = new JSONArray();
        for (FormDefinition.Field field : definition.fields()) {
            List<String> values = parameters.getOrDefault(field.key(), List.of()).stream()
                    .filter(value -> value != null && !value.isEmpty())
                    .toList();
            if (values.isEmpty()) {
                continue;
            }
            JSONObject entry = new JSONObject().put("key", field.key());
            entry.put("value", field.multiple() ? new JSONArray(values) : values.get(0));
            fields.put(entry);
        }
        return new JSONObject()
                .put("submissionId", submissionId)
                .put("configId", definition.id())
                .put("configVersion", version)
                .put("timestamp", at.truncatedTo(ChronoUnit.SECONDS).toString())
                .put("locale", locale)
                .put("sourceUrl", sourceUrl)
                .put("fields", fields)
                .toString();
    }

    static String newSubmissionId() {
        return UUID.randomUUID().toString();
    }
}
