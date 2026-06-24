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

package com.dbn.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Spreadsheets {
    /**
     * Prefixes spreadsheet-interpretable values with an apostrophe so pasted/exported cells remain text.
     */
    public static String neutralizeSpreadsheetFormula(String value) {
        return isSpreadsheetFormulaRisk(value) ? "'" + value : value;
    }

    /**
     * Matches prefixes commonly interpreted as formulas by spreadsheet clients.
     */
    public static boolean isSpreadsheetFormulaRisk(String value) {
        if (value.isEmpty()) return false;

        char firstChar = value.charAt(0);
        if (firstChar == '=') return true;
        if (firstChar == '+') return true;
        if (firstChar == '-') return true;
        if (firstChar == '@') return true;
        if (firstChar == '\t') return true;
        if (firstChar == '\r') return true;
        return false;
    }
}
