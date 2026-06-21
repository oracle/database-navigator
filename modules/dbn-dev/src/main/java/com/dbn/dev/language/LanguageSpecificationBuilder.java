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

import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@NonNls
public class LanguageSpecificationBuilder {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final LanguageSpecificationBuilderInput input = new LanguageSpecificationBuilderInput();

    public static void main(String[] args) throws Exception {
        input.setDatabase(selectOption("database", LanguageSpecificationBuilderInput.DATABASE_OPTIONS));
        input.setLanguage(selectOption("language", LanguageSpecificationBuilderInput.LANGUAGE_OPTIONS));

        Artifact artifact = selectOption("artifact", LanguageSpecificationBuilderInput.ARTIFACT_OPTIONS);
        if (artifact == Artifact.ALL) {
            runAll();
            return;
        }

        Action action = selectOption("action", artifact.getActionOptions());

        if (artifact == Artifact.LEXER && action == Action.UPDATE_DEFINITION) {
            buildLexerDefinition();
        } else if (artifact == Artifact.PARSER && action == Action.UPDATE_DEFINITION) {
            buildParserDefinition();
        } else if (artifact == Artifact.LEXER && action == Action.BUILD_CLASS) {
            buildLexerClass();
        } else {
            throw new IllegalArgumentException("Unsupported action: " + artifact + " " + action);
        }
    }

    private static void runAll() throws Exception {
        buildLexerDefinition();
        buildLexerClass();
        buildParserDefinition();
    }

    private static void buildLexerDefinition() {
        LanguageSpecificationLexerBuilder lexerBuilder = new LanguageSpecificationLexerBuilder(input);
        lexerBuilder.build();
    }

    private static void buildLexerClass() throws Exception {
        LanguageSpecificationLexerClassBuilder lexerBuilder = new LanguageSpecificationLexerClassBuilder(input);
        lexerBuilder.build();
    }

    private static void buildParserDefinition() {
        LanguageSpecificationParserBuilder parserBuilder = new LanguageSpecificationParserBuilder(input);
        parserBuilder.build();
    }

    private static <T> T selectOption(String name, Map<String, T> options) {
        System.out.println("_______________________________________");
        System.out.print("Select " + name + " (x to exit)\n" + presentableOptions(options) + "\n");
        String s = SCANNER.next();

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

    private static String presentableOptions(Map<String, ?> options) {
        return options.keySet().stream().map(k -> k + " " + options.get(k)).collect(Collectors.joining("\n"));
    }
}
