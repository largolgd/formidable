package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.modules.formidable.engine.api.FormAction;
import org.jahia.modules.formidable.engine.api.FormActionException;
import org.jahia.modules.formidable.engine.api.SubmittedFile;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Hands each submission of a connected form to the {@link SubmissionDispatcher}, which sends it after
 * the visitor has been answered. This action builds the body (a few property reads, no network) and
 * queues it; it never throws, so that nothing the NNM system does, or this module, can fail a submission.
 *
 * <p>The body carries the configuration version the system last accepted
 * ({@link SyncState}). A form that was never synchronised has none: nothing is sent, and its
 * configuration is asked for, so that the next submission can go.</p>
 */
@Component(service = FormAction.class)
public class SubmissionFormAction implements FormAction {

    private static final Logger log = LoggerFactory.getLogger(SubmissionFormAction.class);

    private final Clock clock;

    @Reference
    private SyncState state;

    @Reference
    private SubmissionDispatcher dispatcher;

    @Reference
    private ConfigSynchronizer synchronizer;

    public SubmissionFormAction() {
        this(Clock.systemUTC());
    }

    SubmissionFormAction(Clock clock) {
        this.clock = clock;
    }

    SubmissionFormAction(Clock clock, SyncState state, SubmissionDispatcher dispatcher, ConfigSynchronizer synchronizer) {
        this(clock);
        this.state = state;
        this.dispatcher = dispatcher;
        this.synchronizer = synchronizer;
    }

    @Override
    public String getNodeType() {
        return FormDefinitionReader.SYNC_ACTION;
    }

    @Override
    public void execute(
            JCRNodeWrapper actionNode,
            HttpServletRequest req,
            JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            List<SubmittedFile> files
    ) {
        try {
            // action -> actions list -> form
            JCRNodeWrapper form = actionNode.getParent().getParent();
            if (!form.isNodeType(FmdbMixin.FORM_ROOT)) {
                log.warn("[SubmissionFormAction] Action '{}' is not under a form: nothing sent.", actionNode.getPath());
                return;
            }
            String formUuid = form.getIdentifier();
            SyncState.Entry known = state.get(formUuid).orElse(null);
            if (known == null) {
                log.warn("[SubmissionFormAction] Form '{}' was never sent to the NNM system: this submission is not sent, the configuration is.", form.getName());
                synchronizer.syncLater(formUuid, true);
                return;
            }
            FormDefinition definition = FormDefinitionReader.read(form);
            String submissionId = SubmissionPayload.newSubmissionId();
            String json = SubmissionPayload.build(submissionId, definition, known.version(), clock.instant(),
                    session.getLocale() == null ? "" : session.getLocale().getLanguage(), sourceUrl(req), parameters);
            dispatcher.send(formUuid, submissionId, json);
        } catch (RepositoryException | RuntimeException e) {
            // the visitor must not learn of it: logged, and the submission goes on
            log.error("[SubmissionFormAction] Could not prepare the submission for the NNM system: {}", e.getMessage(), e);
        }
    }

    /** The page the visitor was on: the browser's Referer, or failing that the Origin of the request. */
    private static String sourceUrl(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return referer;
        }
        String origin = req.getHeader("Origin");
        return origin == null ? "" : origin;
    }
}
