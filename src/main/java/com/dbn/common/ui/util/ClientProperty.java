/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    AVAILABILITY_CONDITION,
    FIELD_ERROR,
    ACTION_TOOLBAR,
    TAB_ICON,
    TAB_COLOR,
    TAB_TOOLTIP,
    TAB_CONTENT,
    FOCUS_INHERITANCE,
    COMPONENT_GROUP_QUALIFIER;


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
