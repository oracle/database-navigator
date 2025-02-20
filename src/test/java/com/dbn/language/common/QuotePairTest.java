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

package com.dbn.language.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuotePairTest {

    @Test
    public void isPossibleBeginQuote() {
        // Arrange
        new QuotePair('"', '"');
        new QuotePair('\'', '\'');
        new QuotePair('`', '`');
        new QuotePair('[', ']');

        // Act & Assert
        assertTrue(QuotePair.isPossibleBeginQuote('"'));
        assertTrue(QuotePair.isPossibleBeginQuote('\''));
        assertTrue(QuotePair.isPossibleBeginQuote('`'));
        assertTrue(QuotePair.isPossibleBeginQuote('['));
        assertFalse(QuotePair.isPossibleBeginQuote('(')); // Not added
    }

    @Test
    public void isPossibleEndQuote() {
        // Arrange
        new QuotePair('"', '"');
        new QuotePair('\'', '\'');
        new QuotePair('`', '`');
        new QuotePair('[', ']');

        // Act & Assert
        assertTrue(QuotePair.isPossibleEndQuote('"'));
        assertTrue(QuotePair.isPossibleEndQuote('\''));
        assertTrue(QuotePair.isPossibleEndQuote('`'));
        assertTrue(QuotePair.isPossibleEndQuote(']'));
        assertFalse(QuotePair.isPossibleEndQuote('}')); // Not added
    }

    @Test
    public void beginQuote() {
        // Arrange
        QuotePair squareBracketPair = new QuotePair('[', ']');

        // Act
        String beginQuote = squareBracketPair.beginQuote();

        // Assert
        assertEquals("[", beginQuote);
    }

    @Test
    public void endQuote() {
        // Arrange
        QuotePair backTickPair = new QuotePair('`', '`');

        // Act
        String endQuote = backTickPair.endQuote();

        // Assert
        assertEquals("`", endQuote);
    }

    @Test
    public void isQuoted() {
        // Arrange
        QuotePair simpleQuotePair = new QuotePair('"', '"');
        QuotePair backTickPair = new QuotePair('`', '`');
        QuotePair squareBracketPair = new QuotePair('[', ']');

        String simpleQuoted = "\"test\"";
        String simpleUnquoted = "test";

        String backTickQuoted = "`test`";
        String backTickUnquoted = "test";

        String squareBracketQuoted = "[test]";
        String squareBracketUnquoted = "test";

        // Act & Assert (Simple quotes)
        assertTrue(simpleQuotePair.isQuoted(simpleQuoted));
        assertFalse(simpleQuotePair.isQuoted(simpleUnquoted));

        // Act & Assert (Backticks)
        assertTrue(backTickPair.isQuoted(backTickQuoted));
        assertFalse(backTickPair.isQuoted(backTickUnquoted));

        // Act & Assert (Square Brackets)
        assertTrue(squareBracketPair.isQuoted(squareBracketQuoted));
        assertFalse(squareBracketPair.isQuoted(squareBracketUnquoted));
    }

    @Test
    public void quote() {
        // Arrange
        QuotePair backTickPair = new QuotePair('`', '`');
        QuotePair squareBracketPair = new QuotePair('[', ']');

        String unquoted = "test";
        String alreadyBackTickQuoted = "`test`";
        String alreadySquareBracketQuoted = "[test]";

        // Act (Backticks)
        String backTickResult1 = backTickPair.quote(unquoted);
        String backTickResult2 = backTickPair.quote(alreadyBackTickQuoted);

        // Act (Square Brackets)
        String squareBracketResult1 = squareBracketPair.quote(unquoted);
        String squareBracketResult2 = squareBracketPair.quote(alreadySquareBracketQuoted);

        // Assert (Backticks)
        assertEquals("`test`", backTickResult1);
        assertEquals("`test`", backTickResult2);

        // Assert (Square Brackets)
        assertEquals("[test]", squareBracketResult1);
        assertEquals("[test]", squareBracketResult2);
    }

    @Test
    public void unquote() {
        // Arrange
        QuotePair backTickPair = new QuotePair('`', '`');
        QuotePair squareBracketPair = new QuotePair('[', ']');

        String backTickQuoted = "`test`";
        String backTickUnquoted = "test";

        String squareBracketQuoted = "[test]";
        String squareBracketUnquoted = "test";

        // Act (Backticks)
        String backTickResult1 = backTickPair.unquote(backTickQuoted);
        String backTickResult2 = backTickPair.unquote(backTickUnquoted);

        // Act (Square Brackets)
        String squareBracketResult1 = squareBracketPair.unquote(squareBracketQuoted);
        String squareBracketResult2 = squareBracketPair.unquote(squareBracketUnquoted);

        // Assert (Backticks)
        assertEquals("test", backTickResult1);
        assertEquals("test", backTickResult2);

        // Assert (Square Brackets)
        assertEquals("test", squareBracketResult1);
        assertEquals("test", squareBracketResult2);
    }

    @Test
    public void edgeCases() {
        // Arrange
        QuotePair backTickPair = new QuotePair('`', '`');
        QuotePair squareBracketPair = new QuotePair('[', ']');

        // Edge: Malformed quotes for backticks
        String malformedBackTickStart = "`test";
        String malformedBackTickEnd = "test`";
        String malformedBackTickMiddle = "te`st";

        // Edge: Unbalanced square brackets
        String unbalancedSquareBracketStart = "[test";
        String unbalancedSquareBracketEnd = "test]";
        String unmatchedBrackets = "]test[";

        // Act & Assert (Backticks)
        assertFalse(backTickPair.isQuoted(malformedBackTickStart));
        assertFalse(backTickPair.isQuoted(malformedBackTickEnd));
        assertFalse(backTickPair.isQuoted(malformedBackTickMiddle));

        // Act & Assert (Square Brackets)
        assertFalse(squareBracketPair.isQuoted(unbalancedSquareBracketStart));
        assertFalse(squareBracketPair.isQuoted(unbalancedSquareBracketEnd));
        assertFalse(squareBracketPair.isQuoted(unmatchedBrackets));
    }

    @Test
    public void chainedQuoteUnquote() {
        // Arrange
        QuotePair squareBracketPair = new QuotePair('[', ']');
        String original = "test";

        // Act
        String quotedOnce = squareBracketPair.quote(original);
        String quotedTwice = squareBracketPair.quote(quotedOnce);
        String unquotedOnce = squareBracketPair.unquote(quotedTwice);
        String unquotedTwice = squareBracketPair.unquote(unquotedOnce);

        // Assert
        assertEquals("[test]", quotedOnce);              // Apply brackets once
        assertEquals("[test]", quotedTwice);             // Nested brackets
        assertEquals("test", unquotedOnce);            // Remove one level of brackets
        assertEquals("test", unquotedTwice);             // Completely unquoted
    }


}