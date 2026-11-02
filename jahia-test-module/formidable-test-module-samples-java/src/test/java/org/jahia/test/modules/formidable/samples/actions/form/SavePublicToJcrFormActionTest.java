package org.jahia.test.modules.formidable.samples.actions.form;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavePublicToJcrFormActionTest {

    @Test
    void privateFieldsAreDroppedFromWhatIsHandedToTheEngine() {
        Map<String, List<String>> submitted = Map.of(
                "email", List.of("a@b.c"), "name", List.of("Ada"), "city", List.of("Paris"));

        Map<String, List<String>> stored = SavePublicToJcrFormAction.withoutPrivate(submitted, Set.of("email", "name"));

        assertEquals(Map.of("city", List.of("Paris")), stored);
        // The received map is untouched: the other actions still get every value.
        assertEquals(3, submitted.size());
    }

    @Test
    void withoutPrivateFieldsEverythingIsHandedOver() {
        Map<String, List<String>> submitted = Map.of("email", List.of("a@b.c"));

        assertEquals(submitted, SavePublicToJcrFormAction.withoutPrivate(submitted, Set.of()));
    }
}
