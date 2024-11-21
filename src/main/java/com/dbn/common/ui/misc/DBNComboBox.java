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

package com.dbn.common.ui.misc;

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Loader;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.PresentableFactory;
import com.dbn.common.ui.ValueSelectorListener;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Context;
import com.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.ColoredListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DBNComboBox<T extends Presentable> extends JComboBox<T> implements PropertyHolder<ValueSelectorOption> {

    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();
    private ListPopup popup;
    private PresentableFactory<T> valueFactory;
    private Loader<List<T>> valueLoader;

    private final PropertyHolder<ValueSelectorOption> options = new PropertyHolderBase.IntStore<>() {
        @Override
        protected ValueSelectorOption[] properties() {
            return ValueSelectorOption.VALUES;
        }
    };

    private final MouseListener mouseListener = Mouse.listener().onPress(e -> {
        if (DBNComboBox.this.isEnabled()) {
            showPopup();
        }
    });

    public DBNComboBox(T ... values) {
        this();
        setValues(values);
    }

    public DBNComboBox() {
        super(new DBNComboBoxModel<>());
        Mouse.removeMouseListeners(this);

        addMouseListener(mouseListener);
        Color background = Colors.getTextFieldBackground();
        for (Component component : getComponents()) {
            component.addMouseListener(mouseListener);
        }
        setBackground(background);

        setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    append(DBNComboBox.this.getName(value));
                    setIcon(value.getIcon());
                }
                setBackground(background);
            }
        });
    }

    @Override
    public void setBackground(Color background) {
        super.setBackground(background);
        ComboBoxEditor editor = getEditor();
        if (editor != null) {
            editor.getEditorComponent().setBackground(background);
        }
    }

    @Override
    public void setPopupVisible(boolean visible) {
        if (visible && !isPopupVisible()) {
            displayPopup();
        }
    }

    @Override
    public boolean isPopupVisible() {
        return popup != null;
    }

    private void displayPopup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (T value : getModel().getItems()) {
            actionGroup.add(new SelectValueAction(value));
        }
        if (valueFactory != null) {
            actionGroup.add(Actions.SEPARATOR);
            actionGroup.add(new AddValueAction());
        }
        popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                actionGroup,
                Context.getDataContext(this),
                false,
                false,
                false,
                () -> {
                    popup = null;
                    UserInterface.repaintAndFocus(DBNComboBox.this);
                },
                10,
                preselect -> {
                    if (preselect instanceof DBNComboBox.SelectValueAction) {
                        SelectValueAction action = (SelectValueAction) preselect;
                        T value = action.value;
                        return value != null && value.equals(getSelectedValue());
                    }
                    return false;
                });
        Popups.showUnderneathOf(popup, this, 3, 200);
    }

    public void setValueFactory(PresentableFactory<T> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void setValueLoader(Loader<List<T>> valueLoader) {
        this.valueLoader = valueLoader;
        setValues(valueLoader.load());
    }

    public void addListener(ValueSelectorListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueSelectorListener<T> listener) {
        listeners.remove(listener);
    }


    public void clearValues() {
        selectValue(null);
        getModel().removeAllElements();
    }

    public String getOptionDisplayName(T value) {
        return getName(value);
    }

    public void reloadValues() {
        setValues(valueLoader.load());
    }

    public class SelectValueAction extends BasicAction {
        private final T value;

        SelectValueAction(T value) {
            super(getOptionDisplayName(value), null, options != null && options.is(ValueSelectorOption.HIDE_ICON) ? null : value == null ? null : value.getIcon());
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            selectValue(value);
            DBNComboBox.this.requestFocus();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setVisible(isVisible(value));
            presentation.setText(getOptionDisplayName(value), false);
        }
    }

    private class AddValueAction extends BasicAction {
        AddValueAction() {
            super(valueFactory.getActionName(), null, Icons.ACTION_ADD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            valueFactory.create(inputValue -> {
                if (inputValue != null) {
                    addValue(inputValue);
                    selectValue(inputValue);
                }
            });
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(valueFactory != null);
        }
    }

    @NotNull
    private String getName(T value) {
        if (value != null) {
            String description = value.getDescription();
            String name = value.getName();
            return options.is(ValueSelectorOption.HIDE_DESCRIPTION) || Strings.isEmpty(description) ? name : name + " (" + description + ")";
        } else {
            return "";
        }
    }

    public boolean isVisible(T value) {
        return true;
    }

    @Nullable
    public T getSelectedValue() {
        return (T) getSelectedItem();
    }

    public void setSelectedValue(@Nullable T value) {
        selectValue(value);
    }

    protected java.util.List<T> loadValues() {
        return new ArrayList<>();
    }

    public void setValues(T ... values) {
        setValues(Arrays.asList(values));
    }

    public void setValues(java.util.List<T> values) {
        DBNComboBoxModel<T> model = getModel();
        model.removeAllElements();
        addValues(values);
    }

    private void addValue(T value) {
        DBNComboBoxModel<T> model = getModel();
        model.addElement(value);
    }

    @Override
    public DBNComboBoxModel<T> getModel() {
        return (DBNComboBoxModel<T>) super.getModel();
    }

    @Override
    public void setModel(ComboBoxModel<T> aModel) {
        super.setModel(aModel);
    }

    public void addValues(Collection<T> values) {
        for (T value : values) {
            addValue(value);
        }
    }

    @Override
    public void setSelectedItem(Object anObject) {
        T oldValue = getSelectedValue();

        super.setSelectedItem(anObject);
        T newValue = getSelectedValue();
        listeners.notify(l -> l.selectionChanged(oldValue, newValue));
    }

    private void selectValue(T value) {
        T oldValue = getSelectedValue();
        DBNComboBoxModel<T> model = getModel();
        if (value != null) {
            value = model.containsItem(value) ? value : model.isEmpty() ? null : model.getElementAt(0);
        }
        if (!Commons.match(oldValue, value) || (model.isEmpty() && value == null)) {
            setSelectedItem(value);
        }
    }

    void selectNext() {
        T selectedValue = getSelectedValue();
        if (selectedValue != null) {
            List<T> values = getModel().getItems();
            int index = values.indexOf(selectedValue);
            if (index < values.size() - 1) {
                T nextValue = values.get(index + 1);
                selectValue(nextValue);
            }
        }
    }

    void selectPrevious() {
        T selectedValue = getSelectedValue();
        if (selectedValue != null) {
            List<T> values = getModel().getItems();
            int index = values.indexOf(selectedValue);
            if (index > 0) {
                T previousValue = values.get(index - 1);
                selectValue(previousValue);
            }
        }
    }

    @Override
    public boolean set(ValueSelectorOption status, boolean value) {
        return options.set(status, value);
    }

    @Override
    public boolean is(ValueSelectorOption status) {
        return options.is(status);
    }
}
