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

package com.dbn.common.ui.tab;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.ClientProperty;
import lombok.experimental.UtilityClass;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;

@UtilityClass
public class DBNTabs {
    public static void initTabComponent(Component component, Icon icon, Color color, DBNForm content) {
        ClientProperty.TAB_ICON.set(component, icon);
        ClientProperty.TAB_COLOR.set(component, color);
        ClientProperty.TAB_CONTENT.set(component, content, true);
    }

    public static void updateTabColor(Component component, Color color) {
        ClientProperty.TAB_COLOR.set(component, color);
    }
}
