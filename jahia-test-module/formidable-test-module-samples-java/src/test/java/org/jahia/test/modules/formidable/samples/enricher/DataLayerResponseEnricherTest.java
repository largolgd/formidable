package org.jahia.test.modules.formidable.samples.enricher;

import org.jahia.modules.formidable.engine.api.AcceptedSubmission;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataLayerResponseEnricherTest {

    private final DataLayerResponseEnricher enricher = new DataLayerResponseEnricher();
    private JCRNodeWrapper form;
    private JCRNodeWrapper actions;
    private JCRNodeWrapper action;
    private JCRNodeIteratorWrapper children;

    @BeforeEach
    void setUp() throws RepositoryException {
        form = mock(JCRNodeWrapper.class);
        actions = mock(JCRNodeWrapper.class);
        action = mock(JCRNodeWrapper.class);
        children = mock(JCRNodeIteratorWrapper.class);
        when(form.getName()).thenReturn("contact");
        when(form.hasNode("actions")).thenReturn(true);
        when(form.getNode("actions")).thenReturn(actions);
        when(actions.getNodes()).thenReturn(children);
        when(children.iterator()).thenAnswer(i -> List.of(action).iterator());
    }

    private Map<String, Object> enrich() {
        return enricher.enrich(new AcceptedSubmission(form, "site", Locale.ENGLISH, Map.of()));
    }

    @Test
    void sendsTheEventAndTheSystemNameOfTheForm() throws RepositoryException {
        when(action.isNodeType("fmdbsample:submissionIdAction")).thenReturn(true);

        assertEquals(Map.of("dataLayer", Map.of("event", "form_completed", "form_id", "contact")), enrich());
    }

    @Test
    void staysSilentForAFormWithoutTheAction() {
        assertTrue(enrich().isEmpty());
    }

    @Test
    void staysSilentForAFormWithoutActions() throws RepositoryException {
        when(form.hasNode("actions")).thenReturn(false);

        assertTrue(enrich().isEmpty());
    }
}
