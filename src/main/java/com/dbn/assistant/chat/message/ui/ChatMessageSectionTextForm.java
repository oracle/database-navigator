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

package com.dbn.assistant.chat.message.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.intellij.lang.Language;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Dimension;
import java.util.function.Function;

public class ChatMessageSectionTextForm extends ChatMessageSectionForm {
    private JTextPane messageTextPane;
    private JPanel mainPanel;
    private TextContent content;

    public ChatMessageSectionTextForm(DBNForm parent, String content) {
        this(parent, content, c -> TextContent.plain(c));
    }

    public ChatMessageSectionTextForm(DBNForm parent, String content, Function<String, TextContent> contentBuilder) {
        super(parent, contentBuilder);
        this.content = createTextContent(content);
        applyContent();

        whenSettingsChange(() -> {
            this.content.rebuild();
            applyContent();
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void setContent(TextContent content) {
        this.content = content;
        applyContent();
    }

    private void applyContent() {
        messageTextPane.setContentType(content.getTypeId());
        messageTextPane.setText(content.getText());

        Dimension preferredSize = messageTextPane.getPreferredSize();
        //preferredSize = Dimensions.change(preferredSize, 4, 4);
        messageTextPane.setSize(preferredSize);
        messageTextPane.revalidate();
    }

    @Override
    protected void applyContent(TextContent content, @Nullable Language language) {
        setContent(content);
    }
}
