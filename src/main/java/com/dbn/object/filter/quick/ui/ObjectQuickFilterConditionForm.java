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

package com.dbn.object.filter.quick.ui;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.ComboBoxSelectionKeyListener;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Actions;
import com.dbn.object.filter.ConditionOperator;
import com.dbn.object.filter.quick.ObjectQuickFilter;
import com.dbn.object.filter.quick.ObjectQuickFilterCondition;
import com.dbn.object.filter.quick.ObjectQuickFilterManager;
import com.dbn.object.filter.quick.action.DeleteQuickFilterConditionAction;
import com.dbn.object.filter.quick.action.EnableDisableQuickFilterConditionAction;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.cachedUpperCase;

public class ObjectQuickFilterConditionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JLabel objectNameLabel;
    private JTextField patternTextField;
    private DBNComboBox<ConditionOperator> operatorComboBox;

    private final ObjectQuickFilterCondition condition;

    ObjectQuickFilterConditionForm(@NotNull ObjectQuickFilterForm parent, @NotNull final ObjectQuickFilterCondition condition) {
        super(parent);
        this.condition = condition;
        ObjectQuickFilter<?> filter = condition.getFilter();

        DBObjectType objectType = filter.getObjectType();
        objectNameLabel.setIcon(objectType.getIcon());
        objectNameLabel.setText(cachedUpperCase(objectType.getName()) + " NAME");

        operatorComboBox.setValues(ConditionOperator.values());
        patternTextField.setText(condition.getPattern());
        operatorComboBox.setSelectedValue(condition.getOperator());
        operatorComboBox.addListener((oldValue, newValue) -> {
            Project project = ensureProject();
            ObjectQuickFilterManager quickFilterManager = ObjectQuickFilterManager.getInstance(project);
            quickFilterManager.setLastUsedOperator(newValue);
            condition.setOperator(newValue);
        });

        patternTextField.setToolTipText("<html>press <b>Up/Down</b> keys to change the operator</html>");
        patternTextField.addKeyListener(ComboBoxSelectionKeyListener.create(operatorComboBox, false));
        onTextChange(patternTextField, e -> condition.setPattern(patternTextField.getText().trim()));

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,
                "DBNavigator.QuickFilter.Condition", true,
                new EnableDisableQuickFilterConditionAction(this),
                new DeleteQuickFilterConditionAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    public ObjectQuickFilterForm getParentForm() {
        return ensureParentComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return patternTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    protected ObjectQuickFilterCondition getCondition() {
        return Failsafe.nn(condition);
    }

    public void remove() {
        getParentForm().removeConditionPanel(condition);
    }

    public boolean isActive() {
        return getCondition().isActive();
    }

    public void setActive(boolean active) {
        getCondition().setActive(active);
    }
}
