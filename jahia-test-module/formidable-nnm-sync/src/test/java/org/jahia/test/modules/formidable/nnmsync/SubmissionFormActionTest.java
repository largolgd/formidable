package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.jahia.test.modules.formidable.nnmsync.Nodes.child;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.node;
import static org.jahia.test.modules.formidable.nnmsync.Nodes.withChildren;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionFormActionTest {

    private final SyncState state = mock(SyncState.class);
    private final SubmissionDispatcher dispatcher = mock(SubmissionDispatcher.class);
    private final ConfigSynchronizer synchronizer = mock(ConfigSynchronizer.class);
    private final SubmissionFormAction action = new SubmissionFormAction(
            Clock.fixed(Instant.parse("2026-10-21T14:32:05Z"), ZoneOffset.UTC), state, dispatcher, synchronizer);

    private final JCRSessionWrapper session = mock(JCRSessionWrapper.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private JCRNodeWrapper actionNode;

    private void formWithOneEmailField() throws Exception {
        JCRNodeWrapper email = node("email", FmdbMixin.SUBMITTABLE_FIELD, FmdbMixin.EMAIL_FIELD);
        JCRNodeWrapper form = child(node("contact", FmdbMixin.FORM_ROOT), "fields", withChildren(node("fields"), email));
        when(form.getIdentifier()).thenReturn("uuid-1");
        actionNode = node("nnm", FormDefinitionReader.SYNC_ACTION);
        JCRNodeWrapper list = node("actions");
        when(actionNode.getParent()).thenReturn(list);
        when(list.getParent()).thenReturn(form);
        when(session.getLocale()).thenReturn(Locale.FRENCH);
        when(request.getHeader("Referer")).thenReturn("https://www.example.com/fr/contact");
    }

    @Test
    void queuesTheSubmissionWithTheVersionTheSystemKnows() throws Exception {
        formWithOneEmailField();
        when(state.get("uuid-1")).thenReturn(Optional.of(new SyncState.Entry(4, "h")));

        action.execute(actionNode, request, session, Map.of("email", List.of("john@doe.com")), List.of());

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(dispatcher).send(eq("uuid-1"), anyString(), json.capture());
        JSONObject body = new JSONObject(json.getValue());
        assertEquals("contact", body.getString("configId"));
        assertEquals(4, body.getInt("configVersion"));
        assertEquals("fr", body.getString("locale"));
        assertEquals("https://www.example.com/fr/contact", body.getString("sourceUrl"));
    }

    @Test
    void aFormNeverSentSendsNothingAndAsksForItsConfiguration() throws Exception {
        formWithOneEmailField();
        when(state.get("uuid-1")).thenReturn(Optional.empty());

        action.execute(actionNode, request, session, Map.of("email", List.of("john@doe.com")), List.of());

        verify(dispatcher, never()).send(anyString(), anyString(), anyString());
        verify(synchronizer).syncLater("uuid-1", true);
    }

    @Test
    void nothingThatGoesWrongReachesTheVisitor() throws Exception {
        formWithOneEmailField();
        when(state.get(any())).thenThrow(new RepositoryException("down"));

        assertDoesNotThrow(() -> action.execute(actionNode, request, session, Map.of(), List.of()));
    }
}
