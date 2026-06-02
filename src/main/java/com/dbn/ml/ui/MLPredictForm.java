/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.ui;

import com.dbn.common.Priority;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.MLTaskType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Form for entering feature values for ad-hoc prediction.
 *
 * @author ayoub allali
 */
public class MLPredictForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel fieldsPanel;
    private JScrollPane fieldsScrollPane;
    private JPanel resultPanel;
    private JLabel resultLabel;
    private JLabel probabilityLabel;

    private final MLResult mlResult;
    private final List<String> featureColumns;
    private final List<JTextField> inputFields = new ArrayList<>();

    MLPredictForm(MLPredictDialog parent, @NotNull MLResult mlResult, List<String> featureColumns) {
        super(parent);
        this.mlResult = mlResult;
        this.featureColumns = featureColumns;

        String modelName = mlResult.getModelHandle() != null ?
                mlResult.getModelHandle().toString() : mlResult.getAlgorithmName();

        ConnectionHandler connection = mlResult.getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this,
                "Prediction using " + modelName,
                Icons.DBO_AI_MODEL,
                connection.getEnvironmentType().getColor());
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        buildFieldsPanel();
        buildResultPanel();
    }

    private void buildFieldsPanel() {
        fieldsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < featureColumns.size(); i++) {
            // Label
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            fieldsPanel.add(new JLabel(featureColumns.get(i) + ":"), gbc);

            // Text field
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            JTextField field = new JTextField(20);
            inputFields.add(field);
            fieldsPanel.add(field, gbc);
        }

        // Add vertical glue at the bottom
        gbc.gridx = 0;
        gbc.gridy = featureColumns.size();
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        fieldsPanel.add(new JPanel(), gbc);
    }

    private void buildResultPanel() {
        resultPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Result label
        gbc.gridx = 0;
        gbc.gridy = 0;
        resultLabel = new JLabel("Click 'Predict' to get a prediction", SwingConstants.CENTER);
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 14f));
        resultPanel.add(resultLabel, gbc);

        // Probability label (for classification)
        gbc.gridy = 1;
        probabilityLabel = new JLabel("", SwingConstants.CENTER);
        probabilityLabel.setForeground(Color.GRAY);
        resultPanel.add(probabilityLabel, gbc);
    }

    public void runPrediction() {
        // Validate inputs
        List<String> values = getFeatureValues();
        for (int i = 0; i < values.size(); i++) {
            if (Strings.isEmpty(values.get(i))) {
                resultLabel.setText("Please fill in all feature values");
                resultLabel.setForeground(Color.RED);
                probabilityLabel.setText("");
                return;
            }
        }

        resultLabel.setText("Predicting...");
        resultLabel.setForeground(Color.GRAY);
        probabilityLabel.setText("");

        DBMSModelHandle dbmsHandle = (DBMSModelHandle) mlResult.getModelHandle();
        String modelName = dbmsHandle.getModelName();
        ConnectionHandler connection = dbmsHandle.getConnection();
        Project project = connection.getProject();
        boolean isClassification = mlResult.getTaskType() == MLTaskType.CLASSIFICATION;
        String featureClause = buildFeatureClause(featureColumns, values);

        try {
            DatabaseInterfaceInvoker.execute(Priority.HIGH,
                    "Predicting",
                    "Running prediction",
                    project,
                    connection.getConnectionId(),
                    conn -> {
                        DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                        if (isClassification) {
                            ResultSet rs = mlInterface.predictWithProbability(conn, modelName, featureClause);
                            try {
                                if (rs.next()) {
                                    String prediction = rs.getString("PREDICTION");
                                    double probability = rs.getDouble("PROBABILITY");
                                    updateResult(prediction, probability);
                                }
                            } finally {
                                rs.close();
                            }
                        } else {
                            String prediction = mlInterface.predict(conn, modelName, featureClause);
                            updateResult(prediction, -1);
                        }
                    });
        } catch (Exception ex) {
            updateError("Prediction failed: " + ex.getMessage());
        }
    }

    private void updateResult(String prediction, double probability) {
        resultLabel.setText("Prediction: " + prediction);
        resultLabel.setForeground(new Color(0, 128, 0)); // Green

        if (probability >= 0) {
            probabilityLabel.setText(String.format("Confidence: %.2f%%", probability * 100));
        } else {
            probabilityLabel.setText("");
        }
    }

    private void updateError(String message) {
        resultLabel.setText(message);
        resultLabel.setForeground(Color.RED);
        probabilityLabel.setText("");
    }

    private String buildFeatureClause(List<String> columns, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            String value = values.get(i);

            if (Strings.isEmpty(value)) {
                sb.append("NULL");
            } else if (isNumeric(value)) {
                sb.append(value);
            } else {
                sb.append("'").append(value.replace("'", "''")).append("'");
            }
            sb.append(" AS ").append(columns.get(i));
        }
        return sb.toString();
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public List<String> getFeatureValues() {
        List<String> values = new ArrayList<>();
        for (JTextField field : inputFields) {
            values.add(field.getText().trim());
        }
        return values;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return inputFields.isEmpty() ? null : inputFields.get(0);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
