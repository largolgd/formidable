package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.services.content.JCRNodeWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jahia.test.modules.formidable.nnmsync.Nodes.bool;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.child;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.node;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.options;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.string;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.withChildren;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormDefinitionReaderTest {

    private static final String SUBMITTABLE = FmdbMixin.SUBMITTABLE_FIELD;

    @Test
    void walksThroughStepsAndFieldsetsAndFlattensTheSubmittableFields() throws Exception {
        JCRNodeWrapper email = bool(node("email", SUBMITTABLE, FmdbMixin.EMAIL_FIELD), "required", true);
        JCRNodeWrapper phone = node("phone", SUBMITTABLE, FmdbMixin.TEXT_FIELD);
        JCRNodeWrapper fieldset = withChildren(node("contact", FmdbMixin.FORM_CONTAINER), phone);
        JCRNodeWrapper step = withChildren(node("step1", FmdbMixin.FORM_STEP), email, fieldset);
        JCRNodeWrapper button = node("send");
        JCRNodeWrapper form = child(node("contact-us", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), step, button));

        FormDefinition definition = FormDefinitionReader.read(form);

        assertEquals("contact-us", definition.id());
        assertEquals(2, definition.fields().size());
        assertEquals(new FormDefinition.Field("email", "email", true, List.of()), definition.fields().get(0));
        assertEquals(new FormDefinition.Field("phone", "text", false, List.of()), definition.fields().get(1));
    }

    @Test
    void aFileFieldIsNotSent() throws Exception {
        JCRNodeWrapper file = node("cv", SUBMITTABLE, FmdbMixin.FILE_FIELD);
        JCRNodeWrapper form = child(node("f", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), file));

        assertTrue(FormDefinitionReader.read(form).fields().isEmpty());
    }

    @Test
    void aRadioIsATextWithItsOptionCodes() throws Exception {
        JCRNodeWrapper country = options(node("country", SUBMITTABLE, FmdbMixin.CHOICE_FIELD), "CH", "FR");
        JCRNodeWrapper form = child(node("f", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), country));

        FormDefinition.Field field = FormDefinitionReader.read(form).field("country");

        assertEquals("text", field.type());
        assertEquals(List.of("CH", "FR"), field.options());
    }

    @Test
    void aCheckboxGroupIsAMultiselectAndASingleBoxIsText() throws Exception {
        JCRNodeWrapper group = options(node("interests", SUBMITTABLE, FmdbMixin.CHOICE_FIELD, FmdbMixin.CARDINALITY_FROM_CHOICES), "PB", "AU");
        JCRNodeWrapper single = options(node("newsletter", SUBMITTABLE, FmdbMixin.CHOICE_FIELD, FmdbMixin.CARDINALITY_FROM_CHOICES), "yes");
        JCRNodeWrapper form = child(node("f", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), group, single));

        FormDefinition definition = FormDefinitionReader.read(form);

        assertEquals("multiselect", definition.field("interests").type());
        assertEquals("text", definition.field("newsletter").type());
    }

    @Test
    void optionsFromASourceAreNotSent() throws Exception {
        JCRNodeWrapper country = string(node("country", SUBMITTABLE, FmdbMixin.CHOICE_FIELD), "optionsMode", "source");
        JCRNodeWrapper form = child(node("f", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), country));

        assertTrue(FormDefinitionReader.read(form).field("country").options().isEmpty());
    }

    @Test
    void aFormIsConnectedByTheSyncActionOnly() throws Exception {
        JCRNodeWrapper connected = child(node("a", FmdbMixin.FORM_ROOT), "actions",
                withChildren(node("actions"), node("save"), node("nnm", FormDefinitionReader.SYNC_ACTION)));
        JCRNodeWrapper plain = child(node("b", FmdbMixin.FORM_ROOT), "actions", withChildren(node("actions"), node("save")));
        JCRNodeWrapper noActions = node("c", FmdbMixin.FORM_ROOT);

        assertTrue(FormDefinitionReader.carriesSyncAction(connected));
        assertFalse(FormDefinitionReader.carriesSyncAction(plain));
        assertFalse(FormDefinitionReader.carriesSyncAction(noActions));
        assertNull(FormDefinitionReader.read(noActions).field("x"));
    }
}
