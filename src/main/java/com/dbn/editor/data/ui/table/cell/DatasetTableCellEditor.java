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

package com.dbn.editor.data.ui.table.cell;

 import com.dbn.common.color.Colors;
 import com.dbn.common.thread.Background;
 import com.dbn.common.thread.Dispatch;
 import com.dbn.common.thread.Progress;
 import com.dbn.common.ui.util.Borders;
 import com.dbn.common.ui.util.Cursors;
 import com.dbn.common.ui.util.Mouse;
 import com.dbn.data.editor.ui.BasicDataEditorComponent;
 import com.dbn.data.editor.ui.DataEditorComponent;
 import com.dbn.data.model.ColumnInfo;
 import com.dbn.data.type.DBDataType;
 import com.dbn.data.type.GenericDataType;
 import com.dbn.editor.data.DatasetEditorManager;
 import com.dbn.editor.data.filter.DatasetFilterInput;
 import com.dbn.editor.data.model.DatasetEditorModelCell;
 import com.dbn.editor.data.ui.table.DatasetEditorTable;
 import com.dbn.object.DBColumn;
 import com.dbn.object.DBDataset;
 import com.intellij.ui.JBColor;
 import com.intellij.ui.SimpleTextAttributes;
 import org.jetbrains.annotations.NotNull;

 import javax.swing.JTextField;
 import javax.swing.border.Border;
 import javax.swing.border.LineBorder;
 import javax.swing.text.Document;
 import java.awt.MouseInfo;
 import java.awt.Point;
 import java.awt.event.KeyEvent;
 import java.awt.event.KeyListener;
 import java.awt.event.MouseEvent;
 import java.util.Objects;

 public class DatasetTableCellEditor extends AbstractDatasetTableCellEditor implements KeyListener{
    private static final Border ERROR_BORDER = new LineBorder(JBColor.RED, 1);
    private static final Border POPUP_BORDER = new LineBorder(JBColor.BLUE, 1);

    public static final int HIGHLIGHT_TYPE_NONE = 0;
    public static final int HIGHLIGHT_TYPE_POPUP = 1;
    public static final int HIGHLIGHT_TYPE_ERROR = 2;

    DatasetTableCellEditor(DatasetEditorTable table) {
        this(table, new BasicDataEditorComponent());
    }

    DatasetTableCellEditor(DatasetEditorTable table, DataEditorComponent editorComponent) {
        super(table, editorComponent);
        JTextField textField = getTextField();
        textField.addKeyListener(this);
        textField.addMouseListener(mouseListener);
        textField.addMouseMotionListener(mouseListener);

        textField.setFont(table.getFont());

        updateTextField();
        Colors.subscribe(this, () -> updateTextField());
    }

     private void updateTextField() {
         JTextField textField = getTextField();
         textField.setForeground(Colors.getTextFieldForeground());
         textField.setBackground(Colors.getTextFieldBackground());
         SimpleTextAttributes selTextAttributes = getSelectionTextAttributes();
         textField.setSelectionColor(selTextAttributes.getBgColor());
         textField.setSelectedTextColor(selTextAttributes.getFgColor());
     }

     private SimpleTextAttributes getSelectionTextAttributes() {
         return getTable().getCellRenderer().getAttributes().getSelection();
     }

     public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        setCell(cell);
        ColumnInfo columnInfo = cell.getColumnInfo();
        DBDataType dataType = columnInfo.getDataType();
        if (dataType.isNative()) {
            GenericDataType genericDataType = dataType.getGenericDataType();
            highlight(cell.hasError() ? HIGHLIGHT_TYPE_ERROR : HIGHLIGHT_TYPE_NONE);
            Object userValue = cell.getUserValue();
            if (genericDataType == GenericDataType.LITERAL) {
                String value = userValue == null ? null : userValue.toString();
                setEditable(value == null || value.indexOf('\n') == -1);
            } else if (genericDataType.is(GenericDataType.DATE_TIME, GenericDataType.NUMERIC)) {
                setEditable(true);
            } else {
                setEditable(false);
            }
            JTextField textField = getTextField();
            selectText(textField);
        }
    }

    public void setEditable(boolean editable) {
        getTextField().setEditable(editable);
    }

    public void highlight(int type) {
        DataEditorComponent editorComponent = getEditorComponent();
        switch (type) {
            case HIGHLIGHT_TYPE_NONE:  editorComponent.setBorder(Borders.EMPTY_BORDER); break;
            case HIGHLIGHT_TYPE_POPUP: editorComponent.setBorder(POPUP_BORDER); break;
            case HIGHLIGHT_TYPE_ERROR: editorComponent.setBorder(Borders.EMPTY_BORDER/*ERROR_BORDER*/); break;
        }
    }

    public boolean isEditable() {
        return getTextField().isEditable();
    }

    void selectText(JTextField textField) {
        if (textField.isEditable()) {
            String originalText = textField.getText();
            Dispatch.run(() -> {
                checkDisposed();
                // select all only if the text didn't change
                if (settings.getGeneralSettings().getSelectContentOnCellEdit().value()) {
                    if (Objects.equals(originalText, textField.getText())) {
                        textField.grabFocus();
                        textField.selectAll();
                    }
                } else {
                    textField.requestFocus();
                    if (textField.isShowing()) {
                        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
                        Point textFieldLocation = textField.getLocationOnScreen();
                        int x = (int) Math.max(mouseLocation.getX() - textFieldLocation.getX(), 0);
                        int y = (int) Math.min(Math.max(mouseLocation.getY() - textFieldLocation.getY(), 0), 10);

                        Point location = new Point(x, y);
                        int position = textField.viewToModel(location);
                        textField.setCaretPosition(position);
                    }
                }
            });
        }
    }

    protected boolean isSelected() {
        JTextField textField = getTextField();
        Document document = textField.getDocument();
        return document.getLength() > 0 && textField.getSelectionStart() == 0 && textField.getSelectionEnd() == document.getLength();
    }

    @Override
    public Object getCellEditorValue() {
        return super.getCellEditorValue();
/*
        String stringValue = (String) super.getCellEditorValue();
        return stringValue != null &&
                stringValue.trim().length() > 0 ? stringValue : null;
*/
    }

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {}



    @Override
    public void keyPressed(KeyEvent e) {
        if (!e.isConsumed()) {
            JTextField textField = getTextField();

            int caretPosition = textField.getCaretPosition();
            if (e.getKeyCode() == 37 ) { // LEFT
                if (isSelected()) {
                    textField.setCaretPosition(0);
                } else  if (caretPosition == 0) {
                    e.consume();
                    DatasetEditorModelCell cell = getCell();
                    if (cell != null) cell.editPrevious();
                }
            }
            else if (e.getKeyCode() == 39 ) { // RIGHT
                if (!isSelected() && caretPosition == textField.getDocument().getLength()) {
                    e.consume();
                    DatasetEditorModelCell cell = getCell();
                    if (cell != null) cell.editNext();
                }
            }
            else if (e.getKeyCode() == 27 ) { // ESC
                e.consume();
                getTable().cancelEditing();
            }
        }
    }

    /********************************************************
     *                    MouseListener                     *
     ********************************************************/
    private final Mouse.Listener mouseListener = Mouse.listener().
            onClick(e -> {
                if (Mouse.isNavigationEvent(e)) {
                    DatasetEditorModelCell cell = getCell();
                    DatasetEditorTable table = getTable();
                    if (cell != null && cell.isNavigable()) {
                        DBDataset dataset = table.getDataset();
                        DBColumn column = cell.getColumn();

                        Progress.prompt(getProject(), dataset, true,
                                "Opening record",
                                "Opening record details for " + column.getQualifiedNameWithType(),
                                progress -> {
                                    DatasetFilterInput filterInput = table.getModel().resolveForeignKeyRecord(cell);
                                    if (filterInput != null) {
                                        Dispatch.run(() -> {
                                            DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(table.getProject());
                                            datasetEditorManager.navigateToRecord(filterInput, e);
                                        });
                                    }
                                });
                    }
                    e.consume();
                }
            }).
            onRelease(e -> {
                DatasetEditorModelCell cell = getCell();
                if (cell != null && e.getButton() == MouseEvent.BUTTON3 ) {
                    getTable().showPopupMenu(e, cell, cell.getColumnInfo());
                }}).
            onMove(e -> {
                JTextField textField = getTextField();
                DatasetEditorModelCell cell = getCell();
                if (cell != null) {
                    if (e.isControlDown() && cell.isNavigable()) {

                        Background.run(getProject(), () -> {
                            DBColumn column = cell.getColumn();
                            DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                            if (foreignKeyColumn != null && !e.isConsumed()) {
                                Dispatch.run(() -> {
                                    textField.setToolTipText("<html>Show referenced <b>" + foreignKeyColumn.getDataset().getQualifiedName() + "</b> record<html>");
                                    textField.setCursor(Cursors.handCursor());
                                });
                            }
                        });

                    } else {
                        textField.setCursor(Cursors.textCursor());
                        textField.setToolTipText(null);
                    }
                }
            });
 }
