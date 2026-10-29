package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Builds the small JCR trees the reader walks, from mocks. */
final class Nodes {

    private Nodes() {
    }

    static JCRNodeWrapper node(String name, String... mixins) throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.getName()).thenReturn(name);
        for (String mixin : mixins) {
            when(node.isNodeType(mixin)).thenReturn(true);
        }
        JCRNodeIteratorWrapper none = iterator(List.of());
        when(node.getNodes()).thenReturn(none);
        return node;
    }

    static JCRNodeWrapper withChildren(JCRNodeWrapper parent, JCRNodeWrapper... children) throws RepositoryException {
        JCRNodeIteratorWrapper iterator = iterator(Arrays.asList(children));
        when(parent.getNodes()).thenReturn(iterator);
        return parent;
    }

    /** {@code parent.getNode(name)} answers {@code child}, {@code parent.hasNode(name)} is true. */
    static JCRNodeWrapper child(JCRNodeWrapper parent, String name, JCRNodeWrapper child) throws RepositoryException {
        when(parent.hasNode(name)).thenReturn(true);
        when(parent.getNode(name)).thenReturn(child);
        return parent;
    }

    static JCRNodeWrapper bool(JCRNodeWrapper node, String property, boolean value) throws RepositoryException {
        JCRPropertyWrapper prop = mock(JCRPropertyWrapper.class);
        when(prop.getBoolean()).thenReturn(value);
        when(node.hasProperty(property)).thenReturn(true);
        when(node.getProperty(property)).thenReturn(prop);
        return node;
    }

    static JCRNodeWrapper string(JCRNodeWrapper node, String property, String value) throws RepositoryException {
        JCRPropertyWrapper prop = mock(JCRPropertyWrapper.class);
        when(prop.getString()).thenReturn(value);
        when(node.hasProperty(property)).thenReturn(true);
        when(node.getProperty(property)).thenReturn(prop);
        return node;
    }

    /** A choice field's options, stored as the editor stores them: {"value","label","selected"}. */
    static JCRNodeWrapper options(JCRNodeWrapper node, String... values) throws RepositoryException {
        JCRValueWrapper[] stored = new JCRValueWrapper[values.length];
        for (int i = 0; i < values.length; i++) {
            JCRValueWrapper v = mock(JCRValueWrapper.class);
            when(v.getString()).thenReturn("{\"value\":\"" + values[i] + "\",\"label\":\"" + values[i] + " label\",\"selected\":false}");
            stored[i] = v;
        }
        JCRPropertyWrapper prop = mock(JCRPropertyWrapper.class);
        when(prop.getValues()).thenReturn(stored);
        when(node.hasProperty("options")).thenReturn(true);
        when(node.getProperty("options")).thenReturn(prop);
        return node;
    }

    private static JCRNodeIteratorWrapper iterator(List<JCRNodeWrapper> nodes) {
        Iterator<JCRNodeWrapper> it = nodes.iterator();
        JCRNodeIteratorWrapper iterator = mock(JCRNodeIteratorWrapper.class);
        when(iterator.hasNext()).thenAnswer(call -> it.hasNext());
        when(iterator.nextNode()).thenAnswer(call -> it.next());
        return iterator;
    }
}
