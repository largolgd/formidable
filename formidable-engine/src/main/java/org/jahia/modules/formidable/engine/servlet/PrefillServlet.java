package org.jahia.modules.formidable.engine.servlet;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Hands a value kept in the visitor's HTTP session to the hidden field that shows it:
 * {@code GET /modules/formidable-engine/prefill?key=<key>} answers {@code {"value": "..."}}, or a bare
 * {@code 204} when the session holds nothing under that key.
 *
 * <p>A page that carries a prefilled field is served from the fragment cache, so the value cannot
 * be written into the HTML: it would be frozen at first render and shown to every visitor. The
 * field's island asks for it here instead — an answer that is never cached ({@code no-store}), read
 * from the session of the visitor who asks, so no one else's value can reach them.</p>
 *
 * <p>The producer — a Jahia action, a servlet, any code with the request — stores the value with
 * {@code request.getSession().setAttribute(PrefillServlet.ATTRIBUTE_PREFIX + key, value)} before it
 * redirects. The read is <em>one-shot</em>: the attribute is removed as it is returned, so a reload
 * or a later form on the same session does not inherit it. Only attributes under
 * {@link #ATTRIBUTE_PREFIX} and only {@link String} values are ever read, so the endpoint cannot be
 * used to probe the rest of the session.</p>
 *
 * <p>The value is only a starting point, not a trusted input: a hidden field remains editable in the
 * browser, so whatever consumes the submitted value must validate it, for instance with a field
 * action.</p>
 */
@Component(
    service = { HttpServlet.class, Servlet.class },
    property = { "alias=/formidable-engine/prefill" },
    immediate = true
)
public class PrefillServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** The security-filter scope, auto-applied to hosted origins (see the engine's authorization yml). */
    static final String API = "formidable-prefill";

    /** Prefix of the session attributes this servlet is allowed to read. */
    public static final String ATTRIBUTE_PREFIX = "formidable.prefill.";

    /** A key as the producer and the field agree on it: short, no separator that could reach another attribute. */
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final Logger log = LoggerFactory.getLogger(PrefillServlet.class);

    private final AtomicReference<PermissionService> permissionService = new AtomicReference<>();

    @Reference
    public void setPermissionService(PermissionService service) {
        permissionService.set(service);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // whatever the answer, it belongs to this visitor alone
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");
        try {
            if (!isRequestAllowed()) {
                log.warn("[PrefillServlet] Rejected: request did not match Security Filter scope '{}'", API);
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String key = req.getParameter("key");
            if (key == null || !KEY.matcher(key).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // false: a visitor who has no session has nothing stored, and a read must not create one
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
            log.error("[PrefillServlet] Failed to write the response", e);
        } catch (RuntimeException e) {
            log.error("[PrefillServlet] Unexpected error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    boolean isRequestAllowed() {
        PermissionService service = permissionService.get();
        if (service == null) {
            throw new IllegalStateException("PermissionService is not available.");
        }
        Map<String, Object> query = new HashMap<>();
        query.put("api", API);
        return service.hasPermission(query);
    }
}
