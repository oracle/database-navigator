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

package com.dbn.common.ui.misc;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.scale.JBUIScale;

import javax.swing.Icon;

public class DBNFilterTextField extends ExtendableTextField {

    public DBNFilterTextField() {
        ExtendableTextField.Extension leftExtension = new Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return AllIcons.Actions.Search;
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUIScale.scale(10);
            }
        };

        addExtension(leftExtension);
    }
}
