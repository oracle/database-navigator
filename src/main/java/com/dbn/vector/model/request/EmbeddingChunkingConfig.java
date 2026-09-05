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

package com.dbn.vector.model.request;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@NonNls
public class EmbeddingChunkingConfig extends EmbeddingRequestConfig implements Cloneable<EmbeddingChunkingConfig> {
    private static final String DEFAULT_CHUNK_BY = "WORDS";
    private static final String DEFAULT_SPLIT_BY = "NEWLINE";
    public static final Set<String> CHUNK_BY_OPTIONS = Set.of("CHARACTERS", "WORDS", "NONE");
    public static final Set<String> SPLIT_BY_OPTIONS = Set.of("NEWLINE", "SENTENCE", "RECURSIVELY", "BLANKLINE", "SPACE", "NONE");

    private String chunkBy = DEFAULT_CHUNK_BY;
    private String splitBy = DEFAULT_SPLIT_BY;
    private int maxSize = 100;
    private int overlap = 10;

    public EmbeddingChunkingConfig() {
    }

    public EmbeddingChunkingConfig(String chunkBy, int max, String splitBy, int overlap) {
        this.chunkBy = chunkBy;
        this.maxSize = max;
        this.splitBy = splitBy;
        this.overlap = overlap;
    }

    public String getConfigJson() {
        return "NONE".equals(chunkBy) ? null : Json.writeAsString(getConfigMap());
    }

    @NotNull
    public Map<String, ?> getConfigMap() {
        return Map.of(
                "chunkBy", chunkBy,
                "splitBy", splitBy,
                "max", maxSize,
                "overlap", overlap);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        chunkBy = validChunkBy(stringAttribute(element, "chunk-by", chunkBy));
        splitBy = validSplitBy(stringAttribute(element, "split-by", splitBy));
        maxSize = integerAttribute(element, "max-size", maxSize);
        overlap = integerAttribute(element, "overlap", overlap);
    }

    private static String validChunkBy(String value) {
        return value != null && CHUNK_BY_OPTIONS.contains(value) ? value : DEFAULT_CHUNK_BY;
    }

    private static String validSplitBy(String value) {
        return value != null && SPLIT_BY_OPTIONS.contains(value) ? value : DEFAULT_SPLIT_BY;
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setStringAttribute(element, "chunk-by", chunkBy);
        setStringAttribute(element, "split-by", splitBy);
        setIntegerAttribute(element, "max-size", maxSize);
        setIntegerAttribute(element, "overlap", overlap);
    }

    @Override
    @SneakyThrows
    public EmbeddingChunkingConfig clone() {
        return cast(super.clone());
    }
}
