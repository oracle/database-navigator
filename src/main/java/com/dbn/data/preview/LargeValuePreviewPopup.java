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

package com.dbn.data.preview;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.PopupCloseListener;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.Tables;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.editor.data.DatasetEditorManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;

import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class LargeValuePreviewPopup extends DBNFormBase {
    public static final int INITIAL_MAX_SIZE = 4000;
    private JPanel mainPanel;
    private JTextArea valueTextArea;
    private JPanel topActionsPanel;
    private JLabel infoLabel;
    private JPanel infoPanel;
    private JPanel leftActionsPanel;
    private DBNScrollPane valueScrollPane;

    private transient JBPopup popup;
    private final WeakRef<JTable> table;
    private final UserValueHolder<?> userValueHolder;

    private boolean loadContentVisible;
    private String loadContentCaption;
    private String contentInfoText;

    private final boolean largeTextLayout;
    private boolean pinned;

    public LargeValuePreviewPopup(Project project, JTable table, UserValueHolder<?> userValueHolder, int preferredWidth) {
        super(null, project);
        this.table = WeakRef.of(table);
        this.userValueHolder = userValueHolder;

        loadContent(true);
        String value = valueTextArea.getText();
        int maxRowLength = Strings.textMaxRowLength(value);
        preferredWidth = Math.max(preferredWidth, Math.min(maxRowLength * 8, 600));
        largeTextLayout = preferredWidth > 500 || loadContentVisible;

        valueScrollPane.setBorder(null);
        valueScrollPane.setPreferredSize(new Dimension(preferredWidth + 32, Math.max(60, preferredWidth / 4)));

        if (largeTextLayout) {
            boolean isBasicPreview = !(table instanceof BasicTable);
            if (isBasicPreview) {
                ActionToolbar actionToolbar = Actions.createActionToolbar(leftActionsPanel, false, new WrapUnwrapContentAction());
                JComponent toolbarComponent = actionToolbar.getComponent();
                leftActionsPanel.add(toolbarComponent, BorderLayout.NORTH);
                topActionsPanel.setVisible(false);
            } else {
                ActionToolbar actionToolbar = Actions.createActionToolbar(topActionsPanel, true,
                    /*new PinUnpinPopupAction(),
                    new CloseAction(),
                    ActionUtil.SEPARATOR,*/
                        new WrapUnwrapContentAction(),
                        new LoadReloadAction());
                JComponent toolbarComponent = actionToolbar.getComponent();
                topActionsPanel.add(toolbarComponent, BorderLayout.WEST);
                leftActionsPanel.setVisible(false);
            }

            if (!isBasicPreview) {
                infoPanel.setVisible(true);
                infoPanel.setBorder(Borders.BOTTOM_LINE_BORDER);
            } else {
                infoLabel.setVisible(false);
            }

            DatasetEditorManager dataEditorManager = DatasetEditorManager.getInstance(userValueHolder.getProject());
            pinned = dataEditorManager.isValuePreviewPinned();
            boolean isWrapped = dataEditorManager.isValuePreviewTextWrapping();
            valueTextArea.setLineWrap(isWrapped);
        } else {
            valueTextArea.setLineWrap(true);
            infoPanel.setVisible(false);
            leftActionsPanel.setVisible(false);
        }

        valueTextArea.setBackground(Colors.LIGHT_BLUE);
        valueTextArea.addKeyListener(keyListener);
    }

    @Nullable
    public JTable getTable() {
        return WeakRef.get(table);
    }

    public void addPopupListener(JBPopupListener listener) {
    }

    private void loadContent(boolean initial) {
        String text = "";
        Object userValue = userValueHolder.getUserValue();
        if (userValue instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
            try {
                text = initial ?
                        largeObjectValue.read(INITIAL_MAX_SIZE) :
                        largeObjectValue.read();
                text = Commons.nvl(text, "");

                long contentSize = largeObjectValue.size();
                if (initial && contentSize > INITIAL_MAX_SIZE) {
                    contentInfoText = getNumberOfLines(text) + " lines, " + INITIAL_MAX_SIZE + " characters (partially loaded)";
                    loadContentVisible = true;
                    loadContentCaption = "Load entire content";
                } else {
                    contentInfoText = getNumberOfLines(text) + " lines, " + text.length() + " characters";
                    loadContentVisible = false;
                }
            } catch (SQLException e) {
                conditionallyLog(e);
                contentInfoText = "Could not load " + largeObjectValue.getDisplayValue() + " content. Cause: " + e.getMessage();
                loadContentCaption = "Reload content";
            }
        } else {
            text = userValue == null ? "" : userValue.toString();
            contentInfoText = getNumberOfLines(text) + " lines, " + text.length() + " characters";
            loadContentVisible = false;
        }
        int caretPosition = valueTextArea.getText().length();
        valueTextArea.setText(text);
        valueTextArea.setCaretPosition(caretPosition);
        if (popup != null && largeTextLayout) {
            infoLabel.setText(contentInfoText);
            //popup.setAdText(contentInfoText, SwingUtilities.LEFT);
        }
    }

    private static int getNumberOfLines(String text) {
        return Strings.countNewLines(text) + 1;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            JTable table = getTable();
            // only if fired for table cells. Enable table navigation.
            if (table != null) {
                int selectedRow = table.getSelectedRow();
                int selectedColumn = table.getSelectedColumn();

                int row = selectedRow;
                int column = selectedColumn;
                if (e.getKeyCode() == 38) { // UP
                    if (selectedRow > 0) row = selectedRow - 1;
                } else if (e.getKeyCode() == 40) { // DOWN
                    if (selectedRow < table.getRowCount() - 1) row = selectedRow + 1;
                } else if (e.getKeyCode() == 37) { // LEFT
                    if (selectedColumn > 0) column = selectedColumn - 1;
                } else if (e.getKeyCode() == 39) { // RIGHT
                    if (selectedColumn < table.getColumnCount() - 1) column = selectedColumn + 1;
                }

                if (row != selectedRow || column != selectedColumn) {
                    Tables.selectCell(table, row, column);
                }
            }
        }
    };

    public JBPopup show(Component component) {
        JBPopup popup = createPopup();
        popup.showInScreenCoordinates(component,
                new Point(
                        (int) (component.getLocationOnScreen().getX() + component.getWidth() + 8),
                        (int) component.getLocationOnScreen().getY()));
        return popup;
    }

    public JBPopup show(Component component, Point point) {
        JBPopup popup = createPopup();
        point.setLocation(
                point.getX() + component.getLocationOnScreen().getX(),
                point.getY() + component.getLocationOnScreen().getY());

        popup.showInScreenCoordinates(component, point);
        return popup;
    }

    private JBPopup createPopup() {
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPanel, valueTextArea);
        popupBuilder.setMovable(true);
        popupBuilder.setResizable(true);
        popupBuilder.setRequestFocus(true);
        popupBuilder.setDimensionServiceKey(userValueHolder.getProject(), "LargeValuePreview." + userValueHolder.getName(), false);
