package org.jahia.test.modules.formidable.samples.actions.form;

import org.jahia.modules.formidable.engine.api.FormAction;
import org.jahia.modules.formidable.engine.api.FormActionException;
import org.jahia.modules.formidable.engine.api.SubmittedFile;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.mail.MailMessage;
import org.jahia.services.mail.MailService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sends the submitted form content by email, like Formidable's {@code fmdb:emailContentAction}, to the
 * addresses of the {@code fmdbsample:contactFormOption} nodes the contributor picked on the action.
 *
 * <p>The engine's own action is not reachable from another module (its helpers are internal), so this
 * sample writes the few steps out: the mail service, the addresses the service would refuse left out, a
 * table of the submitted values. It reads the picked options in the live workspace, which is where the
 * pipeline runs actions: an option that is not published cannot be resolved and is logged, not sent to.</p>
 *
 * <p>A message with no valid recipient is not sent and the action returns, so that the actions after it
 * (Save to JCR among them) still run.</p>
 */
@Component(service = FormAction.class)
public class EmailContentToContactFormAction implements FormAction {

    private static final Logger log = LoggerFactory.getLogger(EmailContentToContactFormAction.class);

    private static final String DEFAULT_SUBJECT = "Form submission";

    /**
     * A bare address: a local part, then a domain of dot-separated labels with no empty label, so the
     * typo the mail service refuses later (a domain ending with a dot) is refused here. Deliberately
     * simpler than the service's own parse, which would need the mail library on this module's side.
     */
    private static final Pattern ADDRESS = Pattern.compile(
            "[^@\\s<>,;\"()\\[\\]]+@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+");

    private MailService mailService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unbindMailService")
    protected void bindMailService(MailService service) {
        this.mailService = service;
    }

    protected void unbindMailService(MailService service) {
        if (this.mailService == service) {
            this.mailService = null;
        }
    }

    @Override
    public String getNodeType() {
        return "fmdbsample:emailContentToContactAction";
    }

    @Override
    public void execute(
            JCRNodeWrapper actionNode,
            HttpServletRequest req,
            JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            List<SubmittedFile> files
    ) throws FormActionException {
        if (mailService == null || !mailService.isEnabled()) {
            throw FormActionException.serverError("MailService is unavailable or disabled. Check Jahia SMTP configuration.");
        }

        List<String> recipients = recipients(actionNode);
        if (recipients.isEmpty()) {
            log.warn("[Formidable sample] '{}': no valid address in the contact options picked, nothing sent", actionNode.getPath());
            return;
        }

        String subject = headerSafe(formTitle(actionNode));
        String from = headerSafe(stringProperty(actionNode, "from"));

        MailMessage message = new MailMessage();
        message.setTo(String.join(", ", recipients));
        message.setFrom(isValid(from) ? from : null);
        message.setSubject(subject.isBlank() ? DEFAULT_SUBJECT : subject);
        message.setTextBody(textBody(subject, parameters));
        message.setHtmlBody(htmlBody(subject, parameters));

        try {
            // Queued by Jahia's mail service: a delivery failure later on is logged by it, not raised here.
            mailService.sendMessage(message);
            log.info("[Formidable sample] '{}': content sent to {} recipient(s)", actionNode.getPath(), recipients.size());
        } catch (Exception e) {
            throw FormActionException.serverError("Failed to send form content email to the contacts: " + e.getMessage(), e);
        }
    }

    /**
     * The addresses of every option picked on the action, then the ones typed in {@code customRecipients},
     * valid ones only, each once. Either source may be empty; the caller sends nothing when both are.
     */
    private static List<String> recipients(JCRNodeWrapper actionNode) throws FormActionException {
        List<String> raw = new ArrayList<>();
        try {
            if (actionNode.hasProperty("contacts")) {
                for (JCRValueWrapper reference : actionNode.getProperty("contacts").getValues()) {
                    JCRNodeWrapper option;
                    try {
                        option = reference.getNode();
                    } catch (RepositoryException e) {
                        log.warn("[Formidable sample] '{}': a contact option cannot be read in the live workspace, is it published? {}",
                                actionNode.getPath(), e.getMessage());
                        continue;
                    }
                    if (option != null && option.hasProperty("emails")) {
                        for (JCRValueWrapper email : option.getProperty("emails").getValues()) {
                            raw.add(email.getString());
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw FormActionException.serverError("Could not read the contact options of '" + actionNode.getPath() + "'", e);
        }
        raw.addAll(splitList(stringProperty(actionNode, "customRecipients")));
        return validAddresses(raw);
    }

    /** The entries of a typed list, comma-separated like the addresses of Formidable's own e-mail actions. */
    static List<String> splitList(String list) {
        List<String> entries = new ArrayList<>();
        if (list != null) {
            for (String entry : list.split(",")) {
                if (!entry.isBlank()) {
                    entries.add(entry.trim());
                }
            }
        }
        return entries;
    }

    /**
     * The entries the mail service accepts, trimmed and without duplicates (case-insensitive), in order.
     * The service parses strictly and refuses a bad address later, on a queue that reports nothing back:
     * the entries it would refuse are left out here and logged.
     */
    static List<String> validAddresses(List<String> raw) {
        Map<String, String> kept = new LinkedHashMap<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String address = headerSafe(entry);
            if (!isValid(address)) {
                log.warn("[Formidable sample] address left out, the mail service would refuse it: {}", address);
                continue;
            }
            kept.putIfAbsent(address.toLowerCase(Locale.ROOT), address);
        }
        return new ArrayList<>(kept.values());
    }

    static boolean isValid(String address) {
        return address != null && ADDRESS.matcher(address.trim()).matches();
    }

    /** No line break survives into a header: it would let a value start a header of its own. */
    static String headerSafe(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String stringProperty(JCRNodeWrapper node, String name) {
        try {
            return node.hasProperty(name) ? node.getProperty(name).getString() : "";
        } catch (RepositoryException e) {
            return "";
        }
    }

    private static String formTitle(JCRNodeWrapper actionNode) {
        try {
            JCRNodeWrapper form = actionNode.getParent().getParent();
            String title = stringProperty(form, "jcr:title");
            if (!title.isBlank()) {
                return title;
            }
            String name = form.getDisplayableName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (RepositoryException | RuntimeException e) {
            log.debug("Could not resolve the form title from '{}': {}", actionNode.getPath(), e.getMessage());
        }
        return DEFAULT_SUBJECT;
    }

    private static String textBody(String subject, Map<String, List<String>> parameters) {
        StringBuilder body = new StringBuilder();
        if (!subject.isBlank()) {
            body.append(subject).append("\n\n");
        }
        parameters.forEach((name, values) ->
                body.append(name).append(": ").append(values == null ? "" : String.join(", ", values)).append("\n"));
        return body.toString();
    }

    private static String htmlBody(String subject, Map<String, List<String>> parameters) {
        StringBuilder body = new StringBuilder("<html><body><h2>").append(html(subject)).append("</h2>")
                .append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\">");
        parameters.forEach((name, values) -> body.append("<tr><th align=\"left\">").append(html(name)).append("</th><td>")
                .append(values == null ? "" : values.stream().map(EmailContentToContactFormAction::html).collect(Collectors.joining("<br/>")))
                .append("</td></tr>"));
        return body.append("</table></body></html>").toString();
    }

    /** Every value is the visitor's: escaped before it goes in markup. */
    private static String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
