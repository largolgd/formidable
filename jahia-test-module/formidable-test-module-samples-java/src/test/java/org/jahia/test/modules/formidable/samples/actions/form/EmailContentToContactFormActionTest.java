package org.jahia.test.modules.formidable.samples.actions.form;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