/*
        popupBuilder.setCancelOnMouseOutCallback(new MouseChecker() {
            @Override
            public boolean check(MouseEvent event) {
                return false;
            }
        });
*/

        popupBuilder.setCancelCallback(() -> !pinned);

        if (largeTextLayout) {
            infoLabel.setText(contentInfoText);
            //popupBuilder.setAdText(contentInfoText);
            //popupBuilder.setTitle("Large value preview");
        }

        popup = popupBuilder.createPopup();
        popup.addListener(PopupCloseListener.create(this));
        return popup;
    }

    @Override
    public void disposeInner() {
        Object userValue = userValueHolder.getUserValue();
        if (userValue instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
            largeObjectValue.release();
        }
        popup = replace(popup, null);
        super.disposeInner();
    }


    /**
     * ******************************************************
     * Actions                         *
     * *******************************************************
     */
    public class WrapUnwrapContentAction extends ToggleAction {

        WrapUnwrapContentAction() {
            super("Wrap/Unwrap", "", Icons.ACTION_WRAP_TEXT);
        }


        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            DatasetEditorManager dataEditorManager = getDataEditorManager(e);
            return dataEditorManager != null && dataEditorManager.isValuePreviewTextWrapping();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            DatasetEditorManager editorManager = getDataEditorManager(e);
            if (editorManager != null) {
                editorManager.setValuePreviewTextWrapping(state);
                valueTextArea.setLineWrap(state);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            DatasetEditorManager dataEditorManager = getDataEditorManager(e);
            if (dataEditorManager != null) {
                boolean isWrapped = dataEditorManager.isValuePreviewTextWrapping();
                e.getPresentation().setText(isWrapped ? "Unwrap Content" : "Wrap Content");
            }
        }
    }

    public class PinUnpinPopupAction extends ToggleAction {

        public PinUnpinPopupAction() {
            super("Pin/Unpin", "", Icons.ACTION_PIN);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return pinned;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            DatasetEditorManager editorManager = getDataEditorManager(e);
            if (editorManager != null) {
                editorManager.setValuePreviewPinned(state);
                pinned = state;
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setText(pinned ? "Unpin" : "Pin");

        }
    }

    private class LoadReloadAction extends BasicAction {
        private LoadReloadAction() {
            super(txt("app.data.action.LoadReloadContent"), null, Icons.ACTION_RESUME);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            loadContent(false);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(loadContentVisible);
            presentation.setText(loadContentCaption);
        }
    }

    private class CloseAction extends BasicAction {
        private CloseAction() {
            super(txt("app.shared.action.Close"), null, Icons.ACTION_CLOSE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            pinned = false;
            popup.cancel();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
        }
    }

    @Nullable
    private static DatasetEditorManager getDataEditorManager(AnActionEvent e) {
        Project project = Lookups.getProject(e);
        return project == null ? null : DatasetEditorManager.getInstance(project);
    }

}
