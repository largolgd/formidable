package org.jahia.test.modules.formidable.samples.prefill;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.osgi.service.component.annotations.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * A Jahia action that sends the visitor to a page holding a Formidable form and hands that page's hidden field a
 * value — in neither the URL nor a cookie of its own: the value waits in the visitor's session until the field asks
 * for it ({@link SessionValueServlet}).
 *
 * <p>POST to {@code <any node>.sampleRedirectWithValue.do} with {@code target} (the identifier of the page to land
 * on — a content reference, not a URL written down), {@code key} (the key the field's {@code sessionKey} names) and
 * {@code value}. Answers a redirect to the target page.</p>
 */
@Component(service = Action.class)
public class RedirectWithValueAction extends Action {

    public static final String NAME = "sampleRedirectWithValue";
    /** Caps what a visitor can park in its session through this sample. */
    static final int MAX_VALUE_LENGTH = 1024;

    public RedirectWithValueAction() {
        setName(NAME);
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String target = first(parameters, "target");
        String key = first(parameters, "key");
        String value = first(parameters, "value");
        if (target == null || key == null || value == null
                || !SessionValueServlet.KEY.matcher(key).matches() || value.length() > MAX_VALUE_LENGTH) {
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST);
        }
        // read in the visitor's own session: a page they cannot read is not found
        JCRNodeWrapper page = session.getNodeByIdentifier(target);
        req.getSession().setAttribute(SessionValueServlet.ATTRIBUTE_PREFIX + key, value);
        String url = renderContext.getURLGenerator().buildURL(page, renderContext.getMainResourceLocale().toString(), null);
        return new ActionResult(HttpServletResponse.SC_OK, url);
    }

    private static String first(Map<String, List<String>> parameters, String name) {
        List<String> values = parameters.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
