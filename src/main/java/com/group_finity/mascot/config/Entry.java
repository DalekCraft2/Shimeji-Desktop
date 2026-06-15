package com.group_finity.mascot.config;

import org.w3c.dom.*;

import java.util.*;

/**
 * Represents an XML tag along with its attributes and children.
 * Supports getting a list of all instances of a tag with a given name.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see Element
 */
public class Entry {
    /**
     * The delegate XML element used by this {@code Entry}.
     */
    private final Element element;

    /**
     * The attributes of this {@code Entry}.
     * This is only initialized if needed, to save time and memory.
     *
     * @see #getAttributes()
     * @see #getAttribute(String)
     * @see #hasAttribute(String)
     */
    private Map<String, String> attributes;

    /**
     * The child nodes of this {@code Entry}.
     * This is only initialized if needed, to save time and memory.
     *
     * @see #getChildren()
     * @see #selectChildren(String)
     * @see #hasChild(String)
     */
    private List<Entry> children;

    /**
     * A map that groups the child nodes of this {@code Entry} object by their tag names.
     * This is only initialized if needed, to save time and memory.
     */
    private Map<String, List<Entry>> selected;

    /**
     * Creates a new {@code Entry} with the specified delegate XML element.
     *
     * @param element the delegate XML element used by this {@code Entry}
     */
    public Entry(final Element element) {
        this.element = element;
    }

    /**
     * Gets the tag name of this {@code Entry}.
     *
     * @return the tag name of this {@code Entry}
     * @see Element#getTagName()
     */
    public String getName() {
        return element.getTagName();
    }

    /**
     * Gets the text content of this {@code Entry}.
     *
     * @return the text content of this {@code Entry}
     * @see Element#getTextContent()
     */
    public String getText() {
        return element.getTextContent();
    }

    /**
     * Checks whether this {@code Entry} has an attribute with the specified name.
     *
     * @param attributeName the name of the attribute to check
     * @return {@code true} if an attribute with the specified name is present on this {@code Entry};
     * {@code false} otherwise
     * @see Element#hasAttribute(String)
     */
    public boolean hasAttribute(final String attributeName) {
        return element.hasAttributes() && element.hasAttribute(attributeName);
    }

    /**
     * Gets a map containing the names and values of the attributes of this {@code Entry}.
     *
     * @return a map containing the attributes of this {@code Entry}, or an empty map
     * if this {@code Entry} contains no attributes
     * @see Element#getAttributes()
     */
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

    /**
     * Gets the value of the attribute with the specified name.
     *
     * @param attributeName the name of the attribute whose value is to be returned
     * @return the value of the attribute with the specified name, or {@code null} if
     * this {@code Entry} does not contain an attribute with the specified name
     * @see Element#getAttributeNode(String)
     */
    public String getAttribute(final String attributeName) {
        final Attr attribute = element.getAttributeNode(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Checks whether this {@code Entry} has a child node with the specified name.
     *
     * @param tagName the name of the child node to check
     * @return {@code true} if this {@code Entry} has a child node with the specified name;
     * {@code false} otherwise
     */
    public boolean hasChild(final String tagName) {
        return element.hasChildNodes() && getChildren().stream().anyMatch(child -> child.getName().equals(tagName));
    }

    /**
     * Gets a list of all child nodes in this {@code Entry} that have the specified name.
     *
     * @param tagName the name of the nodes that should be collected into the returned list
     * @return a list of all child nodes with the specified name, or an empty list if this {@code Entry}
     * has no child nodes with the specified name
     */
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

    /**
     * Gets all child nodes of this {@code Entry}.
     *
     * @return a list of this {@code Entry} object's child nodes, or an empty list if this {@code Entry}
     * does not have child nodes
     */
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
                        // Only initialize a new ArrayList if we need to; otherwise, use List.of() to save memory
                        if (children == null) {
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
