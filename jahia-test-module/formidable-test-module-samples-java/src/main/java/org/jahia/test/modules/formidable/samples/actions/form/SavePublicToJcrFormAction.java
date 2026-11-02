package org.jahia.test.modules.formidable.samples.actions.form;

import org.jahia.modules.formidable.engine.api.FmdbMixin;
import org.jahia.modules.formidable.engine.api.FormAction;
import org.jahia.modules.formidable.engine.api.FormActionException;
import org.jahia.modules.formidable.engine.api.SubmittedFile;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Save to JCR, without the private fields": stores a submission exactly as the engine's own Save to JCR
 * action does, minus the fields the author marked private ({@code fmdbsamplemix:privateData}).
 * <p>
 * Nothing of the engine is changed or copied. The action takes the submitted values, drops the private
 * fields' and hands the rest to the engine's Save to JCR service, which only needs the action node to find
 * its form (the action sits in the form's action list) and writes the usual results tree, so the Results
 * page, the ACL and the retention behave as for any form. The other actions of the form (email, forward...)
 * are given the full set of values by the pipeline: a private field is not stored, it can still be mailed.
 * <p>
 * An author picks this action <em>instead of</em> the standard Save to JCR; using both would store the
 * private fields through the standard one.
 */
@Component(service = FormAction.class)
public class SavePublicToJcrFormAction implements FormAction {

    public static final String NODE_TYPE = "fmdbsample:savePublicToJcrAction";
    /** The mixin of the module's own switch; declared in this module's definitions.cnd. */
    public static final String PRIVATE_DATA_MIXIN = "fmdbsamplemix:privateData";

    private static final String FIELDS_NODE = "fields";

    /**
     * The engine's Save to JCR action, picked by its DS component name: its class is internal to the
     * engine and this bundle imports the API package only.
     */
    @Reference(target = "(component.name=org.jahia.modules.formidable.engine.actions.form.storage.SaveToJcrFormAction)")
    private FormAction engineSaveToJcr;

    @Override
    public String getNodeType() {
        return NODE_TYPE;
    }

    @Override
    public void execute(
            JCRNodeWrapper actionNode,
            HttpServletRequest req,
            JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            List<SubmittedFile> files
    ) throws FormActionException {
        Set<String> privateNames;
        try {
            privateNames = privateFieldNames(actionNode);
        } catch (RepositoryException e) {
            // Failing closed: storing nothing is better than storing what the author asked not to keep.
            throw new FormActionException("Could not read which fields are private: " + e.getMessage(), 500, e);
        }
        engineSaveToJcr.execute(actionNode, req, session, withoutPrivate(parameters, privateNames), files);
    }

    /** The node names of the form's fields carrying the private mixin; a field is submitted under its node name. */
    static Set<String> privateFieldNames(JCRNodeWrapper actionNode) throws RepositoryException {
        JCRNodeWrapper actionList = actionNode.getParent();
        JCRNodeWrapper form = actionList == null ? null : actionList.getParent();
        Set<String> names = new HashSet<>();
        if (form != null && form.isNodeType(FmdbMixin.FORM_ROOT) && form.hasNode(FIELDS_NODE)) {
            collect(form.getNode(FIELDS_NODE), names);
        }
        return names;
    }

    private static void collect(JCRNodeWrapper node, Set<String> names) throws RepositoryException {
        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            if (children.nextNode() instanceof JCRNodeWrapper child) {
                if (child.isNodeType(PRIVATE_DATA_MIXIN)) {
                    names.add(child.getName());
                }
                collect(child, names);
            }
        }
    }

    /** A copy of the values without the private fields'; the received map is left as it is for the other actions. */
    static Map<String, List<String>> withoutPrivate(Map<String, List<String>> parameters, Set<String> privateNames) {
        Map<String, List<String>> kept = new LinkedHashMap<>(parameters);
        kept.keySet().removeAll(privateNames);
        return kept;
    }
}
