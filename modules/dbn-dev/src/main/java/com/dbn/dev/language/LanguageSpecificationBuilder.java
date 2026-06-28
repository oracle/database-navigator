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

import com.dbn.dev.language.LanguageSpecificationBuilderInput.Action;
import com.dbn.dev.language.LanguageSpecificationBuilderInput.Artifact;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@NonNls
public class LanguageSpecificationBuilder {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Deque<String> INPUT_BUFFER = new ArrayDeque<>();
    private static final LanguageSpecificationBuilderInput input = new LanguageSpecificationBuilderInput();
    private static Artifact selectedArtifact;
    private static Action selectedAction;

    public static void main(String[] args) throws Exception {
        input.setDatabase(selectOption("database", LanguageSpecificationBuilderInput.DATABASE_OPTIONS));
        input.setLanguage(selectOption("language", LanguageSpecificationBuilderInput.LANGUAGE_OPTIONS));

        selectedArtifact = selectOption("artifact", LanguageSpecificationBuilderInput.ARTIFACT_OPTIONS);
        if (selectedArtifact == Artifact.ALL) {
            selectedAction = Action.ALL;
            runAll();
            return;
        }

        selectedAction = selectOption("action", selectedArtifact.getActionOptions());

        if (selectedArtifact == Artifact.LEXER && selectedAction == Action.UPDATE_DEFINITION) {
            buildLexerDefinition();
        } else if (selectedArtifact == Artifact.PARSER && selectedAction == Action.UPDATE_DEFINITION) {
            buildParserDefinition();
        } else if (selectedArtifact == Artifact.LEXER && selectedAction == Action.BUILD_CLASS) {
            buildLexerClass();
        } else if (selectedArtifact == Artifact.PARSER && selectedAction == Action.BUILD_EXTENSION) {
            buildParserExtension();
        } else if (selectedArtifact == Artifact.LEXER && selectedAction == Action.ALL) {
            buildLexerDefinition();
            buildLexerClass();
        } else if (selectedArtifact == Artifact.PARSER && selectedAction == Action.ALL) {
            buildParserDefinition();
            buildParserExtension();
        } else {
            throw new IllegalArgumentException("Unsupported action: " + selectedArtifact + " " + selectedAction);
        }
    }

    private static void runAll() throws Exception {
        buildLexerDefinition();
        buildLexerClass();
        buildParserDefinition();
        buildParserExtension();
    }

    private static void buildLexerDefinition() throws Exception {
        build(new LanguageSpecificationLexerBuilder(input));
    }

    private static void buildLexerClass() throws Exception {
        build(new LanguageSpecificationLexerClassBuilder(input));
    }

    private static void buildParserDefinition() throws Exception {
        build(new LanguageSpecificationParserBuilder(input));
    }

    private static void buildParserExtension() throws Exception {
        build(new LanguageSpecificationParserExtensionBuilder(input));
    }

    private static void build(LanguageSpecificationArtifactBuilder builder) throws Exception {
        String operation = builder.getClass().getSimpleName()
                .replace("LanguageSpecification", "")
                .replace("Builder", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toUpperCase();
        String title = input.database + " " + input.languageFid.toUpperCase() +
                " | ARTIFACT " + selectedArtifact +
                " | ACTION " + selectedAction +
                " | OPERATION " + operation;
        printBanner("START " + title);
        try {
            builder.build();
        } finally {
            printBanner("END " + title);
        }
    }

    private static void printBanner(String title) {
        String line = "=".repeat(80);
        System.out.println();
        System.out.println(line);
        System.out.println("=== " + title);
        System.out.println(line);
    }

    private static <T> T selectOption(String name, Map<String, T> options) {
        System.out.println("_______________________________________");
        System.out.print("Select " + name + " (x to exit)\n" + presentableOptions(options) + "\n");
        String s = readOptionInput();

        T option = options.get(s.toLowerCase());
        if (option != null) {
            System.out.println("Selected " + name + ": " + option);
            return option;
        }

        if (s.equalsIgnoreCase("x")) {
            System.out.println("Bye bye!");
            System.exit(0);
            return null;
        }

        System.out.println("Invalid option: " + s);
        return selectOption(name, options);
    }

    private static String readOptionInput() {
        if (!INPUT_BUFFER.isEmpty()) {
            return INPUT_BUFFER.removeFirst();
        }

        String input = SCANNER.next();
        if (input.length() > 1) {
            for (int i = 1; i < input.length(); i++) {
                INPUT_BUFFER.addLast(String.valueOf(input.charAt(i)));
            }
            return input.substring(0, 1);
        }
        return input;
    }

    private static String presentableOptions(Map<String, ?> options) {
        return options.keySet().stream().map(k -> k + " " + options.get(k)).collect(Collectors.joining("\n"));
    }
}
