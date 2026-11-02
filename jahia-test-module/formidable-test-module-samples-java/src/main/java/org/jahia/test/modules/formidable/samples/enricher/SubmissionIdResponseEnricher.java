package org.jahia.test.modules.formidable.samples.enricher;

import org.jahia.modules.formidable.engine.api.AcceptedSubmission;
import org.jahia.modules.formidable.engine.api.FmdbNodeName;
import org.jahia.modules.formidable.engine.api.SubmissionResponseEnricher;
import org.jahia.services.content.JCRNodeWrapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

/**
 * Adds the identifier made by {@code SubmissionIdFormAction} to the JSON body of an accepted
 * submission: {@code {"success": true, "submissionId": "20261026-143012-4F2A"}}, for the page's own
 * scripts to show the visitor a reference.
 *
 * <p>It answers only for a form whose action list holds a {@code fmdbsample:submissionIdAction}: it
 * reads the hidden field's name off that action and the value off the accepted parameters, where the
 * action put it. Any other form's response stays as it was.</p>
 */
@Component(service = SubmissionResponseEnricher.class)
public class SubmissionIdResponseEnricher implements SubmissionResponseEnricher {

    /** The top-level key of the body, named after what it carries. */
    public static final String KEY = "submissionId";

    private static final String ACTION_TYPE = "fmdbsample:submissionIdAction";
    private static final String FIELD_NAME = "fieldName";

    private static final Logger logger = LoggerFactory.getLogger(SubmissionIdResponseEnricher.class);

    @Override
    public Map<String, Object> enrich(AcceptedSubmission submission) {
        try {
            JCRNodeWrapper form = submission.formNode();
            if (!form.hasNode(FmdbNodeName.ACTIONS)) {
                return Map.of();
            }
            for (JCRNodeWrapper action : form.getNode(FmdbNodeName.ACTIONS).getNodes()) {
                if (!action.isNodeType(ACTION_TYPE) || !action.hasProperty(FIELD_NAME)) {
                    continue;
                }
                List<String> values = submission.parameters().get(action.getProperty(FIELD_NAME).getString());
                if (values != null && !values.isEmpty()) {
                    return Map.of(KEY, values.get(0));
                }
            }
        } catch (RepositoryException e) {
            // an enricher never fails a submission: the actions have run, so this costs its own entry
            logger.warn("Formidable sample enricher could not read the submission id action: no id added", e);
        }
        return Map.of();
    }
}
