package com.group_finity.mascot.config;

import org.w3c.dom.*;

import java.util.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Entry {
    private Element element;

    private Map<String, String> attributes;

    private List<Entry> children;

    private Map<String, List<Entry>> selected = new HashMap<>();

    public Entry(final Element element) {
        this.element = element;
    }

    public String getName() {
        return element.getTagName();
    }

    public String getText() {
        return element.getTextContent();
    }

    public boolean hasAttribute(final String attributeName) {
        return element.hasAttribute(attributeName);
    }

    public Map<String, String> getAttributes() {
        if (attributes != null) {
            return attributes;
        }

        attributes = new LinkedHashMap<>();
        final NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            final Attr attr = (Attr) attrs.item(i);
            attributes.put(attr.getName(), attr.getValue());
        }

        return attributes;
    }

    public String getAttribute(final String attributeName) {
        final Attr attribute = element.getAttributeNode(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    public boolean hasChild(final String tagName) {
        return getChildren().stream().anyMatch(child -> child.getName().equals(tagName));
    }

    public List<Entry> selectChildren(final String tagName) {
        List<Entry> children = selected.get(tagName);
        if (children != null) {
            return children;
        }
        children = new ArrayList<>();
        for (final Entry child : getChildren()) {
            if (child.getName().equals(tagName)) {
                children.add(child);
            }
        }

        selected.put(tagName, children);

        return children;
    }

    public List<Entry> getChildren() {
        if (children != null) {
            return children;
        }

        children = new ArrayList<>();
        final NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                children.add(new Entry((Element) childNode));
            }
        }

        return children;
    }
}
