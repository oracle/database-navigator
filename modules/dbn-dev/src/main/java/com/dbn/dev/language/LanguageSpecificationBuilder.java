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
import com.dbn.language.common.element.impl.ElementTypeIdCache;
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

    public static void main(String[] args) throws Exception {
        try {
            runSelection();
        } catch (ExitRequestedException e) {
            System.out.println("Bye bye!");
        } finally {
            INPUT_BUFFER.clear();
            clearRunState();
        }
    }

    private static void clearRunState() {
        ElementTypeIdCache.clear();
    }

    private static void runSelection() throws Exception {
        BuildSession session = new BuildSession();
        session.input.setDatabase(selectOption("database", LanguageSpecificationBuilderInput.DATABASE_OPTIONS));
        session.input.setLanguage(selectOption("language", LanguageSpecificationBuilderInput.LANGUAGE_OPTIONS));

        session.selectedArtifact = selectOption("artifact", LanguageSpecificationBuilderInput.ARTIFACT_OPTIONS);
        if (session.selectedArtifact == Artifact.ALL) {
            buildAll(session);
            return;
        }

        session.selectedAction = selectOption("action", session.selectedArtifact.getActionOptions());

        if (session.selectedArtifact == Artifact.LEXER && session.selectedAction == Action.UPDATE_DEFINITION) {
            buildLexerDefinition(session);
        } else if (session.selectedArtifact == Artifact.PARSER && session.selectedAction == Action.UPDATE_DEFINITION) {
            buildParserDefinition(session);
        } else if (session.selectedArtifact == Artifact.LEXER && session.selectedAction == Action.BUILD_CLASS) {
            buildLexerClass(session);
        } else if (session.selectedArtifact == Artifact.PARSER && session.selectedAction == Action.BUILD_EXTENSION) {
            buildParserExtension(session);
        } else if (session.selectedArtifact == Artifact.LEXER && session.selectedAction == Action.ALL) {
            buildLexerDefinition(session);
            buildLexerClass(session);
        } else if (session.selectedArtifact == Artifact.PARSER && session.selectedAction == Action.ALL) {
            buildParserDefinition(session);
            buildParserExtension(session);
        } else {
            throw new IllegalArgumentException("Unsupported action: " + session.selectedArtifact + " " + session.selectedAction);
        }
    }

    private static void buildAll(BuildSession session) throws Exception {
        buildLexerDefinition(session);
        buildLexerClass(session);
        buildParserDefinition(session);
        buildParserExtension(session);
    }

    private static void buildLexerDefinition(BuildSession session) throws Exception {
        build(new LanguageSpecificationLexerBuilder(session.input), session, Artifact.LEXER, Action.UPDATE_DEFINITION);
    }

    private static void buildLexerClass(BuildSession session) throws Exception {
        build(new LanguageSpecificationLexerClassBuilder(session.input), session, Artifact.LEXER, Action.BUILD_CLASS);
    }

    private static void buildParserDefinition(BuildSession session) throws Exception {
        build(new LanguageSpecificationParserBuilder(session.input), session, Artifact.PARSER, Action.UPDATE_DEFINITION);
    }

    private static void buildParserExtension(BuildSession session) throws Exception {
        LanguageSpecificationArtifactBuilder builder = session.input.isParserExtBuilderEnabled() ?
                new LanguageSpecificationParserExtBuilder(session.input) :
                new LanguageSpecificationParserExtensionBuilder(session.input);
        build(builder, session, Artifact.PARSER, Action.BUILD_EXTENSION);
    }

    private static void build(
            LanguageSpecificationArtifactBuilder builder,
            BuildSession session,
            Artifact artifact,
            Action action) throws Exception {
        String title = "database " + session.input.database +
                " | language " + session.input.languageFid.toUpperCase() +
                " | artifact " + artifact.toString().toUpperCase() +
                " | action " + action.toString().toUpperCase();
        printBanner("START: " + title);
        try {
            builder.build();
        } finally {
            printBanner("END: " + title);
        }
    }

    private static void printBanner(String title) {
        String line = "=".repeat(80);
        System.out.println();
        System.out.println(line);
        System.out.println(title);
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
            throw new ExitRequestedException();
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

    private static class ExitRequestedException extends RuntimeException {
    }

    private static class BuildSession {
        private final LanguageSpecificationBuilderInput input = new LanguageSpecificationBuilderInput();
        private Artifact selectedArtifact;
        private Action selectedAction;
    }
}
