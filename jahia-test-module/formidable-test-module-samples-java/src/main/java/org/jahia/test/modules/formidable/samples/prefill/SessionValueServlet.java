package org.jahia.test.modules.formidable.samples.prefill;

import org.jahia.services.securityfilter.PermissionService;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hands a value kept in the visitor's HTTP session to the hidden field that shows it ({@code fmdbsample:inputHiddenSession}
 * of the tsx samples module): {@code GET /modules/formidable-samples/session-value?key=<key>} answers
 * {@code {"value": "..."}}, or a bare {@code 204} when the session holds nothing under that key.
 *
 * <p>Why the field asks instead of the page rendering the value: a page carrying the field is served from the
 * fragment cache, so a value written into the HTML would be frozen at first render and shown to every visitor. The
 * answer here is {@code no-store} and read from the session of whoever asks, so nobody else's value can reach them.
 * The value travels neither in the URL nor in a cookie of its own (the session id cookie is the container's).</p>
 *
 * <p>The producer ({@link RedirectWithValueAction}) stores the value under {@link #ATTRIBUTE_PREFIX} plus the key
 * before it redirects. The read is one-shot: the attribute is removed as it is returned. Only that prefix and only
 * {@link String} values are read, so the endpoint cannot be used to probe the rest of the session.</p>
 *
 * <p>The value is a starting point, not a trusted input: a hidden field stays editable in the browser, so whatever
 * consumes the submitted value validates it (a field action, for instance).</p>
 */
@Component(
    service = { HttpServlet.class, Servlet.class },
    property = { "alias=" + SessionValueServlet.ALIAS },
    immediate = true
)
public class SessionValueServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SessionValueServlet.class);

    public static final String ALIAS = "/formidable-samples/session-value";
    /** The security-filter scope, granted by this module's authorization configuration. */
    static final String API = "formidable-samples-session-value";
    /** Prefix of the session attributes this servlet reads. */
    public static final String ATTRIBUTE_PREFIX = "formidable.samples.session.";
    /** A key as producer and field agree on it: short, nothing that could reach another attribute. */
    static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private transient PermissionService permissionService;

    @Reference
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // whatever the answer, it belongs to this visitor alone
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");
        try {
            Map<String, Object> query = new HashMap<>();
            query.put("api", API);
            if (!permissionService.hasPermission(query)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String key = req.getParameter("key");
            if (key == null || !KEY.matcher(key).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // false: a visitor without a session has nothing stored, and a read must not create one
            HttpSession session = req.getSession(false);
            Object stored = session == null ? null : session.getAttribute(ATTRIBUTE_PREFIX + key);
            if (!(stored instanceof String value)) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            session.removeAttribute(ATTRIBUTE_PREFIX + key);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write(new JSONObject().put("value", value).toString());
        } catch (IOException e) {
            log.warn("[SessionValueServlet] Could not write the answer: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("[SessionValueServlet] Unexpected error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
