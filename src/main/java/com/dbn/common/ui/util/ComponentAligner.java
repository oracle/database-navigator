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

import lombok.experimental.UtilityClass;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for aligning form components within containers.
 * Provides methods to ensure consistent alignment of components across nested forms.
 */
@UtilityClass
public class ComponentAligner {


    public static void alignFormComponents(Container container) {
        int[] metrics = null;
        List<? extends Form> forms = container.getAlignableForms();
        for (Form form : forms) {
            if (metrics == null) metrics = new int[form.getAlignableComponents().length];
            readMetrics(form, metrics);
        }

        for (Form form : forms) {
            adjustMetrics(form, metrics);
        }
    }

    private static void readMetrics(Form form, int[] metrics) {
        Component[] components = form.getAlignableComponents();
        for (int i = 0; i < components.length; i++) {
            int width = (int) components[i].getPreferredSize().getWidth();
            metrics[i] = Math.max(metrics[i], width);
        }
        List<? extends Form> childForms = form.getAlignableForms();
        for (Form childForm : childForms) {
            readMetrics(childForm, metrics);
        }

    }

    private static void adjustMetrics(Form form, int[] metrics) {
        Component[] components = form.getAlignableComponents();
        for (int i = 0; i < components.length; i++) {
            Dimension dimension = new Dimension(metrics[i], components[i].getHeight());
            components[i].setPreferredSize(dimension);
        }
        List<? extends Form> childForms = form.getAlignableForms();
        for (Form childForm : childForms) {
            adjustMetrics(childForm, metrics);
        }
    }

    public interface Form {
        Component[] getAlignableComponents();

        default List<? extends Form> getAlignableForms() {
            return Collections.emptyList();
        }
    }

    public interface Container {
        List<? extends Form> getAlignableForms();
    }
}
