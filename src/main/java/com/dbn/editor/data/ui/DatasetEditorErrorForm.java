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

package com.dbn.editor.data.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.DBNTooltip;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Strings;
import com.dbn.editor.data.DatasetEditorError;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.ide.IdeTooltipManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import static com.dbn.common.dispose.Failsafe.nd;

public class DatasetEditorErrorForm extends DBNFormBase implements ChangeListener {
    private JPanel mainPanel;
    private JLabel errorIconLabel;
    private JTextArea errorMessageTextArea;

    private final WeakRef<DatasetEditorModelCell> cell;

    public DatasetEditorErrorForm(@NotNull DatasetEditorModelCell cell) {
        super(null, cell.getProject());
        this.cell = WeakRef.of(cell);
        DatasetEditorError error = nd(cell.getError());
        error.addChangeListener(this);
        //errorIconLabel.setIcon(Icons.EXEC_MESSAGES_ERROR);
        errorIconLabel.setText("");
        errorMessageTextArea.setText(Strings.textWrap(error.getMessage(), 60, ": ,."));
        Color backgroundColor = Colors.getErrorHintColor();
        errorMessageTextArea.setBackground(backgroundColor);
        errorMessageTextArea.setFont(mainPanel.getFont());
        errorMessageTextArea.setFont(Fonts.deriveFont(Fonts.REGULAR, (float) 14));
        mainPanel.setBackground(backgroundColor);
    }

    @NotNull
    public DatasetEditorModelCell getCell() {
        return cell.ensure();
    }

    public void show() {
        DatasetEditorModelCell cell = getCell();
        DatasetEditorModelRow row = cell.getRow();
        DatasetEditorTable table = row.getModel().getEditorTable();
        if (!table.isShowing()) return;

        Rectangle rectangle = table.getCellRect(row.getIndex(), cell.getIndex(), false);
        Point location = rectangle.getLocation();
        int x = (int) (location.getX() + rectangle.getWidth() / 4);
        int y = (int) (location.getY() - 2);
        Point cellLocation = new Point(x, y);

        JPanel component = this.getMainComponent();
        DBNTooltip tooltip = new DBNTooltip(table, cellLocation, component);
        tooltip.setTextBackground(Colors.getErrorHintColor());
        tooltip.setDismissOnTimeout(false);

        Dispatch.delayed(200, () -> IdeTooltipManager.getInstance().show(tooltip, true));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
    }
}
