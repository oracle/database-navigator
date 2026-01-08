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

package com.dbn.ml.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.onnx.OnnxMetadataHelper;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.tribuo.Model;
import org.tribuo.ONNXExportable;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.impl.ArrayExample;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

public class MLExecutionResultForm extends ExecutionResultFormBase<MLExecutionResult> {
    private JPanel mainPanel;
    private final MLResult result;
    
    // Prediction components
    private List<JBTextField> featureInputFields;
    private JLabel predictionResultLabel;
    private JTextArea predictionDetailsArea;

    public MLExecutionResultForm(@NotNull MLExecutionResult executionResult) {
        super(executionResult);
        this.result = executionResult.getMlResult();
        initializeComponents();
    }

    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header with status, metrics, and action buttons
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane for different views
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Tab 1: Summary Metrics
        tabbedPane.addTab("Summary", createSummaryPanel());
        
        // Tab 2: Per-Class Metrics
        tabbedPane.addTab("Per-Class Metrics", createPerClassMetricsPanel());
        
        // Tab 3: Confusion Matrix
        tabbedPane.addTab("Confusion Matrix", createConfusionMatrixPanel());
        
        // Tab 4: Predictions
        tabbedPane.addTab("Predictions", createPredictionsPanel());
        
        // Tab 5: Model Info
        tabbedPane.addTab("Model Info", createModelInfoPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Left: Status and title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel statusIcon = new JLabel(Icons.COMMON_STATUS_SUCCESS);
        JLabel titleLabel = new JLabel("Model Training Complete");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(statusIcon);
        titlePanel.add(titleLabel);
        
        // Center: Key metrics summary
        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        double accuracy = result.getAccuracy() * 100;
        addMetricBadge(metricsPanel, "Accuracy", String.format("%.1f%%", accuracy));
        addMetricBadge(metricsPanel, "Classes", String.valueOf(result.getClassCount()));
        addMetricBadge(metricsPanel, "Features", String.valueOf(result.getFeatureCount()));
        addMetricBadge(metricsPanel, "Time", result.getTrainingTimeMs() + "ms");
        
        // Right: Action buttons
        JPanel actionsPanel = createActionsPanel();
        
        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(metricsPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.EAST);
        
        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        // Save Model button
        JButton saveButton = new JButton("Save Model");
        saveButton.setToolTipText("Save model to file for later use");
        saveButton.addActionListener(e -> saveModel());
        
        // Export ONNX button
        JButton exportOnnxButton = new JButton("Export ONNX");
        exportOnnxButton.setToolTipText("Export model to ONNX format for deployment");
        exportOnnxButton.addActionListener(e -> exportToOnnx());
        
        // Export to DB button
        JButton exportToDbButton = new JButton("Export to DB");
        exportToDbButton.setToolTipText("Export model directly to Oracle Database");
        exportToDbButton.addActionListener(e -> exportToDatabase());
        
        // Check if model supports ONNX export
        Model<Label> model = result.getModel();
        boolean supportsOnnx = model instanceof ONNXExportable;
        exportOnnxButton.setEnabled(supportsOnnx);
        exportToDbButton.setEnabled(supportsOnnx);
        
        panel.add(saveButton);
        panel.add(exportOnnxButton);
        panel.add(exportToDbButton);
        
        return panel;
    }

