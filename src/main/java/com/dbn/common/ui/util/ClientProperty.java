package com.dbn.common.ui.util;

import com.dbn.common.util.Unsafe;

import javax.swing.JComponent;
import java.awt.Component;

public enum ClientProperty {
    REGULAR_SPLITTER,
    BORDER,
    BORDERLESS,
    REGISTERED,
    CACHED_VALUE,
    CLASSIFICATION,
    VISIBILITY_CONDITION,
    ACCESSIBILITY_CONDITION,
    FIELD_ERROR;


    public boolean is(Component component) {
        Boolean value = get(component);
        return value != null && value;
    }

    public boolean isNot(Component component) {
        return !is(component);
    }

    public <T> T get(Component component) {
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            return Unsafe.cast(comp.getClientProperty(this));
        }
        return null;
    }

    public <T> void set(Component component, T value) {
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            comp.putClientProperty(this, value);
        }
    }

    public boolean isSet(Component component) {
        return get(component) != null;
    }

    public boolean isNotSet(Component component) {
        return !isSet(component);
    }

}
