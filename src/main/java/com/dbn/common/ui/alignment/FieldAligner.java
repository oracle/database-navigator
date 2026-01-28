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

package com.dbn.common.ui.alignment;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.ClientProperty;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/**
 * Utility class for aligning form components within containers.
 * Provides methods to ensure consistent alignment of components across nested forms.
 */
public class FieldAligner {
    public static void alignFormFields(DBNForm rootForm) {
        if (rootForm == null) return;

        FieldAlignerData rootAlignerData = getAlignerData(rootForm);
        if (rootAlignerData == null) return;

        FieldAlignerMetrics metrics = new FieldAlignerMetrics();

        List<DBNForm> forms = rootAlignerData.getAlignableForms();
        for (DBNForm form : forms) {
            FieldAlignerData formAlignerData = getAlignerData(form);
            if (formAlignerData == null) continue;
            readMetrics(form, metrics);
        }

        for (DBNForm form : forms) {
            adjustMetrics(form, metrics);
        }
    }

    private static void readMetrics(DBNForm form, FieldAlignerMetrics metrics) {
        FieldAlignerData alignerData = getAlignerData(form);
        if (alignerData == null) return;

        List<List<Component>> componentGroups = alignerData.getAlignableComponents();
        for (List<Component> components : componentGroups) {
            int componentsCount = components.size();
            int[] widths = metrics.ensureLength(componentsCount);

            for (int i = 0; i < componentsCount; i++) {
                Component component = components.get(i);
                int width = (int) component.getPreferredSize().getWidth();
                widths[i] = Math.max(widths[i], width);
            }
        }
        List<DBNForm> childForms = alignerData.getAlignableForms();
        for (DBNForm childForm : childForms) {
            readMetrics(childForm, metrics);
        }
    }

    private static void adjustMetrics(DBNForm form, FieldAlignerMetrics metrics) {
        FieldAlignerData alignerData = getAlignerData(form);
        if (alignerData == null) return;

        List<List<Component>> componentGroups = alignerData.getAlignableComponents();
        for (List<Component> components : componentGroups) {
            int componentsCount = components.size();
            int[] widths = metrics.ensureLength(componentsCount);

            for (int i = 0; i < componentsCount; i++) {
                Component component = components.get(i);
                Dimension dimension = new Dimension(widths[i], component.getHeight());
                component.setPreferredSize(dimension);
            }
        }

        List<DBNForm> childForms = alignerData.getAlignableForms();
        for (DBNForm childForm : childForms) {
            adjustMetrics(childForm, metrics);
        }
    }

    @Nullable
    private static FieldAlignerData getAlignerData(DBNForm form) {
        JComponent component = form.getComponent();
        return ClientProperty.FIELD_ALIGNER_DATA.get(component);
    }

    private static class FieldAlignerMetrics {
        private int[] widths;

        private int[] ensureLength(int length) {
            if (widths == null) {
                widths = new int[length];
            } else if (widths.length < length) {
                int[] newMetrics = new int[length];
                System.arraycopy(widths, 0, newMetrics, 0, widths.length);
                widths = newMetrics;
            }
            return widths;
        }
    }
}
