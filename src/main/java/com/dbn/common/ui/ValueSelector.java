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

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.ui.panel.DBNButtonPanel;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

@Getter
@Setter
public abstract class ValueSelector<T extends Presentable> extends DBNButtonPanel {

    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();
    private final PropertyHolder<ValueSelectorOption> options = new PropertyHolderBase.IntStore<>() {
        @Override
        protected ValueSelectorOption[] properties() {
            return ValueSelectorOption.VALUES;
        }
    };

    private final JLabel label;
    private final JPanel innerPanel;
    private transient ListPopup popup;

    private List<T> values;
    private PresentableFactory<T> valueFactory;
    private PresentableFactory<T> emptyValueFactory;


    public ValueSelector(@Nullable String text, @Nullable T preselectedValue, ValueSelectorOption... options) {
        this(null, text, null, preselectedValue, options);
    }

    public ValueSelector(@Nullable Icon icon, @Nullable String text, @Nullable T preselectedValue, ValueSelectorOption... options) {
        this(icon, text, null, preselectedValue, options);
    }

    public ValueSelector(@Nullable Icon icon, @Nullable String text, @Nullable List<T> values, @Nullable T preselectedValue, ValueSelectorOption... options) {
        super(new BorderLayout());
        setOptions(options);
        this.values = values;

        text = Commons.nvl(text, "");

        setActionConsumer(e -> displayPopup());
        addMouseListener(createMouseListener());
        setFocusable(true);
        setAccessibleName(this, text);
        setAccessibleDescription(this, text);


        label = new JLabel(text, cropIcon(icon), SwingConstants.LEFT);
        label.setCursor(Cursors.handCursor());
        label.setBorder(JBUI.Borders.empty(4, 6));

        setBorder(DEFAULT_BORDER);

        innerPanel = new JPanel(new BorderLayout());
        innerPanel.add(label, BorderLayout.WEST);
        innerPanel.setCursor(Cursors.handCursor());
        add(innerPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(0, 30));
    }

    public void setOptions(ValueSelectorOption ... options) {
        for (ValueSelectorOption option : options) {
            this.options.set(option, true);
        }
    }

    public void addListener(ValueSelectorListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueSelectorListener<T> listener) {
        listeners.remove(listener);
    }

    private static Icon cropIcon(Icon icon) {
        return icon == null ? null : IconUtil.cropIcon(icon, 16, 16);
    }

    @Override
    public void setEnabled(boolean enabled) {
        label.setCursor(enabled ? Cursors.handCursor(): Cursors.defaultCursor());
        innerPanel.setCursor(enabled ? Cursors.handCursor() : Cursors.defaultCursor());

        innerPanel.setBackground(Colors.getPanelBackground());
        innerPanel.setFocusable(enabled);
        label.setForeground(enabled ? Colors.getTextFieldForeground() : UIUtil.getLabelDisabledForeground());
        super.setEnabled(enabled);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    private MouseListener createMouseListener() {
        return Mouse.listener().
                onEnter(e -> {
                    if (popup != null) return;

                    JPanel innerPanel = getInnerPanel();
                    innerPanel.setBackground(Colors.lafDarker(Colors.getPanelBackground(), 4));
                    UserInterface.repaint(ValueSelector.this);
                }).
                onExit(e -> {
                    if (popup != null) return;

                    JPanel innerPanel = getInnerPanel();
                    innerPanel.setBackground(Colors.getPanelBackground());
                    UserInterface.repaint(ValueSelector.this);
                });
    }

    private void displayPopup() {
        if (getValues().isEmpty()) {
            selectValue(null);
        } else {
            if (isEnabled() && popup == null) {
                showPopup();
            }
        }
    }

    private void showPopup() {
        innerPanel.setCursor(Cursors.defaultCursor());
        label.setCursor(Cursors.defaultCursor());
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        if (emptyValueFactory != null) {
            actionGroup.add(new AddEmptyValueAction());
            actionGroup.add(Actions.SEPARATOR);
        }

        for (T value : getValues()) {
            actionGroup.add(new SelectValueAction(value));
        }

        if (valueFactory != null) {
            actionGroup.add(Actions.SEPARATOR);
            actionGroup.add(new AddValueAction());
        }

        popup = Popups.popupBuilder(actionGroup, this).
                withTitle(label.getText()).
                withTitleVisible(false).
                withMaxRowCount(10).
                withSpeedSearch().
                withDisposeCallback(() -> adjustSelector()).
                build();

        Popups.showUnderneathOf(popup, this, 3, 200);
    }

    private void adjustSelector() {
        popup = null;

        innerPanel.setBackground(Colors.getPanelBackground());
        innerPanel.setCursor(Cursors.handCursor());
        label.setCursor(Cursors.handCursor());

        UserInterface.repaint(ValueSelector.this);
    }

    public void clearValues() {
        selectValue(null);
        values.clear();
    }

    public String getOptionDisplayName(T value) {
        return getName(value);
    }

    public class SelectValueAction extends BasicAction {
        private final T value;

        SelectValueAction(T value) {
            super(getOptionDisplayName(value), null, options.is(ValueSelectorOption.HIDE_ICON) ? null : value.getIcon());
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            selectValue(value);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(isVisible(value));
            e.getPresentation().setText(getOptionDisplayName(value), false);
        }
    }

    private class AddEmptyValueAction extends BasicAction {
        AddEmptyValueAction() {
            super(emptyValueFactory.getActionName(), null, null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            emptyValueFactory.create(inputValue -> {
                if (inputValue != null) {
                    addValue(inputValue);
                    selectValue(inputValue);
                }
            });
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(emptyValueFactory != null);
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

    public void setSelectedValue(@Nullable T value) {
        selectValue(value);
    }

    public final List<T> getValues() {
        if (values == null) {
            values = loadValues();
        }
        return values;
    }

    protected List<T> loadValues() {
        return new ArrayList<>();
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    private void addValue(T value) {
        this.values.add(value);
    }

    public void addValues(Collection<T> value) {
        this.values.addAll(value);
    }


    public void resetValues() {
        this.values = null;
    }

    private void selectValue(T value) {
        listeners.notify(l -> l.selectionChanged(null, value));
    }
}
