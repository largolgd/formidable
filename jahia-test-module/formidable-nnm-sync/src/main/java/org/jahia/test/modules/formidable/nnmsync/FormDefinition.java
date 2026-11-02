package org.jahia.test.modules.formidable.nnmsync;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * What the NNM system is told about a form: its id (the form's system name) and its submittable
 * fields, flat — steps and fieldsets are walked through, not sent. The JSON is written by hand,
 * not through {@code JSONObject}, whose keys come out in no fixed order: {@link #hash()} must be
 * the same for the same form, byte for byte, or every publication would look like a change.
 */
record FormDefinition(String id, List<Field> fields) {

    /**
     * One submittable field.
     *
     * @param key       the field's node name, the name of its HTML control and the key of its submitted value
     * @param type      {@code email}, {@code multiselect} or {@code text}
     * @param mandatory whether the author made it required
     * @param options   the codes a choice field offers, empty for any other (and for a choice field whose
     *                  options come from a source: the engine's exported API cannot resolve those)
     */
    record Field(String key, String type, boolean mandatory, List<String> options) {

        /** Whether the submitted value is a list: the system wants {@code ["PB"]}, not {@code "PB"}. */
        boolean multiple() {
            return MULTISELECT.equals(type);
        }

        private String json() {
            StringBuilder out = new StringBuilder("{\"key\":").append(JSONObject.quote(key))
                    .append(",\"type\":").append(JSONObject.quote(type))
                    .append(",\"isMandatory\":").append(mandatory);
            if (!options.isEmpty()) {
                out.append(",\"options\":[");
                for (int i = 0; i < options.size(); i++) {
                    out.append(i == 0 ? "" : ",").append("{\"code\":").append(JSONObject.quote(options.get(i))).append('}');
                }
                out.append(']');
            }
            return out.append('}').toString();
        }
    }

    static final String EMAIL = "email";
    static final String MULTISELECT = "multiselect";
    static final String TEXT = "text";

    /** The request body of version {@code version}: {@code {"id":…,"version":…,"fields":[…]}}. */
    String json(int version) {
        return "{\"id\":" + JSONObject.quote(id) + ",\"version\":" + version + ",\"fields\":" + fieldsJson() + "}";
    }

    /** Identifies the content the system would receive, whatever its version: equal hashes, nothing to send. */
    String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((JSONObject.quote(id) + fieldsJson()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java platform", e);
        }
    }

    Field field(String key) {
        return fields.stream().filter(field -> field.key().equals(key)).findFirst().orElse(null);
    }

    private String fieldsJson() {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < fields.size(); i++) {
            out.append(i == 0 ? "" : ",").append(fields.get(i).json());
        }
        return out.append(']').toString();
    }
}
