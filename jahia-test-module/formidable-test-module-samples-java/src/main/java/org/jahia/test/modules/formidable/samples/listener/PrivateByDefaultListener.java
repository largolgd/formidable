package org.jahia.test.modules.formidable.samples.listener;

import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.modules.formidable.samples.actions.form.SavePublicToJcrFormAction;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

/**
 * Makes the text, textarea and email fields private by default: when one is created in the editing
 * workspace, the listener turns on its "Private field" switch ({@code fmdbsamplemix:privateData}).
 * <p>
 * A default, not a rule: the author can switch it off on a field that may be stored, and the listener
 * never touches that field again. It only listens to creations in the editing workspace; in live, a
 * field that arrives by publication already carries the author's choice, and adding the mixin there
 * would undo an author's switch-off. A field copied or imported is a creation too, so it comes with the
 * switch on, whatever the original's.
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class PrivateByDefaultListener extends DefaultEventListener {

    private static final Logger log = LoggerFactory.getLogger(PrivateByDefaultListener.class);

    static final String[] FIELD_TYPES = {"fmdb:inputText", "fmdb:textarea", "fmdb:inputEmail"};
    private static final Set<String> FIELD_TYPE_SET = Set.of(FIELD_TYPES);
    private static final String EDIT_WORKSPACE = "default";

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED;
    }

    @Override
    public String[] getNodeTypes() {
        return FIELD_TYPES;
    }

    @Override
    public String getWorkspace() {
        return EDIT_WORKSPACE;
    }

    @Override
    public void onEvent(EventIterator events) {
        // An import or a subtree copy delivers one event per node: one session and one save for the batch.
        List<String> paths = new ArrayList<>();
        while (events.hasNext()) {
            try {
                paths.add(events.nextEvent().getPath());
            } catch (RepositoryException e) {
                log.warn("[PrivateByDefault] Failed to read an event path: {}", e.getMessage());
            }
        }
        if (paths.isEmpty()) {
            return;
        }

        try {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, EDIT_WORKSPACE, null, session -> {
                boolean changed = false;
                for (String path : paths) {
                    changed |= markPrivate(session, path);
                }
                if (changed) {
                    session.save();
                }
                return null;
            });
        } catch (RepositoryException e) {
            log.warn("[PrivateByDefault] Failed to mark the new fields private: {}", e.getMessage());
        }
    }

    /**
     * Adds the mixin to the node at the path when it is one of the three field types — the type filter
     * of Jahia's observation also lets the node's translation sub-nodes through — and does not carry it.
     */
    private static boolean markPrivate(JCRSessionWrapper session, String path) throws RepositoryException {
        JCRNodeWrapper node;
        try {
            node = session.getNode(path);
        } catch (PathNotFoundException e) {
            return false;
        }
        if (!FIELD_TYPE_SET.contains(node.getPrimaryNodeTypeName())
                || node.isNodeType(SavePublicToJcrFormAction.PRIVATE_DATA_MIXIN)) {
            return false;
        }
        session.checkout(node);
        node.addMixin(SavePublicToJcrFormAction.PRIVATE_DATA_MIXIN);
        log.debug("[PrivateByDefault] '{}' is private by default", path);
        return true;
    }
}
