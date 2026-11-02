package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.services.content.JCRTemplate;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Optional;

/**
 * What the NNM system last accepted for each form: the version it knows and the hash of the
 * content of that version. Read at each submission (the version goes out with it) and at each
 * publication (an unchanged hash sends nothing).
 *
 * <p>Kept in the live workspace only, under {@code /nnmSync/<form identifier>}, and not on the form:
 * a publication copies the properties of the default workspace over the live ones, so a counter
 * written on the form would be undone by the next publication. Written only once the system has
 * answered 2xx, so a failed send leaves the previous version in force and the retry reuses the
 * same number.</p>
 */
public interface SyncState {

    record Entry(int version, String hash) {
    }

    Optional<Entry> get(String formUuid) throws RepositoryException;

    void put(String formUuid, Entry entry) throws RepositoryException;

    @Component(service = SyncState.class)
    class InLive implements SyncState {

        static final String WORKSPACE_LIVE = "live";
        static final String ROOT = "nnmSync";
        static final String VERSION = "version";
        static final String HASH = "hash";
        private static final String NODE_TYPE = "nt:unstructured";

        @Override
        public Optional<Entry> get(String formUuid) throws RepositoryException {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, WORKSPACE_LIVE, null, session -> {
                String path = "/" + ROOT + "/" + formUuid;
                if (!session.nodeExists(path)) {
                    return Optional.<Entry>empty();
                }
                Node node = session.getNode(path);
                if (!node.hasProperty(VERSION) || !node.hasProperty(HASH)) {
                    return Optional.<Entry>empty();
                }
                return Optional.of(new Entry((int) node.getProperty(VERSION).getLong(), node.getProperty(HASH).getString()));
            });
        }

        @Override
        public void put(String formUuid, Entry entry) throws RepositoryException {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, WORKSPACE_LIVE, null, session -> {
                Node root = session.getRootNode();
                Node folder = root.hasNode(ROOT) ? root.getNode(ROOT) : root.addNode(ROOT, NODE_TYPE);
                Node node = folder.hasNode(formUuid) ? folder.getNode(formUuid) : folder.addNode(formUuid, NODE_TYPE);
                node.setProperty(VERSION, (long) entry.version());
                node.setProperty(HASH, entry.hash());
                session.save();
                return null;
            });
        }
    }
}
