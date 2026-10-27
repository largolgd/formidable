package org.jahia.test.modules.formidable.samples.actions.form;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailContentToContactFormActionTest {

    @Test
    void keepsValidAddressesOnceInOrder() {
        List<String> raw = Arrays.asList("sales@example.com", " Sales@Example.com ", "support@example.com", null, " ");
        assertEquals(List.of("sales@example.com", "support@example.com"), EmailContentToContactFormAction.validAddresses(raw));
    }

    @Test
    void leavesOutWhatTheMailServiceWouldRefuse() {
        // the typo the strict parse refuses: a domain ending with a dot
        List<String> raw = List.of("no-reply@example.com.", "not an address", "ok@example.com");
        assertEquals(List.of("ok@example.com"), EmailContentToContactFormAction.validAddresses(raw));
    }

    @Test
    void acceptsABareAddressButNotABlankOrNamedOne() {
        assertTrue(EmailContentToContactFormAction.isValid("team@example.com"));
        assertFalse(EmailContentToContactFormAction.isValid(""));
        assertFalse(EmailContentToContactFormAction.isValid("Team <team@example.com>"));
    }

    @Test
    void keepsLineBreaksOutOfHeaders() {
        assertEquals("a b", EmailContentToContactFormAction.headerSafe("a\r\nb"));
    }

    @Test
    void splitsATypedListOnCommasOnly() {
        assertEquals(List.of("a@example.com", "b@example.com"),
                EmailContentToContactFormAction.splitList(" a@example.com, b@example.com ,, "));
        // a semicolon is not a separator: the entry stays whole and is refused as an address
        assertEquals(List.of("a@example.com;b@example.com"),
                EmailContentToContactFormAction.splitList("a@example.com;b@example.com"));
        assertEquals(List.of(), EmailContentToContactFormAction.splitList(null));
    }

    private static String rule(String sourceNodeId, String fieldName, String value, String contactId) {
        return "{\"sourceNodeId\":\"" + sourceNodeId + "\",\"fieldName\":\"" + fieldName + "\",\"value\":\"" + value
                + "\",\"contactId\":\"" + contactId + "\"}";
    }

    @Test
    void readsTheRoutesAndLeavesOutTheOnesThatRouteNothing() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(Arrays.asList(
                rule("f1", "topic", "sales", "c1"),
                rule("", "topic", "support", "c2"),     // named by its stored name only: still a route
                rule("f1", "topic", "", "c3"),          // no value
                rule("f1", "topic", "x", ""),           // no contact
                rule("", "", "x", "c4"),                // no field
                "{not json", " ", null));
        assertEquals(List.of("c1", "c2"), routes.stream().map(EmailContentToContactFormAction.Route::contactId).toList());
    }

    @Test
    void aRouteMatchesWhenItsFieldHoldsItsValue() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(List.of(
                rule("", "topic", "sales", "c1"), rule("", "topic", "support", "c2"), rule("", "country", "fr", "c3")));
        Map<String, List<String>> submitted = Map.of("topic", List.of("sales"), "country", List.of("de"));
        assertEquals(List.of("c1"),
                EmailContentToContactFormAction.matchedContactIds(routes, EmailContentToContactFormAction.Route::fieldName, submitted));
    }

    @Test
    void aFieldWithSeveralValuesMatchesOnAnyOfThem() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(List.of(rule("", "topics", "b", "c1")));
        assertEquals(List.of("c1"), EmailContentToContactFormAction.matchedContactIds(
                routes, EmailContentToContactFormAction.Route::fieldName, Map.of("topics", List.of("a", "b"))));
    }

    @Test
    void aFieldTheSubmissionDoesNotCarryMatchesNothing() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(List.of(rule("", "topic", "sales", "c1")));
        assertEquals(List.of(), EmailContentToContactFormAction.matchedContactIds(
                routes, EmailContentToContactFormAction.Route::fieldName, Map.of("other", List.of("sales"))));
    }

    @Test
    void aContactRoutedTwiceIsListedOnceInTheOrderOfTheRules() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(List.of(
                rule("", "topic", "sales", "c2"), rule("", "country", "fr", "c1"), rule("", "country", "fr", "c2")));
        Map<String, List<String>> submitted = Map.of("topic", List.of("sales"), "country", List.of("fr"));
        assertEquals(List.of("c2", "c1"),
                EmailContentToContactFormAction.matchedContactIds(routes, EmailContentToContactFormAction.Route::fieldName, submitted));
    }

    @Test
    void theValueIsComparedAsIsNotAsAPattern() {
        List<EmailContentToContactFormAction.Route> routes = EmailContentToContactFormAction.parseRoutes(List.of(rule("", "topic", "Sales", "c1")));
        assertEquals(List.of(), EmailContentToContactFormAction.matchedContactIds(
                routes, EmailContentToContactFormAction.Route::fieldName, Map.of("topic", List.of("sales"))));
    }

    @Test
    void anAddressKeepsOneRoleWhateverItsCase() {
        List<String> to = List.of("a@example.com");
        List<String> cc = EmailContentToContactFormAction.withoutRepeats(List.of("A@Example.com", "b@example.com"), to);
        List<String> bcc = EmailContentToContactFormAction.withoutRepeats(
                EmailContentToContactFormAction.withoutRepeats(List.of("a@example.com", "B@example.com", "c@example.com"), to), cc);
        assertEquals(List.of("b@example.com"), cc);
        assertEquals(List.of("c@example.com"), bcc);
    }
}
