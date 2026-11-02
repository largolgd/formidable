package org.jahia.test.modules.formidable.samples.actions.form;

import org.jahia.modules.formidable.engine.api.FormActionException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmissionIdFormActionTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T14:30:12Z"), ZoneOffset.UTC);

    private SubmissionIdFormAction action(int... digits) {
        // A Random whose nextInt(16) answers the given digits in turn
        int[] next = {0};
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                return digits[next[0]++ % digits.length];
            }
        };
        return new SubmissionIdFormAction(clock, random);
    }

    @Test
    void generatesTheDocumentedFormat() {
        assertEquals("20261026-143012-4F2A", action(4, 15, 2, 10).generate());
    }

    @Test
    void overwritesWhatTheBrowserSent() throws Exception {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("submissionId", List.of("forged", "forged-too"));

        action(0, 0, 0, 1).assign("submissionId", parameters);

        assertEquals(List.of("20261026-143012-0001"), parameters.get("submissionId"));
    }

    @Test
    void failsWithoutAFieldName() {
        assertThrows(FormActionException.class,
                () -> action(0).assign(" ", new HashMap<>()));
    }
}
