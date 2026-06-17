package com.group_finity.mascot;

import java.util.ResourceBundle;

/**
 * A component whose localized text can be updated after the component has been created.
 * Used by {@link TrayMenuPanel} to automatically update the text of all windows when the
 * language setting is changed.
 */
public interface Localizable {

    /**
     * Localizes the text of this component using the specified resource bundle.
     *
     * @param languageBundle the resource bundle whose strings will be used to localize this component
     */
    void localize(ResourceBundle languageBundle);
}
