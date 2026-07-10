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

package com.dbn.common.ui.link;

import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.openapi.util.NlsContexts.LinkLabel;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NonNls;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.ClientProperty.NON_DISABLEABLE;

public class HyperLinkForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel textLabel;
    private HyperlinkLabel hyperLink;

    private HyperLinkForm(@Label String text, @LinkLabel String linkText, @NonNls String linkUrl) {
        super(null);
        NON_DISABLEABLE.set(textLabel, true);
        NON_DISABLEABLE.set(hyperLink, true);

        textLabel.setText(text);
        hyperLink.setHyperlinkText(linkText);
        hyperLink.setHyperlinkTarget(linkUrl);
        hyperLink.setToolTipText(linkUrl);
    }

    public static HyperLinkForm create(@Label String text, @LinkLabel String linkText, @NonNls String linkUrl) {
        return new HyperLinkForm(text, linkText, linkUrl);
    }

    public static HyperLinkForm create(@Label String text, @LinkLabel String linkText, Runnable action) {
        HyperLinkForm form = new HyperLinkForm(text, linkText, null);
        onHyperlinkAccess(form.hyperLink, e -> action.run());
        return form;
    }

    public void setTooltipText(@Tooltip String tooltip) {
        hyperLink.setToolTipText(tooltip);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }


}
