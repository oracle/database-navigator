/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.dbn.language.common.psi.scrambler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DBLLanguageFileScramblerTest {
    @Test
    public void scramblesUnicodeLettersAcrossWritingSystems() {
        assertEquals("'####'", DBLLanguageFileScrambler.scrambleText("'Café'"));
        assertEquals("'#######'", DBLLanguageFileScrambler.scrambleText("'这个杀手不太冷'"));
        assertEquals("'###'", DBLLanguageFileScrambler.scrambleText("'日本語'"));
        assertEquals("'###'", DBLLanguageFileScrambler.scrambleText("'한국어'"));
        assertEquals("'#######'", DBLLanguageFileScrambler.scrambleText("'Русский'"));
        assertEquals("'######'", DBLLanguageFileScrambler.scrambleText("'Ελλάδα'"));
        assertEquals("'#######'", DBLLanguageFileScrambler.scrambleText("'العربية'"));
    }

    @Test
    public void scramblesUnicodeNumbers() {
        assertEquals("'###-###'", DBLLanguageFileScrambler.scrambleText("'١٢٣-１２３'"));
    }

    @Test
    public void preservesLiteralSyntaxCharacters() {
        assertEquals("'##-##'", DBLLanguageFileScrambler.scrambleText("'电影-名称'"));
        assertEquals("'#-#_#@#.### !'", DBLLanguageFileScrambler.scrambleText("'a-b_c@d.com !'"));
        assertEquals("'##  ##'", DBLLanguageFileScrambler.scrambleText("'中文  日本'"));
        assertEquals("'#''##'", DBLLanguageFileScrambler.scrambleText("'a''中文'"));
    }

    @Test
    public void scramblesCommentTextWithoutRemovingCommentSyntax() {
        assertEquals("-- ##", DBLLanguageFileScrambler.scrambleComment("-- 电影"));
        assertEquals("  REM ##", DBLLanguageFileScrambler.scrambleComment("  REM 电影"));
    }
}
