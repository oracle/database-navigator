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

package com.dbn.editor.code.content;

import com.dbn.common.latent.Latent;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.intellij.diff.comparison.ByWord;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
public class SourceCodeContent{
    private static final String EMPTY_CONTENT = "";
    private final Latent<SourceCodeOffsets> offsets = Latent.basic(() -> new SourceCodeOffsets());
    private CharSequence text = EMPTY_CONTENT;
    private boolean writable = true;

    public SourceCodeContent() {
    }

    public SourceCodeContent(CharSequence text) {
        this(text, true);
    }

    public SourceCodeContent(CharSequence text, boolean writable) {
        setText(text);
        this.writable = writable;
    }

    public boolean isLoaded() {
        return text != EMPTY_CONTENT;
    }

    public void reset() {
        text = EMPTY_CONTENT;
    }

    public void setText(CharSequence text) {
        // do not capture the mutable char sequence in com.intellij.openapi.editor.Document
        this.text = text == null ? "" : text.toString();
    }

    public byte[] getBytes(Charset charset) {
        return text.toString().getBytes(charset);
    }

    public boolean matches(SourceCodeContent content, boolean soft) {
        if (soft) {
            try {
                ProgressIndicator progress = ProgressMonitor.getProgressIndicator();
                return ByWord.compare(text, content.text, ComparisonPolicy.IGNORE_WHITESPACES, progress).isEmpty();
            } catch (Exception e) {
                conditionallyLog(e);
            }
        }
        return Strings.equals(text, content.text);
    }

    public long length() {
        return text.length();
    }

    @Override
    public String toString() {
        return text.toString();
    }

    public SourceCodeOffsets getOffsets() {
        return offsets.get();
    }

    public void importContent(String content) {
        SourceCodeOffsets offsets = getOffsets();
        offsets.getGuardedBlocks().reset();

        StringBuilder builder = new StringBuilder(Commons.nvl(content, ""));
        int startIndex = builder.indexOf(GuardedBlockMarker.START_OFFSET_IDENTIFIER);


        while (startIndex > -1) {
            builder.replace(startIndex, startIndex + GuardedBlockMarker.START_OFFSET_IDENTIFIER.length(), "");
            int endIndex = builder.indexOf(GuardedBlockMarker.END_OFFSET_IDENTIFIER);
            if (endIndex == -1) {
                throw new IllegalArgumentException("Unbalanced guarded block markers");
            }
            builder.replace(endIndex, endIndex + GuardedBlockMarker.END_OFFSET_IDENTIFIER.length(), "");
            offsets.addGuardedBlock(startIndex, endIndex);

            startIndex = builder.indexOf(GuardedBlockMarker.START_OFFSET_IDENTIFIER);
        }

        setText(builder.toString());
    }

    public String exportContent() {
        StringBuilder builder = new StringBuilder(text);
        List<GuardedBlockMarker> ranges = new ArrayList<>(getOffsets().getGuardedBlocks().getRanges());
        ranges.sort(Collections.reverseOrder());
        for (GuardedBlockMarker range : ranges) {
            builder.insert(range.getEndOffset(), GuardedBlockMarker.END_OFFSET_IDENTIFIER);
            builder.insert(range.getStartOffset(), GuardedBlockMarker.START_OFFSET_IDENTIFIER);
        }
        return builder.toString();
    }
}
