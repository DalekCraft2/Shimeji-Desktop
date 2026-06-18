package com.group_finity.mascot.config;

/**
 * Represents a person who is credited for contributing to the creation of an image set,
 * or, in the case of {@link Type#SUPPORT}, the name and URL of a webpage where the
 * creators of an image set can be supported (e.g., Patreon).
 *
 * @param type the type of this contributor
 * @param name the name of this contributor
 * @param url an optional URL associated with this contributor
 */
// If I come up with a name for this class that describes both contributors
// and support sites like Patreon, I'll use that instead.
public record Contributor(Type type, String name, String url) {
    /**
     * Enumeration of the type of contributor.
     */
    public enum Type {
        /**
         * The contributor is the artist for the image set.
         */
        ARTIST,
        /**
         * The contributor created the configuration files for the image set.
         */
        SCRIPTER,
        /**
         * The contributor commissioned the creation of the image set.
         */
        COMMISSIONER,
        /**
         * The "contributor" represents a webpage where the creators of the image set can be supported.
         */
        SUPPORT
    }
}
