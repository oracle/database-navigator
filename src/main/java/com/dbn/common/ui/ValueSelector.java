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
import com.dbn.common.ui.util.Cursors;
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
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ValueSelector<T extends Presentable> extends JPanel{
    private final Listeners<ValueSelectorListener<T>> listeners = Listeners.create();
    private final PropertyHolder<ValueSelectorOption> options = new PropertyHolderBase.IntStore<>() {
        @Override
        protected ValueSelectorOption[] properties() {
            return ValueSelectorOption.VALUES;
        }
    };

    private final JLabel label;
    private final JPanel innerPanel;
    private boolean isEnabled = true;
    private ListPopup popup;

    private static final Border focusBorder = new CompoundBorder(new RoundedLineBorder(new JBColor(Gray._190, Gray._55), 3, 1), JBUI.Borders.empty(2, 4));
    private static final Border defaultBorder = JBUI.Borders.empty(3, 5);

    private List<T> values;
    private PresentableFactory<T> valueFactory;


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

        label = new JLabel(Commons.nvl(text, ""), cropIcon(icon), SwingConstants.LEFT);
        label.setCursor(Cursors.handCursor());
        label.addMouseListener(mouseListener);

        setBorder(defaultBorder);

        innerPanel = new JPanel(new BorderLayout());
        innerPanel.add(label, BorderLayout.WEST);
        innerPanel.addMouseListener(mouseListener);
        innerPanel.setCursor(Cursors.handCursor());
        add(innerPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(0, 30));
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
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
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        label.setCursor(isEnabled ? Cursors.handCursor(): Cursors.defaultCursor());
        innerPanel.setCursor(isEnabled ? Cursors.handCursor() : Cursors.defaultCursor());

        innerPanel.setBackground(Colors.getPanelBackground());
        innerPanel.setFocusable(isEnabled);
        label.setForeground(isEnabled ? Colors.getTextFieldForeground() : UIUtil.getLabelDisabledForeground());
    }

    public JPanel getInnerPanel() {
        return innerPanel;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
    private final MouseListener mouseListener = Mouse.listener().
            onEnter(e -> {
                if (popup == null) {
                    JPanel innerPanel = getInnerPanel();
                    innerPanel.setBorder(focusBorder);
                    innerPanel.setBackground(new JBColor(Gray._210, Gray._75));

                    UserInterface.repaint(ValueSelector.this);
                }}).
            onExit(e -> {
                if (popup == null) {
                    JPanel innerPanel = getInnerPanel();
                    innerPanel.setBorder(defaultBorder);
                    innerPanel.setBackground(Colors.getPanelBackground());

                    UserInterface.repaint(ValueSelector.this);
                }}).
            onPress(e -> {
                if (getValues().size() == 0) {
                    selectValue(null);
                } else {
                    if (isEnabled && popup == null) {
                        getInnerPanel().requestFocus();
                        showPopup();
                    }
                }
            });

    private void showPopup() {
        innerPanel.setCursor(Cursors.defaultCursor());
        label.setCursor(Cursors.defaultCursor());
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (T value : getValues()) {
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

                    innerPanel.setBorder(defaultBorder);
                    innerPanel.setBackground(Colors.getPanelBackground());
                    innerPanel.setCursor(Cursors.handCursor());
                    label.setCursor(Cursors.handCursor());

                    innerPanel.requestFocus();
                    UserInterface.repaint(ValueSelector.this);
                },
                10,
                preselect -> {
/*
                    if (anAction instanceof ValueSelector.SelectValueAction) {
                        SelectValueAction action = (SelectValueAction) anAction;
                        return action.value.equals(selectedValue);
                    }
*/
                    return false;
                });
        Popups.showUnderneathOf(popup, this, 3, 200);
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
            innerPanel.requestFocus();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setVisible(isVisible(value));
            e.getPresentation().setText(getOptionDisplayName(value), false);
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
