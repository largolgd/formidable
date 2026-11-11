package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The rule an author may set on the sync action so that a submission is sent only when it is satisfied:
 * "the field {@code conditionField} contains (or equals) {@code conditionValue}". The field is named by
 * its node name, which is the name its value is submitted under and the {@code key} the third party knows.
 *
 * <p>No field named means no rule: every submission goes. A field the submission does not carry (left
 * out, or hidden by a conditional display rule) satisfies nothing. A field with several values (check
 * boxes) is satisfied when one of them is. Comparison ignores case.</p>
 */
record SendCondition(String field, Operator operator, String value) {

    static final String FIELD = "conditionField";
    static final String OPERATOR = "conditionOperator";
    static final String VALUE = "conditionValue";

    enum Operator {
        CONTAINS, EQUALS;

        static Operator of(String stored) {
            return "equals".equalsIgnoreCase(stored) ? EQUALS : CONTAINS;
        }
    }

    /** The rule stored on the action, or {@code null} when it has none. */
    static SendCondition read(JCRNodeWrapper actionNode) throws RepositoryException {
        String field = string(actionNode, FIELD).trim();
        if (field.isEmpty()) {
            return null;
        }
        return new SendCondition(field, Operator.of(string(actionNode, OPERATOR)), string(actionNode, VALUE));
    }

    /** True when the submission may go: a missing rule always allows it. */
    static boolean allows(SendCondition condition, Map<String, List<String>> parameters) {
        return condition == null || condition.isSatisfiedBy(parameters);
    }

    boolean isSatisfiedBy(Map<String, List<String>> parameters) {
        List<String> submitted = parameters.get(field);
        if (submitted == null) {
            return false;
        }
        String wanted = value.toLowerCase(Locale.ROOT);
        for (String candidate : submitted) {
            String actual = candidate == null ? "" : candidate.toLowerCase(Locale.ROOT);
            if (operator == Operator.EQUALS ? actual.equals(wanted) : actual.contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static String string(JCRNodeWrapper node, String property) throws RepositoryException {
        return node.hasProperty(property) ? node.getProperty(property).getString() : "";
    }
}
