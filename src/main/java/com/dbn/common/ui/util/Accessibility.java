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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.accessibility.AccessibleAnnouncerUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Container;

import static com.dbn.common.ui.util.ClientProperty.ACCESSIBLE_DESCRIPTION;
import static com.dbn.common.ui.util.ClientProperty.ACCESSIBLE_NAME;
import static com.dbn.common.ui.util.ClientProperty.COMPONENT_GROUP_QUALIFIER;
import static com.dbn.common.ui.util.UserInterface.getComponentLabel;
import static com.dbn.common.ui.util.UserInterface.getComponentText;
import static com.dbn.common.ui.util.UserInterface.visitRecursively;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isNotEmpty;

/**
 * Component accessibility utilities
 * Wrapper for accessible context. Acts more like a marker for identifying modules supporting accessibility.
 *
 * @author Dan Cioca (Oracle)
 */
@Slf4j
@UtilityClass
public class Accessibility {

    public static String getAccessibleName(Component component) {
        AccessibleContext accessibleContext = component.getAccessibleContext();
        return accessibleContext.getAccessibleName();
    }

    public static String getAccessibleDescription(Component component) {
        AccessibleContext accessibleContext = component.getAccessibleContext();
        return accessibleContext.getAccessibleDescription();
    }

    public static boolean hasAccessibleName(Component component) {
        return Strings.isNotEmpty(getAccessibleName(component));
    }

    public static void setAccessibleName(@Nullable Object target, @Nullable @Nls String name) {
        setAccessibleText(target, name, false);
    }

    public static void setAccessibleDescription(@Nullable Object target, @Nls String description) {
        setAccessibleText(target, description, true);
    }

    private static void setAccessibleText(@Nullable Object target, @Nullable @Nls String text, boolean descriptor) {
        if (target == null) return;
        if (text == null) return;
        if (target instanceof AccessibleContext) {
            String friendlyText = friendlyText(text);
            AccessibleContext accessibleContext = (AccessibleContext) target;
            if (descriptor) {
                accessibleContext.setAccessibleDescription(friendlyText);
            } else {
                accessibleContext.setAccessibleName(friendlyText);
            }
            return;
        }

        if (target instanceof Component) {
            Component component = (Component) target;

            setAccessibleText(component.getAccessibleContext(), text, descriptor);
            if (descriptor) {
                ACCESSIBLE_DESCRIPTION.set(component, getAccessibleName(component));
            } else {
                ACCESSIBLE_NAME.set(component, getAccessibleDescription(component));
            }
            return;
        }

        if (target instanceof ListPopup) {
            ListPopup listPopup = (ListPopup) target;
            JList actionList = UserInterface.findChildComponent(listPopup.getContent(), JList.class, c -> true);
            setAccessibleText(actionList, text, descriptor);
            return;
        }

        if (target instanceof ActionToolbar) {
            ActionToolbar toolbar = (ActionToolbar) target;
            setAccessibleText(toolbar.getComponent(), text, descriptor);
            return;
        }

        if (target instanceof ToolbarDecorator) {
            ToolbarDecorator toolbarDecorator = (ToolbarDecorator) target;
            setAccessibleText(toolbarDecorator.getActionsPanel(), text, descriptor);
            return;
        }

        log.warn("Cannot set accessible text to target of type {}", target.getClass().getName());
    }

    public static String friendlyText(String text) {
        // TODO find screen-reader text cleanser library
        return text.replace("_", " ");
    }

    public static void setAccessibleUnit(JComponent component, @Nls String unit, @Nls String ... qualifiers) {
        JLabel label = getComponentLabel(component);
        JComponent accessibleComponent = label == null ? component : label;

        AccessibleContext accessibleContext = accessibleComponent.getAccessibleContext();

        String accessibleName = ACCESSIBLE_NAME.get(component);
        if (accessibleName == null) {
            accessibleName = nvl(accessibleContext.getAccessibleName(), "");
            ACCESSIBLE_NAME.set(component, accessibleName);
        }

        if (unit == null) {
            accessibleContext.setAccessibleName(accessibleName);
        } else {
            StringBuilder builder = new StringBuilder(accessibleName);
            builder.append(" (");
            builder.append(unit);
            if (qualifiers.length > 0) {
                builder.append(" - ");
                builder.append(String.join(", ", qualifiers));
            }

            builder.append(")");
            accessibleContext.setAccessibleName(builder.toString());
        }
    }

    /**
     * Initializes accessibility names for group of components by inheriting the
     * text of first component within a JPanel marked as COMPONENT_GROUP_QUALIFIER
     * It also propagates the accessibility name of parent panels to their child panels if not already set
     * @param component the root component to perform accessibility initialization for
     */
    public static void initComponentGroupsAccessibility(JComponent component) {
        visitRecursively(component, JPanel.class, p -> initAccessibilityGroup(p));
        visitRecursively(component, JPanel.class, p -> propagateAccessibility(p));
    }


    /**
     * Initializes accessibility for custom components within the given container by correctly associating
     * {@link JLabel} components with the appropriate child components they label.
     * The method works recursively, visiting all child components of the provided container.
     *
     * @param component the root {@link JComponent} whose child components will be initialized for accessibility.
     */
    public static void initCustomComponentAccessibility(JComponent component) {
        visitRecursively(component, JLabel.class, l -> {
            Component targetComponent = l.getLabelFor();
            if (targetComponent instanceof ComponentWithBrowseButton) {
                ComponentWithBrowseButton customComponent = (ComponentWithBrowseButton) targetComponent;
                l.setLabelFor(customComponent.getChildComponent());
            }
        });
    }

    private static void propagateAccessibility(JPanel panel) {
        if (hasAccessibleName(panel)) return;
        setAccessibleName(panel, findAccessibilityTitle(panel));
    }

    private static void initAccessibilityGroup(JPanel panel) {
        if (hasAccessibleName(panel)) return;
        for (Component component : panel.getComponents()) {
            if (COMPONENT_GROUP_QUALIFIER.is(component)) {
                String groupName = getComponentText(component);
                setAccessibleName(panel, groupName);
                return;
            }
        }
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

    @Compatibility
    public static void announceEvent(Accessible component, String eventMessage) {
        AccessibleAnnouncerUtil.announce(component, eventMessage, true);
    }

    public static void attachSelectionAnnouncer(DBNComboBox<?> comboBox, String name) {
        ComboBoxes.onSelectionChange(comboBox, selectedItem -> announceEvent(comboBox, name + " selection changed to " + selectedItem.getAccessibleName()));
    }

    public static void attachStateAnnouncer(JToggleButton toggle, String name) {
        toggle.addActionListener(e -> announceEvent(toggle, name + " state changed to " + (toggle.isSelected() ? "checked" : "unchecked")));
    }
}


