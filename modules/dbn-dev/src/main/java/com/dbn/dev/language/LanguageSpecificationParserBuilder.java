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

import com.dbn.language.common.element.ElementTypeBundle;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.dev.language.LanguageSpecificationXmlUtil.outputString;

public class LanguageSpecificationParserBuilder implements LanguageSpecificationArtifactBuilder {
    private final LanguageSpecificationBuilderInput input;

    public LanguageSpecificationParserBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @SneakyThrows
    @Override
    public void build() {
        new LanguageSpecificationParserBundleLoader(input).load(this::writeElementTypeDefinition);
    }

    @SneakyThrows
    private File getParserElementsFile() {
        File file = input.getParserElementsFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Parser elements definition does not exist: " + file.getAbsolutePath());
        }
        return file;
    }

    @SneakyThrows
    private void writeElementTypeDefinition(ElementTypeBundle.Builder builder) {
        if (!builder.isDirty()) {
            System.out.println("Parser elements definition is up to date");
            return;
        }

        File file = getParserElementsFile();
        Path filePath = file.toPath();

        System.out.println("Writing " + filePath);
        Files.writeString(filePath, outputString(builder.getDefinitionDocument()), StandardCharsets.UTF_8);
    }
}
