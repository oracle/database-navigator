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

package com.dbn.diagnostics.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.dbn.diagnostics.action.ParserDiagnosticsFileTypeFilterAction;
import com.dbn.diagnostics.action.ParserDiagnosticsStateFilterAction;
import com.dbn.diagnostics.data.ParserDiagnosticsDeltaResult;
import com.dbn.diagnostics.data.ParserDiagnosticsResult;
import com.dbn.diagnostics.data.StateTransition;
import com.dbn.diagnostics.ui.model.ParserDiagnosticsTableModel;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

public class ParserDiagnosticsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<ParserDiagnosticsResult> resultsList;
    private JLabel detailsLabel;
    private JLabel stateTransitionLabel;
    private JPanel filtersPanel;
    private JBScrollPane diagnosticsTableScrollPane;
    private JPanel actionsPanel;

    @Getter
    private final ParserDiagnosticsManager manager;
    private final DBNTable<ParserDiagnosticsTableModel> diagnosticsTable;


    public ParserDiagnosticsForm(Project project) {
        super(null, project);
        manager = ParserDiagnosticsManager.get(ensureProject());

        diagnosticsTable = new ParserDiagnosticsTable(this, new ParserDiagnosticsTableModel(null, null));
        diagnosticsTable.accommodateColumnsSize();
        diagnosticsTableScrollPane.setViewportView(diagnosticsTable);

        detailsLabel.setText("No result selected");
        stateTransitionLabel.setText("");

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.ParserDiagnostics", "", false);
        actionsPanel.add(actionToolbar.getComponent());

        ActionToolbar filterActionToolbar = Actions.createActionToolbar(filtersPanel,"", true,
                new ParserDiagnosticsStateFilterAction(this),
                new ParserDiagnosticsFileTypeFilterAction(this));
        filtersPanel.add(filterActionToolbar.getComponent(), BorderLayout.WEST);

        ClientProperty.BORDER.set(resultsList, Borders.tableBorder(0, 1, 0, 0));
        resultsList.addListSelectionListener(e -> {
            ParserDiagnosticsResult current = resultsList.getSelectedValue();
            ParserDiagnosticsResult previous = manager.getPreviousResult(current);
            renderResult(previous, current);
        });

        resultsList.setCellRenderer(new ResultListCellRenderer());
        refreshResults();
        selectResult(manager.getLatestResult());
    }

    public void renderResult(@Nullable ParserDiagnosticsResult previous, @Nullable ParserDiagnosticsResult current) {
        ParserDiagnosticsDeltaResult deltaResult = current == null ? null : current.delta(previous);
        ParserDiagnosticsTableModel tableModel = new ParserDiagnosticsTableModel(deltaResult, manager.getResultFilter());
        diagnosticsTable.setModel(tableModel);
        diagnosticsTable.accommodateColumnsSize();

        detailsLabel.setText(deltaResult == null ? "No result selected" : deltaResult.getName());

        StateTransition stateTransition = deltaResult == null ? StateTransition.UNCHANGED : deltaResult.getFilter();
        StateTransition.Category category = stateTransition.getCategory();
        stateTransitionLabel.setText(previous == null ? current == null ? "" : "INITIAL" : stateTransition.name());
        stateTransitionLabel.setForeground(category.getColor());
        stateTransitionLabel.setFont(category.isBold() ?
                Fonts.deriveFont(Fonts.getLabelFont(), Font.BOLD) :
                Fonts.getLabelFont());
    }

    public void refreshResult() {
        ParserDiagnosticsTableModel model = diagnosticsTable.getModel();
        ParserDiagnosticsDeltaResult result = model.getResult();
        model = new ParserDiagnosticsTableModel(result, manager.getResultFilter());
        diagnosticsTable.setModel(model);
        //GUIUtil.repaint(diagnosticsTable);
    }

    public void refreshResults() {
        DefaultListModel<ParserDiagnosticsResult> model = new DefaultListModel<>();
        List<ParserDiagnosticsResult> history = manager.getResultHistory();
        for (ParserDiagnosticsResult result : history) {
            model.addElement(result);
        }

        resultsList.setModel(model);
    }



    @Nullable
    public ParserDiagnosticsResult getSelectedResult() {
        return resultsList.getSelectedValue();
    }

    public void selectResult(@Nullable ParserDiagnosticsResult result) {
        if (result != null) {
            DefaultListModel<ParserDiagnosticsResult>  model = (DefaultListModel<ParserDiagnosticsResult>) resultsList.getModel();
            if (!model.contains(result)) {
                refreshResults();
            }
        }
        resultsList.setSelectedValue(result, true);
    }

    private class ResultListCellRenderer extends ColoredListCellRenderer<ParserDiagnosticsResult> {
        @Override
        protected void customizeCellRenderer(@NotNull JList list, ParserDiagnosticsResult value, int index, boolean selected, boolean hasFocus) {
            append(value.getName() + " - ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            ParserDiagnosticsResult previous = manager.getPreviousResult(value);
            if (previous == null) {
                append("INITIAL", SimpleTextAttributes.GRAY_ATTRIBUTES);
            } else {
                int previousCount = previous.getIssues().issueCount();
                int currentCount = value.getIssues().issueCount();
                if (previousCount < currentCount) {
                    append("DEGRADED", StateTransition.DEGRADED.getCategory().getTextAttributes());
                } else if (previousCount > currentCount) {
                    append("IMPROVED", StateTransition.IMPROVED.getCategory().getTextAttributes());
                } else {
                    append("UNCHANGED", SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }
        }
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.PARSER_DIAGNOSTICS_FORM.is(dataId)) return this;
        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
