/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.dbn.language.common.psi.obfuscator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DBLLanguageFileObfuscatorTest {
    @Test
    public void obfuscatesUnicodeLettersAcrossWritingSystems() {
        assertEquals("'####'", DBLLanguageFileObfuscator.obfuscateText("'Café'"));
        assertEquals("'#######'", DBLLanguageFileObfuscator.obfuscateText("'这个杀手不太冷'"));
        assertEquals("'###'", DBLLanguageFileObfuscator.obfuscateText("'日本語'"));
        assertEquals("'###'", DBLLanguageFileObfuscator.obfuscateText("'한국어'"));
        assertEquals("'#######'", DBLLanguageFileObfuscator.obfuscateText("'Русский'"));
        assertEquals("'######'", DBLLanguageFileObfuscator.obfuscateText("'Ελλάδα'"));
        assertEquals("'#######'", DBLLanguageFileObfuscator.obfuscateText("'العربية'"));
    }

    @Test
    public void obfuscatesUnicodeNumbers() {
        assertEquals("'###-###'", DBLLanguageFileObfuscator.obfuscateText("'١٢٣-１２３'"));
    }

    @Test
    public void preservesLiteralSyntaxCharacters() {
        assertEquals("'##-##'", DBLLanguageFileObfuscator.obfuscateText("'电影-名称'"));
        assertEquals("'#-#_#@#.### !'", DBLLanguageFileObfuscator.obfuscateText("'a-b_c@d.com !'"));
        assertEquals("'##  ##'", DBLLanguageFileObfuscator.obfuscateText("'中文  日本'"));
        assertEquals("'#''##'", DBLLanguageFileObfuscator.obfuscateText("'a''中文'"));
    }

    @Test
    public void obfuscatesCommentTextWithoutRemovingCommentSyntax() {
        assertEquals("-- ##", DBLLanguageFileObfuscator.obfuscateComment("-- 电影"));
        assertEquals("  REM ##", DBLLanguageFileObfuscator.obfuscateComment("  REM 电影"));
    }
}
