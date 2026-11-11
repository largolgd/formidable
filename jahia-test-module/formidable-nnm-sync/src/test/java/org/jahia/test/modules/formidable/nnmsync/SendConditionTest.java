package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.services.content.JCRNodeWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.jahia.test.modules.formidable.nnmsync.Nodes.node;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendConditionTest {

    private static final SendCondition CONTAINS_2 = new SendCondition("field1", SendCondition.Operator.CONTAINS, "2");
    private static final SendCondition EQUALS_2 = new SendCondition("field1", SendCondition.Operator.EQUALS, "2");

    @Test
    void noRuleAllowsEverything() throws Exception {
        assertNull(SendCondition.read(node("nnm")));
        assertTrue(SendCondition.allows(null, Map.of()));
    }

    @Test
    void containsMatchesPartOfTheValue() {
        assertTrue(CONTAINS_2.isSatisfiedBy(Map.of("field1", List.of("12"))));
        assertFalse(CONTAINS_2.isSatisfiedBy(Map.of("field1", List.of("13"))));
    }

    @Test
    void equalsMatchesTheWholeValueWithoutRegardToCase() {
        assertTrue(new SendCondition("f", SendCondition.Operator.EQUALS, "Yes").isSatisfiedBy(Map.of("f", List.of("yes"))));
        assertFalse(EQUALS_2.isSatisfiedBy(Map.of("field1", List.of("12"))));
    }

    @Test
    void oneValueAmongSeveralIsEnough() {
        assertTrue(EQUALS_2.isSatisfiedBy(Map.of("field1", List.of("1", "2"))));
    }

    @Test
    void aFieldTheSubmissionDoesNotCarrySatisfiesNothing() {
        assertFalse(CONTAINS_2.isSatisfiedBy(Map.of("other", List.of("2"))));
    }

    @Test
    void readsTheFieldFromTheAction() throws Exception {
        JCRNodeWrapper action = string(node("nnm"), SendCondition.FIELD, " field1 ");
        SendCondition read = SendCondition.read(action);
        assertEquals("field1", read.field());
    }
}
