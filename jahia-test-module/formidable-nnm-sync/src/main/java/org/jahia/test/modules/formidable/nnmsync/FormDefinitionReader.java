package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.modules.formidable.engine.api.FmdbNodeName;
import org.jahia.modules.formidable.engine.api.FmdbProperty;
import org.jahia.services.content.JCRNodeWrapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a form's {@link FormDefinition} from its content. The node must come from a session in the
 * site's default language: that is where the options' values live. Elements are recognised by their
 * mixins, never by their concrete type, so a field type added by another module is read like the others.
 */
final class FormDefinitionReader {

    /** The type of the action that connects a form to the NNM system (this module's CND). */
    static final String SYNC_ACTION = "fmdbnnm:syncAction";

    private static final Logger log = LoggerFactory.getLogger(FormDefinitionReader.class);

    /** The fixed child of {@code fmdb:form} that holds its steps, fieldsets and fields. */
    private static final String FIELDS = "fields";
    private static final String REQUIRED = "required";
    private static final String MANUAL_MODE = "manual";

    private FormDefinitionReader() {
    }

    /** Whether an author connected this form to the system: a sync action in its action list. */
    static boolean carriesSyncAction(JCRNodeWrapper form) throws RepositoryException {
        if (!form.hasNode(FmdbNodeName.ACTIONS)) {
            return false;
        }
        NodeIterator actions = form.getNode(FmdbNodeName.ACTIONS).getNodes();
        while (actions.hasNext()) {
            if (((JCRNodeWrapper) actions.nextNode()).isNodeType(SYNC_ACTION)) {
                return true;
            }
        }
        return false;
    }

    static FormDefinition read(JCRNodeWrapper form) throws RepositoryException {
        List<FormDefinition.Field> fields = new ArrayList<>();
        if (form.hasNode(FIELDS)) {
            collect(form.getNode(FIELDS), fields);
        }
        return new FormDefinition(form.getName(), List.copyOf(fields));
    }

    private static void collect(JCRNodeWrapper container, List<FormDefinition.Field> fields) throws RepositoryException {
        NodeIterator children = container.getNodes();
        while (children.hasNext()) {
            JCRNodeWrapper child = (JCRNodeWrapper) children.nextNode();
            if (child.isNodeType(FmdbMixin.SUBMITTABLE_FIELD)) {
                // a file travels as a file, not as a value of the system's payload
                if (!child.isNodeType(FmdbMixin.FILE_FIELD)) {
                    fields.add(fieldOf(child));
                }
            } else {
                // a step or a fieldset: walked through, not sent
                collect(child, fields);
            }
        }
    }

    private static FormDefinition.Field fieldOf(JCRNodeWrapper field) throws RepositoryException {
        boolean mandatory = field.hasProperty(REQUIRED) && field.getProperty(REQUIRED).getBoolean();
        boolean choice = field.isNodeType(FmdbMixin.CHOICE_FIELD);
        boolean manual = !field.hasProperty(FmdbProperty.OPTIONS_MODE)
                || MANUAL_MODE.equals(field.getProperty(FmdbProperty.OPTIONS_MODE).getString());
        List<String> options = choice && manual ? manualOptions(field) : List.of();
        if (choice && !manual) {
            log.warn("[FormDefinitionReader] Field '{}' takes its options from a source: they are not sent to the NNM system.", field.getName());
        }

        String type = FormDefinition.TEXT;
        if (field.isNodeType(FmdbMixin.EMAIL_FIELD)) {
            type = FormDefinition.EMAIL;
        } else if (field.isNodeType(FmdbMixin.CARDINALITY_FROM_CHOICES) && (options.size() > 1 || !manual)) {
            // a checkbox with several choices is a group; with one it is a single box
            type = FormDefinition.MULTISELECT;
        }
        return new FormDefinition.Field(field.getName(), type, mandatory, options);
    }

    /** The codes of the authored options, in order: each stored entry is {@code {"value","label","selected"}}. */
    private static List<String> manualOptions(JCRNodeWrapper field) throws RepositoryException {
        List<String> codes = new ArrayList<>();
        if (!field.hasProperty(FmdbProperty.OPTIONS)) {
            return codes;
        }
        for (Value raw : field.getProperty(FmdbProperty.OPTIONS).getValues()) {
            try {
                String code = new JSONObject(raw.getString()).optString("value", "").trim();
                if (!code.isEmpty()) {
                    codes.add(code);
                }
            } catch (JSONException e) {
                log.debug("[FormDefinitionReader] Unreadable option entry on field '{}', skipped.", field.getName());
            }
        }
        return codes;
    }
}
