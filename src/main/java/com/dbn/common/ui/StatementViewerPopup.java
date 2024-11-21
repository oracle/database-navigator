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

import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

public class StatementViewerPopup implements Disposable {
    private final String title;
    private EditorEx viewer;

    public StatementViewerPopup(String title, DBLanguagePsiFile previewFile, ConnectionHandler connection) {
        this.title = title;
        Project project = previewFile.getProject();
        DBLanguage language = DBLanguage.unwrap(previewFile.getLanguage());

        Document document = Documents.ensureDocument(previewFile);
        viewer = Viewers.createViewer(document, project, null, SQLFileType.INSTANCE);
        viewer.setEmbeddedIntoDialogWrapper(true);
        viewer.setBackgroundColor(Colors.getReadonlyEditorBackground());
        Editors.initEditorHighlighter(viewer, language, connection);

        JScrollPane viewerScrollPane = viewer.getScrollPane();
        viewerScrollPane.setViewportBorder(Borders.lineBorder(Colors.getReadonlyEditorBackground(), 8));
        viewerScrollPane.setBorder(null);


        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);

        //mainPanel.setBorder(new LineBorder(Color.BLACK, 1, false));
    }

    public void show(Component component) {
        JBPopup popup = createPopup();
        Point point = new Point(
                (int) (component.getLocationOnScreen().getX() + component.getWidth() + 8),
                (int) component.getLocationOnScreen().getY());
        popup.showInScreenCoordinates(component, point);
    }

    public void show(Component component, Point point) {
        JBPopup popup = createPopup();
        point.setLocation(
                point.getX() + component.getLocationOnScreen().getX() + 16,
                point.getY() + component.getLocationOnScreen().getY() + 16);

        popup.showInScreenCoordinates(component, point);
    }

    private JBPopup createPopup() {
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(viewer.getComponent(), viewer.getContentComponent());
        popupBuilder.setMovable(true);
        popupBuilder.setResizable(true);
        popupBuilder.setRequestFocus(true);
        popupBuilder.setTitle(title == null ? null : "<html>" + title + "</html>");
        JBPopup popup = popupBuilder.createPopup();

        Dimension dimension = Editors.calculatePreferredSize(viewer);
        dimension = new Dimension(
                (int) Math.min(dimension.getWidth() + 20, 1000),
                (int) Math.min(dimension.getHeight() + 60, 800));
        viewer.getScrollPane().setPreferredSize(dimension);

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                dispose();
            }
        });
        return popup;
    }

    @Override
    public void dispose() {
        if (viewer != null) {
            Editors.releaseEditor(viewer);
            viewer = null;
        }
    }
}
