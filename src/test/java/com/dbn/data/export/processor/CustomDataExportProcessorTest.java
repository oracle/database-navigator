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

package com.dbn.data.export.processor;

import com.dbn.data.export.DataExportException;
import com.dbn.data.export.DataExportInstructions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomDataExportProcessorTest {
    @Test
    public void appendFieldQuotesHeaderNewline() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("SAFE\n=HYPERLINK(\"http://example.invalid\",\"x\")", instructions);

        assertEquals("\"SAFE\n=HYPERLINK(\"\"http://example.invalid\"\",\"\"x\"\")\"", exported);
    }

    @Test
    public void appendFieldQuotesCarriageReturn() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("SAFE\rNEXT", instructions);

        assertEquals("\"SAFE\rNEXT\"", exported);
    }

    @Test
    public void appendFieldEscapesQuotes() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("SAFE \"quoted\"", instructions);

        assertEquals("\"SAFE \"\"quoted\"\"\"", exported);
    }

    @Test
    public void appendFieldQuotesSeparatorEvenWhenSeparatorQuotingDisabled() {
        DataExportInstructions instructions = instructions();
        instructions.setQuoteValuesContainingSeparator(false);

        String exported = exportField("SAFE,ALIAS", instructions);

        assertEquals("\"SAFE,ALIAS\"", exported);
    }

    @Test
    public void appendFieldNeutralizesFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("=HYPERLINK(\"http://example.invalid\",\"x\")", instructions);

        assertEquals("\"'=HYPERLINK(\"\"http://example.invalid\"\",\"\"x\"\")\"", exported);
    }

    @Test
    public void appendFieldNeutralizesPlusFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("+SUM(1,2)", instructions);

        assertEquals("\"'+SUM(1,2)\"", exported);
    }

    @Test
    public void appendFieldNeutralizesMinusFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("-10+20", instructions);

        assertEquals("\"'-10+20\"", exported);
    }

    @Test
    public void appendFieldNeutralizesAtFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("@SUM(1,2)", instructions);

        assertEquals("\"'@SUM(1,2)\"", exported);
    }

    @Test
    public void appendFieldNeutralizesTabFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("\t=HYPERLINK(\"http://example.invalid\",\"x\")", instructions);

        assertEquals("\"'\t=HYPERLINK(\"\"http://example.invalid\"\",\"\"x\"\")\"", exported);
    }

    @Test
    public void appendFieldNeutralizesCarriageReturnFormulaPrefix() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("\r=HYPERLINK(\"http://example.invalid\",\"x\")", instructions);

        assertEquals("\"'\r=HYPERLINK(\"\"http://example.invalid\"\",\"\"x\"\")\"", exported);
    }

    @Test
    public void appendFieldPreservesBenignAlias() {
        DataExportInstructions instructions = instructions();

        String exported = exportField("SAFE_ALIAS", instructions);

        assertEquals("SAFE_ALIAS", exported);
    }

    @Test
    public void appendFieldQuotesAllValuesWhenRequested() {
        DataExportInstructions instructions = instructions();
        instructions.setQuoteAllValues(true);

        String exported = exportField("SAFE_ALIAS", instructions);

        assertEquals("\"SAFE_ALIAS\"", exported);
    }

    @Test
    public void appendFieldQuotesMultiCharacterSeparator() {
        DataExportInstructions instructions = instructions();
        instructions.setValueSeparator("||");

        String exported = exportField("SAFE||ALIAS", instructions);

        assertEquals("\"SAFE||ALIAS\"", exported);
    }

    @Test
    public void appendFieldEscapesCustomBeginAndEndQuotes() {
        DataExportInstructions instructions = instructions();
        instructions.setBeginQuote("[");
        instructions.setEndQuote("]");

        String exported = exportField("SAFE [quoted] alias", instructions);

        assertEquals("[SAFE [[quoted]] alias]", exported);
    }

    @Test
    public void appendFieldQuotesTabSeparatedFieldContainingTab() {
        DataExportInstructions instructions = instructions();
        instructions.setValueSeparator("\t");

        String exported = exportField("SAFE\tALIAS", instructions);

        assertEquals("\"SAFE\tALIAS\"", exported);
    }

    private static DataExportInstructions instructions() {
        DataExportInstructions instructions = new DataExportInstructions();
        instructions.setValueSeparator(",");
        instructions.setBeginQuote("\"");
        instructions.setEndQuote("\"");
        instructions.setQuoteValuesContainingSeparator(true);
        instructions.setQuoteAllValues(false);
        return instructions;
    }

    private static String exportField(String value, DataExportInstructions instructions) {
        try {
            DataExportBuffer buffer = new DataExportBuffer();
            CustomDataExportProcessor.appendField(buffer, value, instructions);
            return buffer.toString();
        } catch (DataExportException e) {
            throw new AssertionError(e);
        }
    }
}
