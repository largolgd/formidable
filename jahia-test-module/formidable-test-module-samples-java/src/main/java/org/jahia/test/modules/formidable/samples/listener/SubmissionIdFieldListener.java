package org.jahia.test.modules.formidable.samples.listener;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.modules.formidable.engine.api.FmdbNodeName;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Gives every newly created form its submission identifier: a hidden field named {@code <form>-formId} in
 * the form's fields, and a {@code fmdbsample:submissionIdAction} that fills it, placed first in the actions.
 *
 * <p>A form that already carries such an action (a copy, an import) is left as it is: the field and the
 * action it brought along are the ones to keep, whatever the copy is now called.</p>
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class SubmissionIdFieldListener extends DefaultEventListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionIdFieldListener.class);

    static final String FIELD_SUFFIX = "-formId";
    private static final String FIELDS = "fields";
    private static final String ACTION_TYPE = "fmdbsample:submissionIdAction";
    private static final String FIELD_TYPE = "fmdb:inputHidden";

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED;
    }

    @Override
    public String[] getNodeTypes() {
        return new String[]{FmdbMixin.FORM_ROOT};
    }

    @Override
    public void onEvent(EventIterator events) {
        List<String> paths = new ArrayList<>();
        while (events.hasNext()) {
            try {
                paths.add(events.nextEvent().getPath());
            } catch (RepositoryException e) {
                log.warn("[SubmissionId] Failed to read event path: {}", e.getMessage());
            }
        }
        if (paths.isEmpty()) {
            return;
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, session -> {
                boolean changed = false;
                for (String path : paths) {
                    changed |= addTo(session, path);
                }
                if (changed) {
                    session.save();
                }
                return null;
            });
        } catch (RepositoryException e) {
            log.warn("[SubmissionId] Failed to add the submission id field: {}", e.getMessage());
        }
    }

    static boolean addTo(JCRSessionWrapper session, String path) throws RepositoryException {
        JCRNodeWrapper form;
        try {
            form = session.getNode(path);
        } catch (PathNotFoundException e) {
            return false;
        }
        // Translation subnodes are reported with the parent's type filter: only a real form goes on
        if (!form.isNodeType("fmdb:form") || !form.hasNode(FIELDS) || !form.hasNode(FmdbNodeName.ACTIONS)) {
            return false;
        }
        JCRNodeWrapper actions = form.getNode(FmdbNodeName.ACTIONS);
        if (hasChildOfType(actions, ACTION_TYPE)) {
            return false;
        }

        String fieldName = form.getName() + FIELD_SUFFIX;
        JCRNodeWrapper fields = form.getNode(FIELDS);
        if (!fields.hasNode(fieldName)) {
            JCRNodeWrapper field = fields.addNode(fieldName, FIELD_TYPE);
            if (field.hasProperty("jcr:title") || field.isNodeType("mix:title")) {
                field.setProperty("jcr:title", fieldName);
            }
        }

        // First of the list: the actions run in order, and the ones after it read the value it writes
        JCRNodeWrapper action = actions.addNode(fieldName + "-action", ACTION_TYPE);
        action.setProperty("fieldName", fieldName);
        action.setProperty("jcr:title", "Submission id");
        NodeIterator siblings = actions.getNodes();
        if (siblings.hasNext()) {
            String first = siblings.nextNode().getName();
            if (!first.equals(action.getName())) {
                actions.orderBefore(action.getName(), first);
            }
        }
        log.debug("[SubmissionId] Added '{}' to '{}'", fieldName, path);
        return true;
    }

    private static boolean hasChildOfType(JCRNodeWrapper parent, String type) throws RepositoryException {
        NodeIterator it = parent.getNodes();
        while (it.hasNext()) {
            if (it.nextNode().isNodeType(type)) {
                return true;
            }
        }
        return false;
    }
}
