package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Publication listener: live workspace, under {@code /sites}. Publication writes {@code j:lastPublished}
 * on every node it publishes, the one event worth a look; the nearest form above the marked node is
 * handed to the {@link ConfigSynchronizer}, which decides whether it is connected and whether it changed.
 * Marked available during publication, without which Jahia never delivers publication events to a listener.
 * The pattern is the jExperience module's mapping-rule listener, without its removal handling.
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class PublicationListener extends DefaultEventListener {

    static final String SCOPE = "/sites";
    static final String PUBLICATION_MARK = "j:lastPublished";

    private static final Logger log = LoggerFactory.getLogger(PublicationListener.class);

    @Reference
    private ConfigSynchronizer synchronizer;

    public PublicationListener() {
        setWorkspace(ConfigSynchronizer.WORKSPACE_LIVE);
        setAvailableDuringPublish(true);
    }

    @Override
    public int getEventTypes() {
        return Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED;
    }

    @Override
    public String getPath() {
        return SCOPE;
    }

    @Override
    public void onEvent(EventIterator events) {
        Set<String> paths = new LinkedHashSet<>();
        while (events.hasNext()) {
            try {
                String path = events.nextEvent().getPath();
                if (path.endsWith("/" + PUBLICATION_MARK)) {
                    paths.add(parentPath(path));
                }
            } catch (RepositoryException e) {
                log.debug("[PublicationListener] Skipped an event: {}", e.getMessage());
            }
        }
        if (paths.isEmpty()) {
            return;
        }
        try {
            Set<String> forms = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, ConfigSynchronizer.WORKSPACE_LIVE, null, session -> {
                Set<String> found = new LinkedHashSet<>();
                for (String path : paths) {
                    String form = nearestForm(session, path);
                    if (form != null) {
                        found.add(form);
                    }
                }
                return found;
            });
            forms.forEach(uuid -> synchronizer.syncLater(uuid, false));
        } catch (RepositoryException e) {
            log.warn("[PublicationListener] Could not resolve the forms of a publication: {}", e.getMessage());
        }
    }

    /** The identifier of the nearest form at or above the path, read in live; null when there is none. */
    static String nearestForm(org.jahia.services.content.JCRSessionWrapper session, String path) throws RepositoryException {
        String current = path;
        while (current != null && current.startsWith(SCOPE + "/")) {
            try {
                JCRNodeWrapper node = session.getNode(current);
                if (node.isNodeType(FmdbMixin.FORM_ROOT)) {
                    return node.getIdentifier();
                }
            } catch (PathNotFoundException e) {
                // gone with the publication: keep climbing
            }
            current = parentPath(current);
        }
        return null;
    }

    static String parentPath(String path) {
        int slash = path.lastIndexOf('/');
        return slash > 0 ? path.substring(0, slash) : null;
    }
}
