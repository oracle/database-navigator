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

package com.dbn.dev.language;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@NonNls
public class LanguageSpecificationLexerClassBuilder implements LanguageSpecificationArtifactBuilder {
    private static final String JFLEX_JAVA_PROPERTY = "jflexJava";             // e.g. /Applications/IntelliJ IDEA 26.1.app/Contents/jbr/Contents/Home/bin/java
    private static final String JFLEX_JAR_PROPERTY = "jflexJar";               // e.g. /Users/dcioca/Resources/libraries/jflex-1.9.1/jflex-1.9.1.jar
    private static final String JFLEX_SKELETON_PROPERTY = "jflexSkeleton";     // e.g. /Users/dcioca/Resources/libraries/jflex-1.9.1/idea-flex.skeleton

    private final LanguageSpecificationBuilderInput input;

    public LanguageSpecificationLexerClassBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @Override
    public void build() throws Exception {
        runJFlex(input.getParserLexerFile());
        runJFlex(input.getHighlighterLexerFile());
    }

    private void runJFlex(File flexFile) throws Exception {
        if (!flexFile.exists()) {
            throw new IllegalArgumentException("Flex definition does not exist: " + flexFile.getAbsolutePath());
        }

        File outputDirectory = flexFile.getParentFile();

        List<String> command = List.of(
                input.getRequiredProperty(JFLEX_JAVA_PROPERTY),
                "-Xmx512m",
                "-Dfile.encoding=UTF-8",
                "-Dsun.stdout.encoding=UTF-8",
                "-Dsun.stderr.encoding=UTF-8",
                "-jar",
                input.getRequiredProperty(JFLEX_JAR_PROPERTY),
                "-skel",
                input.getRequiredProperty(JFLEX_SKELETON_PROPERTY),
                "-d",
                outputDirectory.getAbsolutePath(),
                flexFile.getName());

        System.out.println("Working directory: " + outputDirectory.getAbsolutePath());
        System.out.println("Running: " + command.stream().map(LanguageSpecificationLexerClassBuilder::quote).collect(Collectors.joining(" ")));

        Process process = new ProcessBuilder(command).
                directory(outputDirectory).
                inheritIO().
                start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("JFlex failed with exit code " + exitCode);
        }
    }

    private static String quote(String value) {
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }

}
