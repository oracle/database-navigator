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

package com.dbn.common.ui.info;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class DBNInfoForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextPane infoTextPane;

    public DBNInfoForm(@Nullable Disposable parent, TextContent content) {
        super(parent);
        content.setTooltip(true);
        content.rebuild();

        infoTextPane.setContentType(content.getTypeId());
        infoTextPane.setText(content.getText());
        infoTextPane.setForeground(UIUtil.getToolTipForeground());
        whenFirstShown( () -> {
            infoTextPane.revalidate();
            Dialogs.resizeToFitContent(mainPanel);
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
