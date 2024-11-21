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

package com.dbn.common.ui.table;

import com.dbn.common.latent.Latent;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.FontMetrics;

public abstract class DBNTableHeaderRendererBase implements DBNTableHeaderRenderer {

    private final Latent<FontMetrics> fontMetrics = Latent.basic(() -> {
        JLabel nameLabel = getNameLabel();
        return nameLabel.getFontMetrics(nameLabel.getFont());
    });

    protected abstract JLabel getNameLabel();

    @Override
    public final void setFont(Font font) {
        getNameLabel().setFont(font);
        fontMetrics.reset();
    }

    public FontMetrics getFontMetrics() {
        return fontMetrics.get();
    }
}

