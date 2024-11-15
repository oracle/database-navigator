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

package com.dbn.common.ui.form.field;

import com.dbn.common.ui.util.ClientProperty;

import javax.swing.JComponent;
import java.util.HashSet;
import java.util.Set;

/**
 * Classification marker for {@link JComponent}
 */
public interface JComponentCategory {

    /**
     * Utility to classify a component in this {@link JComponentCategory}
     * @param component the {@link JComponent} to be classified
     */
    default void classify(JComponent component) {
        Set<JComponentCategory> categories = ClientProperty.CLASSIFICATION.get(component);
        if (categories == null) {
            categories = new HashSet<>();
            ClientProperty.CLASSIFICATION.set(component, categories);
        }
        categories.add(this);
    }

    /**
     * Utility to determine if a component is classified in this {@link JComponentCategory}
     * @param component the {@link JComponent} to be verified
     * @return true if the component is classified with this category
     */
    default boolean classifies(JComponent component) {
        Set<JComponentCategory> categories = ClientProperty.CLASSIFICATION.get(component);
        return categories != null && categories.contains(this);
    }

}