    private void saveModel() {
        Project project = result.getConnection().getProject();
        
        FileSaverDescriptor descriptor = new FileSaverDescriptor(
                "Save Model",
                "Save the trained model to a file",
                "model"
        );
        
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
        VirtualFileWrapper fileWrapper = dialog.save("trained_model.model");
        
        if (fileWrapper != null) {
            Path path = fileWrapper.getFile().toPath();
            
            Background.run(() -> {
                try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
                    oos.writeObject(result.getModel());
                    
                    Dispatch.run(() -> Messages.showInfoDialog(
                            project,
                            "Model Saved",
                            "Model saved successfully to:\n" + path
                    ));
                } catch (Exception ex) {
                    Dispatch.run(() -> Messages.showErrorDialog(
                            project,
                            "Save Failed",
                            "Failed to save model: " + ex.getMessage()
                    ));
                }
            });
        }
    }

    private void exportToOnnx() {
        Project project = result.getConnection().getProject();
        Model<Label> model = result.getModel();
        
        if (!(model instanceof ONNXExportable)) {
            Messages.showErrorDialog(
                    project,
                    "Export Not Supported",
                    "This model type does not support ONNX export."
            );
            return;
        }
        
        FileSaverDescriptor descriptor = new FileSaverDescriptor(
                "Export ONNX Model",
                "Export the model to ONNX format",
                "onnx"
        );
        
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
        VirtualFileWrapper fileWrapper = dialog.save("trained_model.onnx");
        
        if (fileWrapper != null) {
            Path path = fileWrapper.getFile().toPath();
            
            Background.run(() -> {
                try {
                    ONNXExportable onnxModel = (ONNXExportable) model;
                    
                    // Export ONNX model to file
                    onnxModel.saveONNXModel(
                            "com.dbn.ml",  // domain
                            0,             // model version
                            path
                    );
                    
                    // Save Oracle metadata as sidecar JSON file
                    String oracleMetadata = OnnxMetadataHelper.buildOracleMetadataJson(result);
                    OnnxMetadataHelper.saveMetadataFile(path, oracleMetadata);
                    
                    Path metadataPath = OnnxMetadataHelper.getMetadataPath(path);
                    
                    Dispatch.run(() -> Messages.showInfoDialog(
                            project,
                            "ONNX Export Complete",
                            "Model exported successfully!\n\n" +
                            "ONNX Model: " + path.getFileName() + "\n" +
                            "Oracle Metadata: " + metadataPath.getFileName() + "\n\n" +
                            "Use both files when loading to Oracle DB with DBMS_DATA_MINING.IMPORT_ONNX_MODEL"
                    ));
                } catch (Exception ex) {
                    Dispatch.run(() -> Messages.showErrorDialog(
                            project,
                            "Export Failed",
                            "Failed to export model to ONNX: " + ex.getMessage()
                    ));
                }
            });
        }
    }

    private void exportToDatabase() {
        Project project = result.getConnection().getProject();
        Model<Label> model = result.getModel();
        
        if (!(model instanceof ONNXExportable)) {
            Messages.showErrorDialog(project, "Export Not Supported", 
                    "This model type does not support ONNX export.");
            return;
        }
        
        // Ask user for model name using IntelliJ's Messages
        String modelName = com.intellij.openapi.ui.Messages.showInputDialog(
                project,
                "Enter the model name for Oracle Database:",
                "Export to Database",
                com.intellij.openapi.ui.Messages.getQuestionIcon()
        );
        
        if (modelName == null || modelName.trim().isEmpty()) {
            return; // User cancelled
        }
        
        // Validate model name (Oracle identifier rules)
        String finalModelName = modelName.trim().toUpperCase();
        if (!finalModelName.matches("^[A-Z][A-Z0-9_]*$")) {
            Messages.showErrorDialog(project, "Invalid Model Name",
                    "Model name must start with a letter and contain only letters, numbers, and underscores.");
            return;
        }
        
        Background.run(() -> {
            try {
                // 1. Export ONNX to byte array (in memory)
                ONNXExportable onnxModel = (ONNXExportable) model;
                ai.onnx.proto.OnnxMl.ModelProto modelProto = onnxModel.exportONNXModel(
                        "com.dbn.ml",  // domain
                        0              // model version
                );
                byte[] onnxBytes = modelProto.toByteArray();
                
                // 2. Generate Oracle metadata JSON
                String metadataJson = OnnxMetadataHelper.buildOracleMetadataJson(result);
                
                // 3. Get connection and schema
                ConnectionHandler connection = result.getConnection();
                String schemaName = connection.getUserName();
                
                // 4. Upload to database
                try (DBNConnection conn = connection.getMainConnection()) {
                    // Create BLOB from bytes
                    Blob modelBlob = conn.createBlob();
                    modelBlob.setBytes(1, onnxBytes);
                    
                    // Call the import procedure
                    DatabaseInterfaces interfaces = connection.getInterfaces();
                    DatabaseVectorInterface vectorInterface = interfaces.getVectorInterface();
                    vectorInterface.createModelFromFile(
                            conn,
                            schemaName,
                            finalModelName,
                            modelBlob,
                            metadataJson
                    );
                }
                
                Dispatch.run(() -> Messages.showInfoDialog(
                        project,
                        "Export Complete",
                        "Model '" + finalModelName + "' exported successfully to Oracle Database!\n\n" +
                        "You can now use it in SQL:\n" +
                        "SELECT PREDICTION(" + finalModelName + " USING *) FROM your_table;"
                ));
                
            } catch (Exception ex) {
                Dispatch.run(() -> Messages.showErrorDialog(
                        project,
                        "Export Failed",
                        "Failed to export model to database: " + ex.getMessage()
                ));
            }
        });
    }

    private JPanel createPredictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input section
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Feature Values"));

        // Get feature names from model
        Model<Label> model = result.getModel();
        var featureMap = model.getFeatureIDMap();
        featureInputFields = new ArrayList<>();

        for (var featureInfo : featureMap) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
            JLabel label = new JLabel(featureInfo.getName() + ":");
            label.setPreferredSize(new java.awt.Dimension(150, 25));
            
            JBTextField textField = new JBTextField(10);
            textField.setText("0.0");
            featureInputFields.add(textField);
            
            row.add(label);
            row.add(textField);
            inputPanel.add(row);
        }

        // Predict button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton predictButton = new JButton("Predict");
        predictButton.addActionListener(e -> executePrediction());
        buttonPanel.add(predictButton);
        inputPanel.add(buttonPanel);

        // Result section
        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Prediction Result"));

        predictionResultLabel = new JLabel("Enter feature values and click Predict");
        predictionResultLabel.setFont(predictionResultLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        predictionDetailsArea = new JTextArea(5, 40);
        predictionDetailsArea.setEditable(false);
        predictionDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        resultPanel.add(predictionResultLabel, BorderLayout.NORTH);
        resultPanel.add(new JBScrollPane(predictionDetailsArea), BorderLayout.CENTER);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.WEST);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(resultPanel, BorderLayout.CENTER);

        return panel;
    }

    private void executePrediction() {
        try {
            Model<Label> model = result.getModel();
            var featureMap = model.getFeatureIDMap();
            
            // Collect feature names and values
            String[] featureNames = new String[featureInputFields.size()];
            double[] featureValues = new double[featureInputFields.size()];
            
            int i = 0;
            for (var featureInfo : featureMap) {
                featureNames[i] = featureInfo.getName();
                featureValues[i] = Double.parseDouble(featureInputFields.get(i).getText().trim());
                i++;
            }
            
            // Create example and predict
            ArrayExample<Label> example = new ArrayExample<>(
                    new Label("unknown"),
                    featureNames,
                    featureValues
            );
            
            Prediction<Label> prediction = model.predict(example);
            
            // Display result
            Label predictedLabel = prediction.getOutput();
            predictionResultLabel.setText("Predicted: " + predictedLabel.getLabel() + 
                    " (Score: " + String.format("%.4f", predictedLabel.getScore()) + ")");
            
            // Show all class probabilities
            StringBuilder details = new StringBuilder();
            details.append("=== Prediction Details ===\n\n");
            details.append("Predicted Class: ").append(predictedLabel.getLabel()).append("\n");
            details.append("Confidence Score: ").append(String.format("%.4f", predictedLabel.getScore())).append("\n\n");
            details.append("=== Class Scores ===\n");
            
            var outputScores = prediction.getOutputScores();
            for (var entry : outputScores.entrySet()) {
                details.append(String.format("  %s: %.4f\n", entry.getKey(), entry.getValue().getScore()));
            }
            
            details.append("\n=== Input Features ===\n");
            for (int j = 0; j < featureNames.length; j++) {
                details.append(String.format("  %s: %.4f\n", featureNames[j], featureValues[j]));
            }
            
            predictionDetailsArea.setText(details.toString());
            
        } catch (NumberFormatException ex) {
            Messages.showErrorDialog(
                    result.getConnection().getProject(),
                    "Invalid Input",
                    "Please enter valid numeric values for all features."
            );
        } catch (Exception ex) {
            Messages.showErrorDialog(
                    result.getConnection().getProject(),
                    "Prediction Failed",
                    "Failed to execute prediction: " + ex.getMessage()
            );
        }
    }

    private void addMetricBadge(JPanel panel, String label, String value) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        JLabel labelComp = new JLabel(label + ":");
        labelComp.setFont(labelComp.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(valueComp.getFont().deriveFont(Font.BOLD, 12f));
        badge.add(labelComp);
        badge.add(valueComp);
        panel.add(badge);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Training info
        JPanel trainingPanel = new JPanel();
        trainingPanel.setLayout(new BoxLayout(trainingPanel, BoxLayout.Y_AXIS));
        trainingPanel.setBorder(BorderFactory.createTitledBorder("Training Summary"));

        addInfoRow(trainingPanel, "Algorithm", result.getAlgorithmName());
        addInfoRow(trainingPanel, "Training Samples", String.valueOf(result.getTrainingDataSize()));
        addInfoRow(trainingPanel, "Testing Samples", String.valueOf(result.getTestingDataSize()));
        addInfoRow(trainingPanel, "Number of Features", String.valueOf(result.getFeatureCount()));
        addInfoRow(trainingPanel, "Number of Classes", String.valueOf(result.getClassCount()));
        addInfoRow(trainingPanel, "Training Time", result.getTrainingTimeMs() + " ms");

        // Evaluation metrics
        JPanel evalPanel = new JPanel();
        evalPanel.setLayout(new BoxLayout(evalPanel, BoxLayout.Y_AXIS));
        evalPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Metrics"));

        LabelEvaluation eval = result.getEvaluation();
        if (eval != null) {
            addInfoRow(evalPanel, "Accuracy", String.format("%.4f (%.2f%%)", eval.accuracy(), eval.accuracy() * 100));
            addInfoRow(evalPanel, "Micro Precision", String.format("%.4f", eval.microAveragedPrecision()));
            addInfoRow(evalPanel, "Micro Recall", String.format("%.4f", eval.microAveragedRecall()));
            addInfoRow(evalPanel, "Micro F1", String.format("%.4f", eval.microAveragedF1()));
            addInfoRow(evalPanel, "Macro Precision", String.format("%.4f", eval.macroAveragedPrecision()));
            addInfoRow(evalPanel, "Macro Recall", String.format("%.4f", eval.macroAveragedRecall()));
            addInfoRow(evalPanel, "Macro F1", String.format("%.4f", eval.macroAveragedF1()));
            addInfoRow(evalPanel, "Balanced Error Rate", String.format("%.4f", eval.balancedErrorRate()));
        }

        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        gridPanel.add(trainingPanel);
        gridPanel.add(evalPanel);

        panel.add(gridPanel, BorderLayout.NORTH);
        
        return panel;
    }

    private JPanel createPerClassMetricsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        LabelEvaluation eval = result.getEvaluation();
        if (eval == null) {
            panel.add(new JLabel("No evaluation data available"), BorderLayout.CENTER);
            return panel;
        }

        // Create table with per-class metrics
        String[] columns = {"Class", "n", "TP", "FN", "FP", "Recall", "Precision", "F1"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Get labels from confusion matrix domain
        var confusionMatrix = eval.getConfusionMatrix();
        var labelDomain = confusionMatrix.getDomain();
        
        // Add per-class rows
        for (Label label : labelDomain.getDomain()) {
            String className = label.getLabel();
            double n = confusionMatrix.support(label);
            double tp = confusionMatrix.tp(label);
            double fn = confusionMatrix.fn(label);
            double fp = confusionMatrix.fp(label);
            double recall = eval.recall(label);
            double precision = eval.precision(label);
            double f1 = eval.f1(label);

            tableModel.addRow(new Object[]{
                    className,
                    (int) n,
                    (int) tp,
                    (int) fn,
                    (int) fp,
                    String.format("%.3f", recall),
                    String.format("%.3f", precision),
                    String.format("%.3f", f1)
            });
        }

        // Add totals row
        tableModel.addRow(new Object[]{
                "Total",
                result.getTestingDataSize(),
                "-", "-", "-", "", "", ""
        });

        // Add averages
        tableModel.addRow(new Object[]{
                "Micro Average", "", "", "", "",
                String.format("%.3f", eval.microAveragedRecall()),
                String.format("%.3f", eval.microAveragedPrecision()),
                String.format("%.3f", eval.microAveragedF1())
        });

        tableModel.addRow(new Object[]{
                "Macro Average", "", "", "", "",
                String.format("%.3f", eval.macroAveragedRecall()),
                String.format("%.3f", eval.macroAveragedPrecision()),
                String.format("%.3f", eval.macroAveragedF1())
        });

        JBTable table = new JBTable(tableModel);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add accuracy summary at bottom
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        summaryPanel.add(new JLabel(String.format("Accuracy: %.4f", eval.accuracy())));
        summaryPanel.add(new JLabel(String.format("Balanced Error Rate: %.4f", eval.balancedErrorRate())));
        panel.add(summaryPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createConfusionMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String confusionMatrix = result.getConfusionMatrix();
        
        JTextArea textArea = new JTextArea(confusionMatrix);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel explanationLabel = new JLabel(
                "<html><i>Rows represent actual classes, columns represent predicted classes. " +
                "Diagonal values show correct predictions.</i></html>"
        );
        explanationLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(explanationLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createModelInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        StringBuilder info = new StringBuilder();
        info.append("=== Model Information ===\n\n");
        info.append("Algorithm: ").append(result.getAlgorithmName()).append("\n");
        info.append("Training Samples: ").append(result.getTrainingDataSize()).append("\n");
        info.append("Testing Samples: ").append(result.getTestingDataSize()).append("\n");
        info.append("Features: ").append(result.getFeatureCount()).append("\n");
        info.append("Classes: ").append(result.getClassCount()).append("\n");
        info.append("Training Time: ").append(result.getTrainingTimeMs()).append(" ms\n\n");
        
        // ONNX support info
        Model<Label> model = result.getModel();
        info.append("ONNX Export: ").append(model instanceof ONNXExportable ? "Supported" : "Not Supported").append("\n\n");

        info.append("=== Full Evaluation ===\n\n");
        info.append(result.getEvaluationSummary());

        JTextArea textArea = new JTextArea(info.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void addInfoRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        JLabel labelComponent = new JLabel(label + ":");
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.PLAIN));
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(valueComponent.getFont().deriveFont(Font.BOLD));
        
        row.add(labelComponent);
        row.add(valueComponent);
        panel.add(row);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
