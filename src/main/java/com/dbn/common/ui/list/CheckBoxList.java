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

package com.dbn.common.ui.list;

import com.dbn.common.color.Colors;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class CheckBoxList<T extends Selectable> extends JList<CheckBoxList.Entry<T>> {
    private boolean mutable;

    public CheckBoxList() {
        setSelectionMode(mutable ?
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
                ListSelectionModel.SINGLE_SELECTION);
        setBackground(Colors.getTextFieldBackground());
        setCellRenderer(new CellRenderer());
        addMouseListener(createMouseListener());
        addKeyListener(createKeyListener());
    }

    public void setElements(List<T> elements) {
        DefaultListModel<Entry<T>> model = new DefaultListModel<>();
        for (T element : elements) {
            Entry<T> entry = new Entry<>(element, this);
            model.addElement(entry);
        }
        setModel(model);
    }

    private MouseListener createMouseListener() {
        return Mouse.listener().onPress(e -> {
            if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
                int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    Entry entry = (Entry) getModel().getElementAt(index);
                    if (!CheckBoxList.this.mutable || e.getX() < 20 || e.getClickCount() == 2) {
                        entry.switchSelection();
                    }
                }
            }
        });
    }

    private @NotNull KeyListener createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    int[] indices = CheckBoxList.this.getSelectedIndices();
                    for (int index : indices) {
                        if (index >= 0) {
                            Entry entry = (Entry) getModel().getElementAt(index);
                            entry.switchSelection();
                        }
                    }
                }
            }
        };
    }

    public boolean isSelected(T presentable) {
        for (int i=0; i<getModel().getSize(); i++) {
            Entry<T> entry = (Entry<T>) getModel().getElementAt(i);
            if (entry.getSelectable().equals(presentable)) {
                return entry.isSelected();
            }
        }
        return false;
    }

    public void selectAll() {
        for (int i=0; i<getModel().getSize(); i++) {
            Entry<T> entry = (Entry<T>) getModel().getElementAt(i);
            entry.checkBox.setSelected(true);
        }

        UserInterface.repaint(this);
    }

    private class CellRenderer implements ListCellRenderer<Entry<T>> {

        @Override
        public Component getListCellRendererComponent(JList<? extends Entry<T>> list, Entry<T> entry, int index, boolean isSelected, boolean cellHasFocus) {
            boolean hasFocus = cellHasFocus || (list.getSelectedIndices().length > 1 && UserInterface.hasChildComponent(entry, c -> hasFocus()));

            Color foreground = isSelected ? UIUtil.getListSelectionForeground(hasFocus) : UIUtil.getListForeground();
            Color background = isSelected ? UIUtil.getListSelectionBackground(hasFocus) : UIUtil.getListBackground();

            entry.setBackground(background);
            entry.label.setForeground(foreground);

            return entry;
        }
    }

    public void sortElements(Comparator<T> comparator) {
        List<Entry<T>> entries = new ArrayList<>();
        ListModel<Entry<T>> model = getModel();
        for (int i=0; i<model.getSize(); i++) {
            Entry<T> entry = (Entry<T>) model.getElementAt(i);
            entries.add(entry);
        }
        if (comparator == null)
            Collections.sort(entries); else
            Collections.sort(entries, (o1, o2) -> comparator.compare(o1.selectable, o2.selectable));
        DefaultListModel newModel = new DefaultListModel();
        for (Entry<T> entry : entries) {
            newModel.addElement(entry);
        }
        setModel(newModel);
    }

    public boolean applyChanges(){
        boolean changed = false;
        ListModel model = getModel();
        for (int i=0; i<model.getSize(); i++) {
            Entry entry = (Entry) model.getElementAt(i);
            changed = entry.updatePresentable() || changed;
        }
        return changed;
    }

    public void addActionListener(ActionListener actionListener) {
        DefaultListModel model = (DefaultListModel) getModel();
        for (Object o : model.toArray()) {
            Entry entry = (Entry) o;
            entry.checkBox.addActionListener(actionListener);
        }
    }

    public void removeActionListener(ActionListener actionListener) {
        DefaultListModel model = (DefaultListModel) getModel();
        for (Object o : model.toArray()) {
            Entry entry = (Entry) o;
            entry.checkBox.removeActionListener(actionListener);
        }
    }

    public T getElementAt(int index) {
        Entry<T> entry = (Entry<T>) getModel().getElementAt(index);
        return entry.selectable;
    }


    @Getter
    static class Entry<T extends Selectable> extends JPanel implements Comparable<Entry<T>> {
        private final JCheckBox checkBox;
        private final JLabel label;
        private final T selectable;
        private final WeakRef<CheckBoxList> list;

        @Override
        public synchronized void addMouseListener(MouseListener l) {
            label.addMouseListener(l);
        }

        private Entry(T selectable, CheckBoxList list) {
            super(new BorderLayout(JBUIScale.scale(8), 0));
            this.list = WeakRef.of(list);
            setBackground(Colors.getListBackground());
            this.selectable = selectable;
            checkBox = new JCheckBox("", selectable.isSelected());
            checkBox.setOpaque(false);

            label = new JLabel(selectable.getName(), selectable.getIcon(), SwingConstants.LEFT);
            label.setOpaque(false);
            add(checkBox, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);

            initAccessibility();
        }

        private void initAccessibility() {
            setAccessibleName(this, createAccessibleName());
            checkBox.addActionListener(e -> setAccessibleName(this, createAccessibleName()));
        }

        private @NotNull String createAccessibleName() {
            return selectable.getName() + " (" + (checkBox.isSelected() ?
                    txt("app.shared.hint.Checked") :
                    txt("app.shared.hint.Unchecked")) + ")";
        }

        private boolean updatePresentable() {
            boolean changed = selectable.isSelected() != checkBox.isSelected();
            selectable.setSelected(checkBox.isSelected());
            return changed;
        }

        public boolean isSelected() {
            return checkBox.isSelected();
        }

        private void switchSelection() {
            checkBox.setSelected(!checkBox.isSelected());
            UserInterface.repaint(nvl(list.get(), this));
            for (ActionListener actionListener : checkBox.getActionListeners()) {
                actionListener.actionPerformed(new ActionEvent(checkBox, 0, "selectionChanged"));
            }
        }

        @Override
        public int compareTo(@NotNull Entry<T> o) {
            return selectable.compareTo(o.selectable);
        }
    }
}
