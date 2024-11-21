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

import com.intellij.openapi.editor.RangeMarker;

import java.util.ArrayList;
import java.util.List;

public class GuardedBlockMarkers {
    private final List<GuardedBlockMarker> ranges = new ArrayList<>();

    public void addMarker(int startOffset, int endOffset) {
        ranges.removeIf(range -> range.getStartOffset() >= startOffset && range.getEndOffset() <= endOffset);
        ranges.add(new GuardedBlockMarker(startOffset, endOffset));
    }

    public List<GuardedBlockMarker> getRanges() {
        return ranges;
    }

    public void apply(List<RangeMarker> rangeMarkers) {
        reset();
        for (RangeMarker rangeMarker : rangeMarkers) {
            addMarker(
                rangeMarker.getStartOffset(),
                rangeMarker.getEndOffset());
        }
    }

    public void reset() {
        ranges.clear();
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }
}
