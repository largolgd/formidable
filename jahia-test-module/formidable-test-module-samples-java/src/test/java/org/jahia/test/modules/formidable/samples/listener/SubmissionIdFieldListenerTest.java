package org.jahia.test.modules.formidable.samples.listener;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionIdFieldListenerTest {

    private JCRSessionWrapper session;
    private JCRNodeWrapper form;
    private JCRNodeWrapper fields;
    private JCRNodeWrapper actions;
    private JCRNodeWrapper field;
    private JCRNodeWrapper action;
    private JCRNodeIteratorWrapper actionChildren;

    @BeforeEach
    void setUp() throws RepositoryException {
        session = mock(JCRSessionWrapper.class);
        form = mock(JCRNodeWrapper.class);
        fields = mock(JCRNodeWrapper.class);
        actions = mock(JCRNodeWrapper.class);
        field = mock(JCRNodeWrapper.class);
        action = mock(JCRNodeWrapper.class);
        actionChildren = mock(JCRNodeIteratorWrapper.class);

        when(session.getNode("/sites/s/contact")).thenReturn(form);
        when(form.getName()).thenReturn("contact");
        when(form.isNodeType("fmdb:form")).thenReturn(true);
        when(form.hasNode("fields")).thenReturn(true);
        when(form.hasNode("actions")).thenReturn(true);
        when(form.getNode("fields")).thenReturn(fields);
        when(form.getNode("actions")).thenReturn(actions);
        when(fields.addNode("formId", "fmdb:inputHidden")).thenReturn(field);
        when(actions.addNode("formId-action", "fmdbsample:submissionIdAction")).thenReturn(action);
        when(action.getName()).thenReturn("formId-action");
        when(actions.getNodes()).thenReturn(actionChildren);
    }

    @Test
    void addsTheFormIdFieldAndTheActionFirst() throws RepositoryException {
        JCRNodeWrapper existing = mock(JCRNodeWrapper.class);
        when(existing.getName()).thenReturn("save");
        // first call: the check for an existing action (empty); second: the ordering (one older action)
        JCRNodeIteratorWrapper empty = mock(JCRNodeIteratorWrapper.class);
        when(empty.hasNext()).thenReturn(false);
        when(actionChildren.hasNext()).thenReturn(true);
        when(actionChildren.nextNode()).thenReturn(existing);
        when(actions.getNodes()).thenReturn(empty, actionChildren);

        assertTrue(SubmissionIdFieldListener.addTo(session, "/sites/s/contact"));

        verify(fields).addNode("formId", "fmdb:inputHidden");
        verify(action).setProperty("fieldName", "formId");
        verify(actions).orderBefore("formId-action", "save");
    }

    @Test
    void leavesAFormThatAlreadyHasTheActionAlone() throws RepositoryException {
        JCRNodeWrapper present = mock(JCRNodeWrapper.class);
        when(present.isNodeType("fmdbsample:submissionIdAction")).thenReturn(true);
        when(actionChildren.hasNext()).thenReturn(true, false);
        when(actionChildren.nextNode()).thenReturn(present);

        assertFalse(SubmissionIdFieldListener.addTo(session, "/sites/s/contact"));

        verify(fields, never()).addNode(anyString(), anyString());
    }

    @Test
    void ignoresAPathThatNoLongerResolves() throws RepositoryException {
        when(session.getNode("/gone")).thenThrow(new PathNotFoundException());

        assertFalse(SubmissionIdFieldListener.addTo(session, "/gone"));
    }

    @Test
    void ignoresANodeThatIsNotAForm() throws RepositoryException {
        when(form.isNodeType("fmdb:form")).thenReturn(false);

        assertFalse(SubmissionIdFieldListener.addTo(session, "/sites/s/contact"));
    }
}
