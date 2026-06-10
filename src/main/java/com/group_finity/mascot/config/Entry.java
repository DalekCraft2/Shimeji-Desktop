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

    private Map<String, List<Entry>> selected;

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
        return element.hasAttributes() && element.hasAttribute(attributeName);
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
        return element.hasChildNodes() && getChildren().stream().anyMatch(child -> child.getName().equals(tagName));
    }

    public List<Entry> selectChildren(final String tagName) {
        if (!element.hasChildNodes()) {
            /* If we have no child nodes, don't bother adding List.of() to the selected map.
            Instead, just return List.of() directly to save memory. */
            return List.of();
        } else if (selected == null) {
            selected = new HashMap<>();
        }

        List<Entry> children;
        children = selected.get(tagName);
        if (children != null) {
            return children;
        }

        for (final Entry child : getChildren()) {
            if (child.getName().equals(tagName)) {
                // Only initialize a new ArrayList if we need to; otherwise, use List.of() to save memory
                if (children == null) {
                    children = new ArrayList<>();
                }
                children.add(child);
            }
        }
        if (children == null) {
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
            /* According to the ElementTraversal documentation, all objects
            that implement Element must also implement ElementTraversal,
            so this condition should always evaluate to true. */
            if (element instanceof ElementTraversal elementTraversal) {
                int childElementCount = elementTraversal.getChildElementCount();
                /* It's possible for element.hasChildNodes() to return true even if there are no child elements,
                because not all child nodes are guaranteed to be elements.
                Therefore, ensure that the number of element children is greater than 0. */
                if (childElementCount > 0) {
                    Entry[] childrenArray = new Entry[childElementCount];
                    int i = 0;
                    Element child = elementTraversal.getFirstElementChild();
                    while (i < childElementCount && child != null) {
                        childrenArray[i] = new Entry(child);
                        child = ((ElementTraversal) child).getNextElementSibling();
                        i++;
                    }
                    children = List.of(childrenArray);
                }
            } else {
                // Leave this implementation here as a fallback, just in case our
                // internal element doesn't implement ElementTraversal for whatever reason
                final NodeList childNodes = element.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node childNode = childNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        if (children == null) {
                            // Only initialize a new ArrayList if we need to; otherwise, use List.of() to save memory
                            children = new ArrayList<>(childNodes.getLength());
                        }
                        children.add(new Entry((Element) childNode));
                    }
                }
            }
        }
        if (children == null) {
            children = List.of();
        }

        return children;
    }
}
