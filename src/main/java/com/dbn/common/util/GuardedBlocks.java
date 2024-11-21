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

package com.dbn.common.util;

import com.dbn.common.action.UserDataKeys;
import com.dbn.editor.code.content.GuardedBlockMarkers;
import com.dbn.editor.code.content.GuardedBlockType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.util.Range;

import java.util.ArrayList;
import java.util.List;

public class GuardedBlocks {
    private GuardedBlocks(){}

    public static void createGuardedBlock(Document document, GuardedBlockType type, String reason, boolean highlight) {
        createGuardedBlock(document, type, 0, document.getTextLength(), reason);
        if (highlight) return;

        Editor[] editors = Documents.getEditors(document);
        for (Editor editor : editors) {
            EditorColorsScheme scheme = editor.getColorsScheme();
            scheme.setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, scheme.getDefaultBackground());
        }
    }

    public static void createGuardedBlocks(Document document, GuardedBlockType type, GuardedBlockMarkers ranges, String reason) {
        for (Range<Integer> range : ranges.getRanges()) {
            createGuardedBlock(document, type, range.getFrom(), range.getTo(), reason);
        }
    }

    public static void createGuardedBlock(Document document, GuardedBlockType type, int startOffset, int endOffset, String reason) {
        if (startOffset != endOffset) {
            int textLength = document.getTextLength();
            if (endOffset <= textLength) {
                RangeMarker rangeMarker = document.createGuardedBlock(startOffset, endOffset);
                rangeMarker.setGreedyToLeft(startOffset == 0);
                rangeMarker.setGreedyToRight(endOffset == textLength);
                rangeMarker.putUserData(GuardedBlockType.KEY, type);
                document.putUserData(UserDataKeys.GUARDED_BLOCK_REASON, reason);
            }
        }
    }

    public static void removeGuardedBlocks(Document document, GuardedBlockType type) {
        if (document instanceof DocumentEx) {
            DocumentEx documentEx = (DocumentEx) document;
            List<RangeMarker> guardedBlocks = new ArrayList<>(documentEx.getGuardedBlocks());
            for (RangeMarker block : guardedBlocks) {
                if (block.getUserData(GuardedBlockType.KEY) == type) {
                    document.removeGuardedBlock(block);
                }
            }
            document.putUserData(UserDataKeys.GUARDED_BLOCK_REASON, null);
        }
    }
}
