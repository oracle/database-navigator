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

package com.dbn.plugin.about.ui;

import com.dbn.DatabaseNavigator;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.PopupCloseListener;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;

public class AboutComponent extends DBNFormBase {
    public static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=3QAPZFCCARA4J";
    private JPanel mainPanel;
    private JLabel logoLabel;
    private JLabel downloadPageLinkLabel;
    private JLabel supportPageLinkLabel;
    private JLabel requestTrackerPageLinkLabel;
    private JPanel linksPanel;
    private JLabel versionLabel;
    private JLabel supportPageLabel;

    public AboutComponent(Project project) {
        super(null, project);
        Cursor handCursor = Cursors.handCursor();

        logoLabel.setIcon(Icons.DATABASE_NAVIGATOR);
        logoLabel.setText("");
        //linksPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        downloadPageLinkLabel.setForeground(CodeInsightColors.HYPERLINK_ATTRIBUTES.getDefaultAttributes().getForegroundColor());
        downloadPageLinkLabel.setCursor(handCursor);
        downloadPageLinkLabel.addMouseListener(Mouse.listener().onClick(e ->
                BrowserUtil.browse("http://plugins.jetbrains.com/plugin/?id=1800")));

        supportPageLinkLabel.setForeground(CodeInsightColors.HYPERLINK_ATTRIBUTES.getDefaultAttributes().getForegroundColor());
        supportPageLinkLabel.setCursor(handCursor);
        supportPageLinkLabel.addMouseListener(Mouse.listener().onClick(e ->
                BrowserUtil.browse("http://confluence.jetbrains.com/display/CONTEST/Database+Navigator")));

        // TODO support page no longer available
        supportPageLabel.setVisible(false);
        supportPageLinkLabel.setVisible(false);

        requestTrackerPageLinkLabel.setForeground(CodeInsightColors.HYPERLINK_ATTRIBUTES.getDefaultAttributes().getForegroundColor());
        requestTrackerPageLinkLabel.setCursor(handCursor);
        requestTrackerPageLinkLabel.addMouseListener(Mouse.listener().onClick(e ->
                BrowserUtil.browse("https://database-navigator.atlassian.net/issues/?filter=10104")));

        IdeaPluginDescriptor ideaPluginDescriptor = DatabaseNavigator.getPluginDescriptor();
        String version = ideaPluginDescriptor.getVersion();
        version = version.substring(0, version.lastIndexOf(".")); // remove the compatibility qualifier

        versionLabel.setText("Version: " + version);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void showPopup(Project project) {
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPanel, null);
        JBPopup popup = popupBuilder.createPopup();
        popup.addListener(PopupCloseListener.create(this));
        popup.showCenteredInCurrentWindow(project);

    }
}
