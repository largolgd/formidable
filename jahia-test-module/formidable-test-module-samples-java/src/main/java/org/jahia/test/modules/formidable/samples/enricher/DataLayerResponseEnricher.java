package org.jahia.test.modules.formidable.samples.enricher;

import org.jahia.modules.formidable.engine.api.AcceptedSubmission;
import org.jahia.modules.formidable.engine.api.FmdbNodeName;
import org.jahia.modules.formidable.engine.api.SubmissionResponseEnricher;
import org.jahia.services.content.JCRNodeWrapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds the entry a tag manager wants to the JSON body of an accepted submission:
 * {@code {"dataLayer": {"event": "form_completed", "form_id": "contact"}}}, {@code form_id} being the
 * system name of the submitted form.
 *
 * <p>The server sends data, not script: the page listens to the {@code formidable:submitted} event the
 * form island dispatches and pushes {@code response.dataLayer} into {@code window.dataLayer} (see
 * docs/extension/how-to-enrich-the-submission-response.md). Running code the server sent would need an
 * {@code eval}, which a content security policy forbids and which makes the response an injection point.</p>
 *
 * <p>Like {@link SubmissionIdResponseEnricher}, it answers only for a form whose action list holds a
 * {@code fmdbsample:submissionIdAction}, which every form created while the samples module is deployed carries.</p>
 */
@Component(service = SubmissionResponseEnricher.class)
public class DataLayerResponseEnricher implements SubmissionResponseEnricher {

    /** The top-level key of the body, named after what it carries. */
    public static final String KEY = "dataLayer";
    public static final String EVENT = "form_completed";

    private static final String ACTION_TYPE = "fmdbsample:submissionIdAction";

    private static final Logger logger = LoggerFactory.getLogger(DataLayerResponseEnricher.class);

    @Override
    public Map<String, Object> enrich(AcceptedSubmission submission) {
        try {
            JCRNodeWrapper form = submission.formNode();
            if (hasSubmissionIdAction(form)) {
                return Map.of(KEY, entry(form.getName()));
            }
        } catch (RepositoryException e) {
            // an enricher never fails a submission: the actions have run, so this costs its own entry
            logger.warn("Formidable sample enricher could not read the form's actions: no dataLayer entry added", e);
        }
        return Map.of();
    }

    static Map<String, Object> entry(String formName) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("event", EVENT);
        entry.put("form_id", formName);
        return entry;
    }

    private static boolean hasSubmissionIdAction(JCRNodeWrapper form) throws RepositoryException {
        if (!form.hasNode(FmdbNodeName.ACTIONS)) {
            return false;
        }
        for (JCRNodeWrapper action : form.getNode(FmdbNodeName.ACTIONS).getNodes()) {
            if (action.isNodeType(ACTION_TYPE)) {
                return true;
            }
        }
        return false;
    }
}
