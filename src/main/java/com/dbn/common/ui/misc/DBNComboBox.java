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
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.ValueFactory;
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
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_ICON;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormField;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.enableFormField;
import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.common.ui.util.ClientProperty.VISITED;
import static com.dbn.common.ui.util.ComboBoxes.getEmptyOptionsText;
import static com.dbn.common.ui.util.ComboBoxes.initComboBoxRenderer;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.first;

public class DBNComboBox<T> extends JComboBox<T> implements PropertyHolder<ValueSelectorOption> {

    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();
    private ListPopup popup;
    private ValueFactory<T> valueFactory;
    private Supplier<List<T>> valueLoader;
    private Predicate<T> valuePreselector;
    private transient ActionListener[] actionListeners;

    private final AtomicInteger loadSignature = new AtomicInteger(0);
    private final Lock loadLock = new ReentrantLock();

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

        boolean valueFactoryFirst = values.size() > 5;
        if (valueFactoryFirst && valueFactory != null) {
            actionGroup.add(new AddValueAction());
            actionGroup.add(Actions.SEPARATOR);
        }

        for (T value : values) {
            actionGroup.add(new SelectValueAction(value));
        }

        if (!valueFactoryFirst && valueFactory != null) {
            actionGroup.add(Actions.SEPARATOR);
            actionGroup.add(new AddValueAction());
        }

        if (values.isEmpty() && valueFactory == null) {
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
        if (valueLoader == null) return;

        try {
            loadLock.lock();

            // block the control
            LOADING.set(this, true);
            disableFormField(this, "TEMPORARY_LOAD");
            muteActionListeners();

            // reset values and selection
            setValues(new ArrayList<>());
            setSelectedValue(null);

            int signature = loadSignature.incrementAndGet();
            Background.run(() -> {
                if (!matchesLoadSignature(signature)) return;
                try {
                    List<T> values = valueLoader.get();

                    if (matchesLoadSignature(signature)) {
                        Dispatch.run(this, () -> {
                            setValues(values);
                            preselectValue();
                        });
                    }

                } finally {
                    if (matchesLoadSignature(signature)) {
                        LOADING.set(this, false);
                        enableFormField(this, "TEMPORARY_LOAD");
                        unmuteActionListeners();
                    }
                }
            });

        } finally {
            loadLock.unlock();
        }
    }

    private void preselectValue() {
        DBNComboBoxModel<T> model = getModel();
        if (model.isEmpty()) return;

        Predicate<T> valuePreselector = this.valuePreselector;

        if (valuePreselector == null) {
            if (model.getSize() == 1) {
                // preselect if only one option available
                T firstElement = model.getElementAt(0);
                selectValue(firstElement);
            }
        } else {
            T selectedValue = first(model.getItems(), valuePreselector);
            if (selectedValue != null) {
                this.valuePreselector = null; // one-time selection
                selectValue(selectedValue);
            }
        }
    }

    public DBNComboBox<T> withValueLoader(Supplier<List<T>> valueLoader) {
        this.valueLoader = valueLoader;
        return this;
    }

    public DBNComboBox<T> withValuePreselector(Predicate<T> valuePreselector) {
        this.valuePreselector = valuePreselector;
        return this;
    }

    public DBNComboBox<T> withValueFactory(ValueFactory<T> valueFactory) {
        this.valueFactory = valueFactory;
        return this;
    }

    public void triggerLoad(){
        if (isShowing()) {
            loadValues();
        } else {
            whenFirstShown(this, () -> loadValues());
        }
    }

    private void muteActionListeners() {
        actionListeners = getActionListeners();
        if (actionListeners == null) return;

        for (ActionListener actionListener : actionListeners) {
            removeActionListener(actionListener);
        }
    }

    private void unmuteActionListeners() {
        if (actionListeners == null) return;
        for (ActionListener actionListener : actionListeners) {
            addActionListener(actionListener);
        }
    }

    private boolean matchesLoadSignature(int signature) {
        return signature == loadSignature.get();
    }

    public void reloadValues(Predicate<T> valuePreselector) {
        this.valuePreselector = valuePreselector;
        reloadValues();
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
            super(getOptionDisplayName(value), null, getOptionIcon(value));
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

    private @Nullable Icon getOptionIcon(T value) {
        if (value == null) return null;
        if (options.is(HIDE_ICON)) return null;
        if (value instanceof Presentable) {
            Presentable presentable = (Presentable) value;
            return presentable.getIcon();
        }
        return null;
    }

    private class AddValueAction extends BasicAction {
        AddValueAction() {
            super(valueFactory.getActionName(), null, valueFactory.getIcon() != null ? valueFactory.getIcon() : Icons.ACTION_ADD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            valueFactory.createValue(inputValue -> {
                if (inputValue == null) return;

                addValue(inputValue);
                selectValue(inputValue);
            });
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(valueFactory != null);
        }
    }

    @NotNull
    private String getValueName(T value) {
        if (value == null) return " ";

        String name;
        String description = null;

        if (value instanceof Presentable) {
            Presentable presentable = (Presentable) value;
            name = presentable.getName();
            description = presentable.getDescription();
        } else {
            name = value.toString();
        }

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

    public void setValues(List<T> values) {
        T selectedValue = getSelectedValue();
        DBNComboBoxModel<T> model = new DBNComboBoxModel<>(values);
        setModel(model);

        selectValue(selectedValue);
        VISITED.set(this, false); // reset visited flag on model changes
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
        if (!Commons.match(oldValue, newValue)) {
            listeners.notify(l -> l.selectionChanged(oldValue, newValue));
        }
    }

    private void selectValue(T value) {
        T oldValue = getSelectedValue();
        DBNComboBoxModel<T> model = getModel();
        if (value != null) {
            value = model.containsItem(value) ? value : model.isEmpty() ? null : model.getElementAt(0);
        }
        if (!Commons.match(oldValue, value)) {
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
