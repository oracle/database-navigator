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
import org.tribuo.Model;
import org.tribuo.classification.Label;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for Oracle DB ONNX metadata management.
 * 
 * Oracle's DBMS_DATA_MINING.IMPORT_ONNX_MODEL requires metadata JSON to map:
 * - DB columns → ONNX input tensor positions
 * - ONNX output tensor → class labels
 * 
 * This class creates/reads sidecar JSON metadata files (.onnx.json) that 
 * accompany ONNX model files for Oracle DB deployment.
 * 
 * File convention:
 *   model.onnx      - The ONNX model file
 *   model.onnx.json - Oracle metadata JSON (created by this helper)
 */
public class OnnxMetadataHelper {
    
    /**
     * Suffix for Oracle metadata sidecar files
     */
    public static final String METADATA_SUFFIX = ".json";
    
    /**
     * Builds Oracle DB metadata JSON from ML result.
     * 
     * The JSON format required by Oracle:
     * {
     *   "function": "classification",
     *   "input": {"input": ["COL1", "COL2", ...]},
     *   "classificationProbOutput": "output",
     *   "labels": ["label1", "label2", ...]
     * }
     */
    public static String buildOracleMetadataJson(MLResult result) {
        List<String> featureColumns = result.getFeatureColumns();
        Model<Label> model = result.getModel();
        
        // Extract labels from model (sorted alphabetically - Tribuo convention)
        List<String> labels = model.getOutputIDInfo()
                .getDomain()
                .stream()
                .map(Label::getLabel)
                .sorted()
                .collect(Collectors.toList());
        
        return buildOracleMetadataJson(featureColumns, labels);
    }
    
    /**
     * Builds Oracle DB metadata JSON from feature columns and labels.
     */
    public static String buildOracleMetadataJson(List<String> featureColumns, List<String> labels) {
        // Sort labels alphabetically (Tribuo convention)
        List<String> sortedLabels = labels.stream().sorted().collect(Collectors.toList());
        
        // Build JSON manually to avoid extra dependencies
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
     * Saves Oracle metadata JSON as a sidecar file next to ONNX file.
     * 
     * Example: model.onnx → model.onnx.json
     * 
     * @param onnxPath Path to the ONNX file
     * @param metadataJson The Oracle metadata JSON string
     */
    public static void saveMetadataFile(Path onnxPath, String metadataJson) throws IOException {
        Path metadataPath = getMetadataPath(onnxPath);
        Files.writeString(metadataPath, metadataJson);
    }
    
    /**
     * Reads Oracle metadata JSON from sidecar file.
     * 
     * @param onnxPath Path to the ONNX file
     * @return The Oracle metadata JSON string, or null if not found
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
     * 
     * @param onnxPath Path to the ONNX file
     * @return Path to the metadata file (e.g., model.onnx → model.onnx.json)
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
