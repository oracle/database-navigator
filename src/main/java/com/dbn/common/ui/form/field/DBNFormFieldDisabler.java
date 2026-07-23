/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.ui.form.field;

import com.dbn.common.thread.Dispatch;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import javax.swing.JComponent;
import java.util.HashSet;
import java.util.Set;

import static com.dbn.common.ui.util.ClientProperty.FIELD_DISABLED_REASONS;

/**
 * Disabling fields may concomitantly happen for several reasons. This utility is keeping track of the reasons a field is disabled,
 * making sure it only enables it if there are no disable-reasons associated with the field
 */
@UtilityClass
public class DBNFormFieldDisabler {
    public static void disableFormFields(JComponent[] components, @NonNls String reason) {
        for (JComponent component : components) {
            disableFormField(component, reason);
        }
    }

    public static void enableFormFields(JComponent[] components, @NonNls String reason) {
        for (JComponent component : components) {
            enableFormField(component, reason);
        }
    }

    public static void disableFormField(JComponent component, @NonNls String reason) {
        Set<String> disabledReasons = getDisabledReasons(component);
        disabledReasons.add(reason);
        setFieldEnabled(component, false);
    }

    public static void enableFormField(JComponent component, @NonNls String reason) {
        Set<String> disabledReasons = getDisabledReasons(component);
        disabledReasons.remove(reason);
        boolean enabled = disabledReasons.isEmpty();
        setFieldEnabled(component, enabled);
    }

    public static void setFormFieldEnabled(JComponent component, @NonNls String reason, boolean enabled) {
        if (enabled) {
            enableFormField(component, reason);
        } else {
            disableFormField(component, reason);
        }
    }

    public static void setFieldEnabled(JComponent component, boolean enabled) {
        Dispatch.run(component, true, () -> component.setEnabled(enabled));
    }

    private static Set<String> getDisabledReasons(JComponent component) {
        return FIELD_DISABLED_REASONS.get(component, () -> new HashSet<>());
    }
}
