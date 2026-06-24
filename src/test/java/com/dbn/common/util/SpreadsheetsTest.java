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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpreadsheetsTest {
    @Test
    public void isSpreadsheetFormulaRiskDetectsSpreadsheetPrefixes() {
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("=HYPERLINK(\"http://example.invalid\",\"x\")"));
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("+SUM(1,2)"));
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("-10+20"));
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("@SUM(1,2)"));
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("\t=HYPERLINK(\"http://example.invalid\",\"x\")"));
        assertTrue(Spreadsheets.isSpreadsheetFormulaRisk("\r=HYPERLINK(\"http://example.invalid\",\"x\")"));
    }

    @Test
    public void isSpreadsheetFormulaRiskIgnoresBenignValues() {
        assertFalse(Spreadsheets.isSpreadsheetFormulaRisk(""));
        assertFalse(Spreadsheets.isSpreadsheetFormulaRisk("SAFE_ALIAS"));
        assertFalse(Spreadsheets.isSpreadsheetFormulaRisk("SAFE=ALIAS"));
    }

    @Test
    public void neutralizeSpreadsheetFormulaPrefixesFormulaRiskValues() {
        assertEquals("'=HYPERLINK(\"http://example.invalid\",\"x\")", Spreadsheets.neutralizeSpreadsheetFormula("=HYPERLINK(\"http://example.invalid\",\"x\")"));
        assertEquals("'+SUM(1,2)", Spreadsheets.neutralizeSpreadsheetFormula("+SUM(1,2)"));
        assertEquals("'-10+20", Spreadsheets.neutralizeSpreadsheetFormula("-10+20"));
        assertEquals("'@SUM(1,2)", Spreadsheets.neutralizeSpreadsheetFormula("@SUM(1,2)"));
        assertEquals("'\t=HYPERLINK(\"http://example.invalid\",\"x\")", Spreadsheets.neutralizeSpreadsheetFormula("\t=HYPERLINK(\"http://example.invalid\",\"x\")"));
        assertEquals("'\r=HYPERLINK(\"http://example.invalid\",\"x\")", Spreadsheets.neutralizeSpreadsheetFormula("\r=HYPERLINK(\"http://example.invalid\",\"x\")"));
    }

    @Test
    public void neutralizeSpreadsheetFormulaPreservesBenignValues() {
        assertEquals("", Spreadsheets.neutralizeSpreadsheetFormula(""));
        assertEquals("SAFE_ALIAS", Spreadsheets.neutralizeSpreadsheetFormula("SAFE_ALIAS"));
    }
}
