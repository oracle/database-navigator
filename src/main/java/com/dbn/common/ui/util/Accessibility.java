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

import com.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.ActionToolbar;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;

import static com.dbn.common.ui.util.ClientProperty.COMPONENT_GROUP_QUALIFIER;
import static com.dbn.common.ui.util.UserInterface.findChildComponent;
import static com.dbn.common.ui.util.UserInterface.getComponentLabel;
import static com.dbn.common.ui.util.UserInterface.getComponentText;
import static com.dbn.common.ui.util.UserInterface.visitRecursively;
import static com.dbn.common.util.Strings.isNotEmpty;

/**
 * Component accessibility utilities
 * Wrapper for accessible context. Acts more like a marker for identifying modules supporting accessibility.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Accessibility {

    public static String getAccessibleName(Component component) {
        AccessibleContext accessibleContext = component.getAccessibleContext();
        return accessibleContext.getAccessibleName();
    }

    public static boolean hasAccessibleName(Component component) {
        return Strings.isNotEmpty(getAccessibleName(component));
    }

    public static void setAccessibleName(ActionToolbar toolbar, @Nullable String name) {
        setAccessibleName(toolbar.getComponent(), name);
    }

    public static void setAccessibleDescription(ActionToolbar toolbar, String description) {
        setAccessibleDescription(toolbar.getComponent(), description);
    }

    public static void setAccessibleName(Component component, @Nullable String name) {
        if (name == null) return;

        String friendlyName = name.replace("_", " ");
        AccessibleContext accessibleContext = component.getAccessibleContext();
        accessibleContext.setAccessibleName(friendlyName);
    }

    public static void setAccessibleDescription(Component component, String description) {
        String friendlyDescription = description.replace("_", " ");
        AccessibleContext accessibleContext = component.getAccessibleContext();
        accessibleContext.setAccessibleDescription(friendlyDescription);
    }

    public static void setAccessibleUnit(JTextField textField, @Nls String unit, @Nls String ... qualifiers) {
        JLabel label = getComponentLabel(textField);
        if (label != null) setAccessibleUnit(label, unit, qualifiers);
    }

    private static void setAccessibleUnit(JLabel label, @Nls String unit, @Nls String ... qualifiers) {
        AccessibleContext accessibleContext = label.getAccessibleContext();

        StringBuilder accessibleName = new StringBuilder(accessibleContext.getAccessibleName());
        accessibleName.append(" (");
        accessibleName.append(unit);
        if (qualifiers.length > 0) {
            accessibleName.append(" - ");
            accessibleName.append(String.join(", ", qualifiers));
        }

        accessibleName.append("(");
        accessibleContext.setAccessibleName(accessibleName.toString());
    }

    /**
     * Initializes accessibility names for group of components by inheriting the
     * text of first component within a JPanel marked as COMPONENT_GROUP_QUALIFIER
     * It also propagates the accessibility name of parent panels to their child panels if not already set
     * @param component the root component to perform accessibility initialization for
     */
    public static void initAccessibilityGroups(JComponent component) {
        visitRecursively(component, JPanel.class, p -> initAccessibilityGroup(p));
        visitRecursively(component, JPanel.class, p -> propagateAccessibility(p));
    }

    private static void propagateAccessibility(JPanel panel) {
        if (hasAccessibleName(panel)) return;
        setAccessibleName(panel, findAccessibilityTitle(panel));
    }

    private static void initAccessibilityGroup(JPanel panel) {
        if (hasAccessibleName(panel)) return;

        JComponent groupQualifier = findChildComponent(panel, c -> COMPONENT_GROUP_QUALIFIER.is(c));
        String groupName = getComponentText(groupQualifier);
        setAccessibleName(panel, groupName);
    }

    private static String findAccessibilityTitle(JPanel panel) {
        Container parent = panel.getParent();
        if (parent instanceof JPanel) {
            JPanel parentPanel = (JPanel) parent;
            String accessibleName = parentPanel.getAccessibleContext().getAccessibleName();
            if (isNotEmpty(accessibleName)) return accessibleName;
            return findAccessibilityTitle(parentPanel);
        }
        return null;
    }
}


