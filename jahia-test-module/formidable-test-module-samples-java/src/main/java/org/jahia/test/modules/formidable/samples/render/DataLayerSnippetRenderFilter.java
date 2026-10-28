package org.jahia.test.modules.formidable.samples.render;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Writes, before every form in live, the inline script that listens to the {@code formidable:submitted}
 * event of the form island and pushes the {@code dataLayer} entry of the answer (see
 * {@code DataLayerResponseEnricher}) into {@code window.dataLayer}.
 *
 * <p>The script is the same for every visitor and every form, so the fragment stays cached. A page with
 * several forms receives it several times: the {@code window} flag makes the later copies do nothing,
 * one listener catching the event of every form.</p>
 *
 * <p>It is an <em>inline</em> script: a content security policy without {@code 'unsafe-inline'} (or a
 * nonce/hash for it) will refuse it, and the console says so.</p>
 */
@Component(service = RenderFilter.class, immediate = true)
public class DataLayerSnippetRenderFilter extends AbstractFilter {

    static final String SNIPPET = "<script>(function(){"
            + "if(window.__fmdbDataLayer){return;}window.__fmdbDataLayer=true;"
            + "document.addEventListener('formidable:submitted',function(e){"
            + "var r=e.detail&&e.detail.response;var d=r&&r.dataLayer;if(!d){return;}"
            + "window.dataLayer=window.dataLayer||[];window.dataLayer.push(d);});"
            + "})();</script>";

    @Activate
    public void activate() {
        // just inside the fragment cache, as the engine's other form filters
        setPriority(17);
        setApplyOnNodeTypes(FmdbMixin.FORM_ROOT);
        setApplyOnTemplateTypes("html");
        setApplyOnModes("live");
        // the same form rendered again through an include, a wrapper or an option must not repeat the script
        setSkipOnConfigurations("include,wrapper,option");
        setDescription("Adds the script pushing the submission's dataLayer entry into window.dataLayer");
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) {
        return SNIPPET + previousOut;
    }
}
