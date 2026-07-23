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

package com.dbn.diagnostics.data;

import com.dbn.common.color.Colors;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import java.awt.Color;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum StateTransition {
    UNCHANGED(Category.NEUTRAL),
    IMPROVED(Category.GOOD),
    FIXED(Category.GOOD),
    DEGRADED(Category.BAD),
    BROKEN(Category.BAD);

    private final Category category;

    StateTransition(Category category) {
        this.category = category;
    }

    public @Nls String getName() {
        return txt("app.diagnostics.label.StateTransition_" + name());
    }

    @Getter
    public enum Category {
        NEUTRAL(SimpleTextAttributes.GRAY_ATTRIBUTES.getFgColor(), false),
        GOOD(Colors.getLabelSuccessForeground(), true),
        BAD(Colors.getLabelErrorForeground(), true);

        private final Color color;
        private final boolean bold;
        private final SimpleTextAttributes textAttributes;

        Category(Color color, boolean bold) {
            this.color = color;
            this.bold = bold;

            textAttributes = new SimpleTextAttributes(bold ?
                    SimpleTextAttributes.STYLE_BOLD :
                    SimpleTextAttributes.STYLE_PLAIN, color);
        }
    }
}
