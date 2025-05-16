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
import com.dbn.common.thread.Background;
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
import com.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.popup.ListPopup;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_ICON;
import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.common.ui.util.ComboBoxes.getEmptyOptionsText;
import static com.dbn.common.ui.util.ComboBoxes.initComboBoxRenderer;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.firstElement;

public class DBNComboBox<T extends Presentable> extends JComboBox<T> implements PropertyHolder<ValueSelectorOption> {

    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();
    private ListPopup popup;
    private PresentableFactory<T> valueFactory;
    private Loader<List<T>> valueLoader;

    @Delegate
    private final PropertyHolder<ValueSelectorOption> options = PropertyHolderBase.intBase(ValueSelectorOption.VALUES);

    public DBNComboBox(T ... values) {
        this();
        setValues(values);
    }

    public DBNComboBox() {
        super(new DBNComboBoxModel<>());
        Mouse.removeMouseListeners(this);

        MouseListener mouseListener = Mouse
                .listener()
                .onPress(e -> when(isEnabled(), () -> showPopup()));

        addMouseListener(mouseListener);
        Color background = Colors.getTextFieldBackground();
        for (Component component : getComponents()) {
            component.addMouseListener(mouseListener);
        }
        setBackground(background);
        initComboBoxRenderer(this);
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
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
        ActionGroup actionGroup = createActionGroup();

        JLabel label = UserInterface.getComponentLabel(this);
        String title = label == null ? null : label.getText();
        popup = Popups.popupBuilder(actionGroup, this).
                withTitle(title).
                withTitleVisible(false).
                withMaxRowCount(10).
                withSpeedSearch().
                withDisposeCallback(() -> disposePopup()).
                withPreselectCondition(a -> preselectAction(a)).
                build();


        Popups.showUnderneathOf(popup, this, 3, 200);
    }

    private ActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        List<T> values = getModel().getItems();
        for (T value : values) {
            actionGroup.add(new SelectValueAction(value));
        }

        if (valueFactory != null) {
            actionGroup.add(Actions.SEPARATOR);
            actionGroup.add(new AddValueAction());
            return actionGroup;
        }

        if (values.isEmpty()) {
            String emptyOptionsText = getEmptyOptionsText(this);
            if (emptyOptionsText == null) return actionGroup;

            actionGroup.add(new EmptyValuesAction(emptyOptionsText));
        }

        return actionGroup;

    }

    private void disposePopup() {
        popup = null;
        UserInterface.repaintAndFocus(this);
    }

    private boolean preselectAction(AnAction a) {
        if (a instanceof DBNComboBox.SelectValueAction) {
            SelectValueAction action = (SelectValueAction) a;
            T value = action.value;
            return value != null && value.equals(getSelectedValue());
        }
        return false;
    }

    public void setValueFactory(PresentableFactory<T> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void setValueLoader(Loader<List<T>> valueLoader) {
        this.valueLoader = valueLoader;
        loadValues();
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
        return getValueName(value);
    }

    public void loadValues() {
        setValues(new ArrayList<>());
        setSelectedValue(null);

        if (valueLoader == null) return;

        Background.run(() -> {
            boolean enabled = isEnabled();
            try {
                setEnabled(false);
                LOADING.set(this, true);
                List<T> values = valueLoader.load();
                setValues(values);

                T selectedValue = firstElement(values);
                setSelectedValue(selectedValue);

            } finally {
                LOADING.set(this, false);
                setEnabled(enabled);
            }
        });
    }

    public void reloadValues() {
        setValues(Collections.emptyList());
        loadValues();
    }

    private static class EmptyValuesAction extends BasicAction {
        public EmptyValuesAction(String emptyOptionsText) {
            super(emptyOptionsText);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // just a placeholder, no action invocation
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(false);
        }
    }

    public class SelectValueAction extends BasicAction {
        private final T value;

        SelectValueAction(T value) {
            super(getOptionDisplayName(value), null, options != null && options.is(HIDE_ICON) ? null : value == null ? null : value.getIcon());
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            selectValue(value);
            requestFocus();
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
            super(valueFactory.getActionName(), null, valueFactory.getIcon() != null ? valueFactory.getIcon() : Icons.ACTION_ADD);
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
    private String getValueName(T value) {
        if (value == null) return "";

        String name = value.getName();
        String description = value.getDescription();

        if (options.is(HIDE_DESCRIPTION)) return name;
        if (Strings.isEmptyOrSpaces(description)) return name;
        if (Objects.equals(name, description)) return name;

        return name + " (" + description + ")";
    }

    public boolean isVisible(T value) {
        return true;
    }

    @Nullable
    public T getSelectedValue() {
        return (T) getSelectedItem();
    }

    @Nullable
    public String getSelectedValueName() {
        T value = getSelectedValue();
        return value == null ? "" : getValueName(value);
    }

    public void setSelectedValue(@Nullable T value) {
        selectValue(value);
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
}
