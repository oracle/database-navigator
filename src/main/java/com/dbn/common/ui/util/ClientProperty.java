package com.dbn.common.ui.util;

import com.dbn.common.ref.WeakRef;
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
    FIELD_ERROR,
    ACTION_TOOLBAR;


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
            Object prop = comp.getClientProperty(this);
            if (prop instanceof WeakRef) {
                WeakRef ref = (WeakRef) prop;
                prop = ref.get();
            }
            return Unsafe.cast(prop);
        }
        return null;
    }

    public <T> void set(Component component, T value) {
        set(component, value, false);
    }

    public <T> void set(Component component, T value, boolean weak) {
        if (component instanceof JComponent) {
            JComponent comp = (JComponent) component;
            comp.putClientProperty(this, weak ? WeakRef.of(value) : value);
        }
    }


    public boolean isSet(Component component) {
        return get(component) != null;
    }

    public boolean isNotSet(Component component) {
        return !isSet(component);
    }

}
