package com.group_finity.mascot.config;

import org.w3c.dom.*;

import java.util.*;

/**
 * Represents an XML tag along with its attributes and children.
 * Supports getting a list of all instances of a tag with a given name.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Entry {
    private final Element element;

    private Map<String, String> attributes;

    private List<Entry> children;

    private final Map<String, List<Entry>> selected = new HashMap<>();

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

        if (element.hasAttributes()) {
            final NamedNodeMap attrs = element.getAttributes();
            attributes = new LinkedHashMap<>(attrs.getLength());
            for (int i = 0; i < attrs.getLength(); i++) {
                final Attr attr = (Attr) attrs.item(i);
                attributes.put(attr.getName(), attr.getValue());
            }
        } else {
            attributes = Map.of();
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

        if (element.hasChildNodes()) {
            children = new ArrayList<>();
            for (final Entry child : getChildren()) {
                if (child.getName().equals(tagName)) {
                    children.add(child);
                }
            }
        } else {
            children = List.of();
        }

        selected.put(tagName, children);

        return children;
    }

    public List<Entry> getChildren() {
        if (children != null) {
            return children;
        }

        if (element.hasChildNodes()) {
            final NodeList childNodes = element.getChildNodes();
            children = new ArrayList<>(childNodes.getLength());
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    children.add(new Entry((Element) childNode));
                }
            }
        } else {
            children = List.of();
        }

        return children;
    }
}
