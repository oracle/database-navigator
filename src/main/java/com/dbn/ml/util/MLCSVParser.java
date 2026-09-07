/*
 * Copyright 2025-2026 Oracle and/or its affiliates
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

package com.dbn.ml.util;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static com.dbn.nls.NlsResources.txt;

/**
 * Parses and profiles CSV sources used by the Machine Learning Toolbox.
 */
public final class MLCSVParser {
    public static final int CLOUD_SAMPLE_ROWS = 200;
    public static final int MAX_TEXT_LENGTH = 4000;
    public static final int MAX_IDENTIFIER_LENGTH = 128;

    private static final Pattern ORACLE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0," + (MAX_IDENTIFIER_LENGTH - 1) + "}");

    private MLCSVParser() {}

    public static CSVReader createReader(Reader reader, String delimiter) {
        char separator = parseDelimiter(delimiter);
        return new CSVReaderBuilder(reader)
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build();
    }

    public static Profile profile(Reader source, String delimiter, boolean hasHeader, int maximumRows)
            throws IOException, CsvValidationException {
        try (CSVReader reader = createReader(source, delimiter)) {
            String[] firstRecord = reader.readNext();
            if (firstRecord == null) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.EmptyCsvFile"));
            }

            List<String> columns = hasHeader ? normalizeHeaders(firstRecord) : generatedColumns(firstRecord.length);
            boolean[] numeric = new boolean[columns.size()];
            boolean[] populated = new boolean[columns.size()];
            Arrays.fill(numeric, true);

            int rowCount = 0;
            long recordNumber = 1;
            if (!hasHeader && !isEmpty(firstRecord)) {
                inspectRow(firstRecord, columns.size(), numeric, populated, recordNumber);
                rowCount++;
            }

            String[] values;
            while ((maximumRows <= 0 || rowCount < maximumRows) && (values = reader.readNext()) != null) {
                recordNumber++;
                if (isEmpty(values)) continue;

                inspectRow(values, columns.size(), numeric, populated, recordNumber);
                rowCount++;
            }

            if (rowCount == 0) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.CsvDataRowsMissing"));
            }

            Set<String> numericColumns = new HashSet<>();
            for (int i = 0; i < columns.size(); i++) {
                if (numeric[i] && populated[i]) {
                    numericColumns.add(columns.get(i));
                }
            }
            return new Profile(columns, numericColumns);
        }
    }

    public static List<String> readColumns(Reader source, String delimiter, boolean hasHeader)
            throws IOException, CsvValidationException {
        try (CSVReader reader = createReader(source, delimiter)) {
            String[] firstRecord = reader.readNext();
            if (firstRecord == null) return List.of();
            return hasHeader ? normalizeHeaders(firstRecord) : generatedColumns(firstRecord.length);
        }
    }

    public static List<String> normalizeHeaders(String[] values) {
        List<String> columns = new ArrayList<>(values.length);
        Set<String> uniqueColumns = new LinkedHashSet<>();
        for (int i = 0; i < values.length; i++) {
            columns.add(uniqueColumnName(normalizeHeader(values[i], i + 1), uniqueColumns));
        }
        return columns;
    }

    public static void validateColumnNames(List<String> columns) {
        Set<String> uniqueColumns = new HashSet<>();
        for (String column : columns) {
            if (!ORACLE_IDENTIFIER.matcher(column).matches()) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.CsvInvalidColumnName", column));
            }

            if (!uniqueColumns.add(column.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.CsvDuplicateColumnName", column));
            }
        }
    }

    public static void validateRow(String[] values, int expectedColumns, long rowNumber) {
        if (values.length != expectedColumns) {
            throw new IllegalArgumentException(txt(
                    "msg.machineLearning.exception.CsvColumnCountMismatch",
                    rowNumber,
                    values.length,
                    expectedColumns));
        }
    }

    public static boolean isEmpty(String[] values) {
        return values.length == 0 || Arrays.stream(values).allMatch(String::isBlank);
    }

    public static boolean isNumeric(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void inspectRow(
            String[] values,
            int columnCount,
            boolean[] numeric,
            boolean[] populated,
            long rowNumber) {
        validateRow(values, columnCount, rowNumber);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            if (value.isEmpty()) continue;
            if (value.length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException(txt(
                        "msg.machineLearning.exception.CsvValueTooLong",
                        rowNumber,
                        i + 1,
                        MAX_TEXT_LENGTH));
            }

            populated[i] = true;
            if (!isNumeric(value)) {
                numeric[i] = false;
            }
        }
    }

    private static List<String> generatedColumns(int columnCount) {
        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add("COLUMN_" + i);
        }
        return columns;
    }

    private static String normalizeHeader(String value, int columnIndex) {
        String header = removeBom(value).trim().toUpperCase(Locale.ROOT);
        StringBuilder identifier = new StringBuilder(header.length());
        boolean separatorPending = false;
        for (int i = 0; i < header.length(); i++) {
            char character = header.charAt(i);
            if (isIdentifierCharacter(character)) {
                if (separatorPending && identifier.length() > 0) identifier.append('_');
                identifier.append(character);
                separatorPending = false;
            } else {
                separatorPending = true;
            }
        }

        if (identifier.isEmpty()) return "COLUMN_" + columnIndex;
        if (!Character.isLetter(identifier.charAt(0))) identifier.insert(0, "COLUMN_");
        return identifier.substring(0, Math.min(identifier.length(), MAX_IDENTIFIER_LENGTH));
    }

    private static String uniqueColumnName(String baseName, Set<String> names) {
        String name = baseName;
        for (int suffix = 2; !names.add(name); suffix++) {
            String duplicateSuffix = "_" + suffix;
            int baseLength = MAX_IDENTIFIER_LENGTH - duplicateSuffix.length();
            name = baseName.substring(0, Math.min(baseName.length(), baseLength)) + duplicateSuffix;
        }
        return name;
    }

    private static boolean isIdentifierCharacter(char character) {
        return character >= 'A' && character <= 'Z' ||
                character >= '0' && character <= '9' ||
                character == '_' || character == '$' || character == '#';
    }

    private static char parseDelimiter(String delimiter) {
        if (delimiter == null || delimiter.length() != 1) {
            throw new IllegalArgumentException(txt("msg.machineLearning.exception.CsvInvalidDelimiter"));
        }
        return delimiter.charAt(0);
    }

    private static String removeBom(String value) {
        if (value == null) return "";
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    public static final class Profile {
        private final List<String> columns;
        private final Set<String> numericColumns;

        private Profile(List<String> columns, Set<String> numericColumns) {
            this.columns = List.copyOf(columns);
            this.numericColumns = Set.copyOf(numericColumns);
        }

        public List<String> getColumns() {
            return columns;
        }

        public Set<String> getNumericColumns() {
            return numericColumns;
        }
    }
}
