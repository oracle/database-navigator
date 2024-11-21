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

package com.dbn.editor.code.diff;

import java.util.ArrayList;
import java.util.List;

public class MergeContent {
    private final List<SourceCodeDiffContent> contents = new ArrayList<>();

    public MergeContent(SourceCodeDiffContent leftContent, SourceCodeDiffContent targetContent, SourceCodeDiffContent rightContent) {
        contents.add(leftContent);
        contents.add(targetContent);
        contents.add(rightContent);
    }

    public List<String> getTitles() {
        List<String> titles = new ArrayList<>();
        for (SourceCodeDiffContent content : contents) {
            titles.add(content.getTitle());
        }

        return titles;
    }

    public List<byte[]> getByteContents() {
        List<byte[]> byteContents = new ArrayList<>();
        for (SourceCodeDiffContent content : contents) {
            byteContents.add(content.getByteContent());
        }

        return byteContents;
    }

}
