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

package com.dbn.ml.onnx;

import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.MLTaskType;
import org.tribuo.Model;
import org.tribuo.classification.Label;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for Oracle DB ONNX metadata management.
 */
public class OnnxMetadataHelper {
    
    public static final String METADATA_SUFFIX = ".json";
    
    /**
     * Builds Oracle DB metadata JSON from ML result.
     * Automatically detects task type (classification or regression).
     */
    public static String buildOracleMetadataJson(MLResult result) {
        MLTaskType taskType = result.getTaskType();
        List<String> featureColumns = result.getFeatureColumns();
        List<String> labelColumns = result.getLabelColumns();
        
        return switch (taskType) {
            case CLASSIFICATION -> buildClassificationMetadata(result, featureColumns);
            case REGRESSION -> buildRegressionMetadata(featureColumns, labelColumns);
        };
    }
    
    /**
     * Builds Oracle DB metadata JSON for classification models.
     */
    @SuppressWarnings("unchecked")
    private static String buildClassificationMetadata(MLResult result, List<String> featureColumns) {
        Model<Label> model = (Model<Label>) result.getTribuoModel();
        
        // Extract labels from model (sorted alphabetically - Tribuo convention)
        List<String> labels = model.getOutputIDInfo()
                .getDomain()
                .stream()
                .map(Label::getLabel)
                .sorted()
                .collect(Collectors.toList());
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"function\": \"classification\",\n");
        json.append("  \"input\": {\n");
        json.append("    \"input\": [");
        json.append(featureColumns.stream()
                .map(c -> "\"" + escapeJson(c) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("]\n");
        json.append("  },\n");
        json.append("  \"classificationProbOutput\": \"output\",\n");
        json.append("  \"labels\": [");
        json.append(labels.stream()
                .map(l -> "\"" + escapeJson(l) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("]\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Builds Oracle DB metadata JSON for regression models.
     * Supports both single-output and multi-output regression.
     */
    private static String buildRegressionMetadata(List<String> featureColumns, List<String> labelColumns) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"function\": \"regression\",\n");
        json.append("  \"input\": {\n");
        json.append("    \"input\": [");
        json.append(featureColumns.stream()
                .map(c -> "\"" + escapeJson(c) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("]\n");
        json.append("  },\n");
        
        // Multi-output regression requires explicit output attribute names
        if (labelColumns != null && labelColumns.size() > 1) {
            json.append("  \"output\": {\n");
            json.append("    \"output\": [");
            json.append(labelColumns.stream()
                    .map(c -> "\"" + escapeJson(c) + "\"")
                    .collect(Collectors.joining(", ")));
            json.append("]\n");
            json.append("  }\n");
        } else {
            json.append("  \"regressionOutput\": \"output\"\n");
        }
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Builds Oracle DB metadata JSON from feature columns and labels (classification).
     */
    public static String buildClassificationMetadataJson(List<String> featureColumns, List<String> labels) {
        List<String> sortedLabels = labels.stream().sorted().collect(Collectors.toList());
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"function\": \"classification\",\n");
        json.append("  \"input\": {\n");
        json.append("    \"input\": [");
        json.append(featureColumns.stream()
                .map(c -> "\"" + escapeJson(c) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("]\n");
        json.append("  },\n");
        json.append("  \"classificationProbOutput\": \"output\",\n");
        json.append("  \"labels\": [");
        json.append(sortedLabels.stream()
                .map(l -> "\"" + escapeJson(l) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("]\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Builds Oracle DB metadata JSON from feature columns (regression).
     * For single-output regression.
     */
    public static String buildRegressionMetadataJson(List<String> featureColumns) {
        return buildRegressionMetadata(featureColumns, null);
    }
    
    /**
     * Builds Oracle DB metadata JSON from feature columns and label columns (multi-output regression).
     */
    public static String buildRegressionMetadataJson(List<String> featureColumns, List<String> labelColumns) {
        return buildRegressionMetadata(featureColumns, labelColumns);
    }
    
    /**
     * Saves Oracle metadata JSON as a sidecar file next to ONNX file.
     */
    public static void saveMetadataFile(Path onnxPath, String metadataJson) throws IOException {
        Path metadataPath = getMetadataPath(onnxPath);
        Files.writeString(metadataPath, metadataJson);
    }
    
    /**
     * Reads Oracle metadata JSON from sidecar file.
     */
    public static String readMetadataFile(Path onnxPath) throws IOException {
        Path metadataPath = getMetadataPath(onnxPath);
        if (Files.exists(metadataPath)) {
            return Files.readString(metadataPath);
        }
        return null;
    }
    
    /**
     * Gets the metadata sidecar file path for an ONNX file.
     */
    public static Path getMetadataPath(Path onnxPath) {
        return onnxPath.resolveSibling(onnxPath.getFileName() + METADATA_SUFFIX);
    }
    
    /**
     * Checks if a metadata sidecar file exists for an ONNX file.
     */
    public static boolean hasMetadataFile(Path onnxPath) {
        return Files.exists(getMetadataPath(onnxPath));
    }
    
    /**
     * Escapes special characters for JSON string values.
     */
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
