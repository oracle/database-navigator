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

package com.dbn.common.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

public class HyperLinkForm extends DBNFormBase {
    private JPanel mainPanel;
    private HyperlinkLabel hyperLink;
    private JLabel textLabel;

    private HyperLinkForm(String text, String linkText, String linkUrl) {
        super(null);
        textLabel.setText(text);
        hyperLink.setHyperlinkText(linkText);
        hyperLink.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(linkUrl);
            }
        });
    }

    public static HyperLinkForm create(String text, String linkText, String linkUrl) {
        return new HyperLinkForm(text, linkText, linkUrl);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }


}
