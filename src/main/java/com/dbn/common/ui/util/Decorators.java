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

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.ToolbarDecorator;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

/**
 * Utility class for creating and configuring toolbar decorators for various components.
 * Provides static methods for setting up toolbar decorators tailored for JTable and JList,
 * and for creating components with decorator configurations.
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Decorators {

    @NotNull
    public static ToolbarDecorator createToolbarDecorator(JTable table) {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table);
        return initToolbarDecorator(decorator);
    }

    private static @NotNull ToolbarDecorator initToolbarDecorator(ToolbarDecorator decorator) {
        decorator.setToolbarPosition(ActionToolbarPosition.TOP);
        decorator.setToolbarBorder(Borders.TOOLBAR_DECORATOR_BORDER);
        decorator.setPanelBorder(Borders.EMPTY_BORDER);
        return decorator;
    }

    @NotNull
    public static ToolbarDecorator createToolbarDecorator(JList<?> list) {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(list);
        return initToolbarDecorator(decorator);
    }

    public static JPanel createToolbarDecoratorComponent(ToolbarDecorator decorator, JComponent target) {
        JPanel decoratorPanel = decorator.createPanel();
        UserInterface.propagateBackgroundUp(target);
        String accessibleName = Accessibility.getAccessibleName(target);
        CommonActionsPanel actionsPanel = decorator.getActionsPanel();
        target.setFocusable(true);
        setAccessibleName(actionsPanel, accessibleName + " actions");
        return decoratorPanel;
    }
}
