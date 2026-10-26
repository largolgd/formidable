package org.jahia.test.modules.formidable.samples.actions.form;

import org.jahia.modules.formidable.engine.api.FormAction;
import org.jahia.modules.formidable.engine.api.FormActionException;
import org.jahia.modules.formidable.engine.api.SubmittedFile;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.osgi.service.component.annotations.Component;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Writes a unique identifier such as {@code 20261026-143012-4F2A} into a hidden field of the form.
 *
 * <p>The value is made here, when the submission arrives, and not in the view: a view is rendered once
 * and cached, so every visitor would receive the same value, and a value made in the browser could be
 * edited or replayed. The pipeline hands every action the same mutable parameter map, so the actions
 * listed after this one (save, email, forward) read the identifier this one put in it. Put this action
 * first in the list. Whatever the browser sent for the field is overwritten.</p>
 */
@Component(service = FormAction.class)
public class SubmissionIdFormAction implements FormAction {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int SUFFIX_LENGTH = 4;

    private final Clock clock;
    private final Random random;

    public SubmissionIdFormAction() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    SubmissionIdFormAction(Clock clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public String getNodeType() {
        return "fmdbsample:submissionIdAction";
    }

    @Override
    public void execute(
            JCRNodeWrapper actionNode,
            HttpServletRequest req,
            JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            List<SubmittedFile> files
    ) throws FormActionException {
        String fieldName;
        try {
            fieldName = actionNode.hasProperty("fieldName") ? actionNode.getProperty("fieldName").getString() : "";
        } catch (RepositoryException e) {
            throw FormActionException.serverError("Could not read the field name of the submission id action", e);
        }
        assign(fieldName, parameters);
    }

    /** The part that needs no repository: puts a fresh identifier in the named field of the submission. */
    void assign(String fieldName, Map<String, List<String>> parameters) throws FormActionException {
        if (fieldName == null || fieldName.isBlank()) {
            throw FormActionException.serverError("The submission id action has no field name");
        }
        // A replaced list, never an added value: a client that sent the field must not end up with two values
        List<String> value = new ArrayList<>(1);
        value.add(generate());
        parameters.put(fieldName, value);
    }

    /** {@code yyyyMMdd-HHmmss-XXXX}, the time in UTC (no ambiguity at a DST change), X an uppercase hex digit. */
    String generate() {
        StringBuilder id = new StringBuilder(STAMP.format(clock.instant().atOffset(ZoneOffset.UTC))).append('-');
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            id.append(Character.toUpperCase(Character.forDigit(random.nextInt(16), 16)));
        }
        return id.toString();
    }
}
