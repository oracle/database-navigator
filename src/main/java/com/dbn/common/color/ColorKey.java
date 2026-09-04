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

package com.dbn.common.color;

/** Stable identities used by the shared color cache. */
public enum ColorKey {
    PANEL_BACKGROUND,
    LABEL_FOREGROUND,
    TEXT_FIELD_BACKGROUND,
    TEXT_FIELD_DISABLED_BACKGROUND,
    TEXT_FIELD_FOREGROUND,
    TABLE_BACKGROUND,
    TABLE_FOREGROUND,
    LIST_BACKGROUND,
    LIST_FOREGROUND,
    LIST_SELECTION_BACKGROUND_FOCUSED,
    LIST_SELECTION_BACKGROUND_UNFOCUSED,
    LIST_SELECTION_FOREGROUND_FOCUSED,
    LIST_SELECTION_FOREGROUND_UNFOCUSED,
    TABLE_CARET_ROW,
    TABLE_SELECTION_BACKGROUND_FOCUSED,
    TABLE_SELECTION_BACKGROUND_UNFOCUSED,
    TABLE_SELECTION_FOREGROUND_FOCUSED,
    TABLE_SELECTION_FOREGROUND_UNFOCUSED,
    TABLE_GRID,
    TABLE_HEADER_GRID,
    TABLE_GUTTER_BACKGROUND,
    TABLE_GUTTER_FOREGROUND,
    EDITOR_BACKGROUND,
    EDITOR_FOREGROUND,
    EDITOR_CARET_ROW_BACKGROUND,
    READONLY_EDITOR_BACKGROUND,
    READONLY_EDITOR_CARET_ROW_BACKGROUND,
    LIGHTER_PANEL_BACKGROUND,
    LIGHT_PANEL_BACKGROUND,
    DARKER_PANEL_BACKGROUND,
    DARK_PANEL_BACKGROUND,
    INFO_HINT,
    LABEL_INFO_FOREGROUND,
    LABEL_ERROR_FOREGROUND,
    LABEL_SUCCESS_FOREGROUND,
    LABEL_WARNING_FOREGROUND,
    WARNING_HINT,
    ERROR_HINT,
    OUTLINE,
    TEXT_FIELD_INACTIVE_FOREGROUND
}
