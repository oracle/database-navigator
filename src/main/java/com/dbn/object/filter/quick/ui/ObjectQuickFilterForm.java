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

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.filter.Filter;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueSelector;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.ConditionJoinType;
import com.dbn.object.filter.ConditionOperator;
import com.dbn.object.filter.quick.ObjectQuickFilter;
import com.dbn.object.filter.quick.ObjectQuickFilterCondition;
import com.dbn.object.filter.quick.ObjectQuickFilterManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class ObjectQuickFilterForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel conditionsPanel;
    private JPanel actionsPanel;
    private JPanel hintPanel;
    private DBNComboBox<ConditionJoinType> joinTypeComboBox;
    private JPanel joinTypePanel;

    private final List<ObjectQuickFilterConditionForm> conditionForms = DisposableContainers.list(this);
    private final DBObjectList<?> objectList;
    private ObjectQuickFilter<?> filter;

    ObjectQuickFilterForm(@NotNull ObjectQuickFilterDialog parent, DBObjectList<?> objectList) {
        super(parent);
        this.objectList = objectList;

        addHeader(objectList);
        this.conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        this.conditionsPanel.setBorder(new CompoundBorder(Borders.BOTTOM_LINE_BORDER, JBUI.Borders.emptyBottom(4)));
        this.filter = objectList.getQuickFilter();
        if (this.filter == null) {
            this.filter = new ObjectQuickFilter<>(objectList.getObjectType());
        } else {
            this.filter = this.filter.clone();
        }

        Filter<?> configFilter = objectList.getConfigFilter();
        if (configFilter != null) {
            TextContent hintText = TextContent.plain("NOTE: This list is already filtered according to connection \"Filter\" settings. Any additional condition will narrow down the already filtered list.");
            DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
            hintPanel.add(hintForm.getComponent());
        }

        List<ObjectQuickFilterCondition> conditions = this.filter.getConditions();
        for (ObjectQuickFilterCondition condition : conditions) {
            addConditionPanel(condition);
        }

        actionsPanel.add(new NewFilterSelector(this.filter), BorderLayout.CENTER);

        joinTypeComboBox.setValues(ConditionJoinType.values());
        joinTypeComboBox.setSelectedValue(this.filter.getJoinType());
        joinTypeComboBox.setEnabled(conditionForms.size() > 1);
        joinTypeComboBox.addListener((oldValue, newValue) -> {
            this.filter.setJoinType(newValue);
        });

    }

    private void addHeader(DBObjectList<?> objectList) {
        Icon headerIcon = Icons.DATASET_FILTER;
        ConnectionHandler connection = objectList.getConnection();
        DatabaseEntity parentElement = objectList.getParentEntity();
        String headerText = "[" + connection.getName() + "] " +
                (parentElement instanceof DBSchema ? (parentElement.getName() + " - ") : "") +
                Naming.capitalizeWords(objectList.getObjectType().getName()) + " filters";
        Color headerBackground = connection.getEnvironmentType().getColor();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, headerText, headerIcon, headerBackground);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void addConditionPanel(ObjectQuickFilterCondition condition) {
        ObjectQuickFilterConditionForm conditionForm = new ObjectQuickFilterConditionForm(this, condition);
        conditionsPanel.add(conditionForm.getComponent());
        conditionForms.add(conditionForm);
        joinTypeComboBox.setEnabled(conditionForms.size() > 1);
    }

    void removeConditionPanel(ObjectQuickFilterCondition condition) {
        filter.removeCondition(condition);
        for (ObjectQuickFilterConditionForm conditionForm : conditionForms) {
            if (conditionForm.getCondition() == condition) {
                conditionForms.remove(conditionForm);
                conditionsPanel.remove(conditionForm.getComponent());
                break;
            }
        }
        joinTypeComboBox.setEnabled(conditionForms.size() > 1);
        UserInterface.repaint(mainPanel);
    }

    private class NewFilterSelector extends ValueSelector<ConditionOperator> {
        @Override
        public String getOptionDisplayName(ConditionOperator value) {
            return super.getOptionDisplayName(value);
        }

        NewFilterSelector(final ObjectQuickFilter<?> filter) {
            super(PlatformIcons.ADD_ICON, "Add Name Condition", null, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> {
                Project project = ensureProject();
                ObjectQuickFilterManager quickFilterManager = ObjectQuickFilterManager.getInstance(project);
                quickFilterManager.setLastUsedOperator(newValue);
                ObjectQuickFilterCondition condition = filter.addNewCondition(newValue);
                addConditionPanel(condition);
                UserInterface.repaint(mainPanel);
            });

        }

        @Override
        public List<ConditionOperator> loadValues() {
            return Arrays.asList(ConditionOperator.values());
        }
    }

    public ObjectQuickFilter<?> getFilter() {
        return filter;
    }

    public DBObjectList<?> getObjectList() {
        return objectList;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
