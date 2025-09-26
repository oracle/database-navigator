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

package com.dbn.data.grid.color;

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.ui.JBColor;

import java.awt.Color;

public interface DataGridTextAttributesKeys {
    TextAttributesKey DEFAULT_PLAIN_DATA     = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.PlainData");
    TextAttributesKey DEFAULT_CARET_ROW      = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.CaretRow");
    TextAttributesKey DEFAULT_AUDIT_DATA     = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.AuditData");
    TextAttributesKey DEFAULT_MODIFIED_DATA  = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.ModifiedData");
    TextAttributesKey DEFAULT_DELETED_DATA   = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.DeletedData");
    TextAttributesKey DEFAULT_ERROR_DATA     = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.ErrorData");
    TextAttributesKey DEFAULT_READONLY_DATA  = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.ReadonlyData");
    TextAttributesKey DEFAULT_LOADING_DATA   = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.LoadingData");
    TextAttributesKey DEFAULT_UPDATING_DATA  = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.UpdatingData");
    TextAttributesKey DEFAULT_PRIMARY_KEY    = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.PrimaryKey");
    TextAttributesKey DEFAULT_FOREIGN_KEY    = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.ForeignKey");
    TextAttributesKey DEFAULT_SELECTION      = TextAttributesKey.createTextAttributesKey("DBN.DefaultAttributes.DataEditor.Selection");

    interface Colors {
        Color DEFAULT_BACKGROUND   = HighlighterColors.TEXT.getDefaultAttributes().getBackgroundColor();
        Color DEFAULT_FOREGROUND   = HighlighterColors.TEXT.getDefaultAttributes().getForegroundColor();
        Color LIGHT_BACKGROUND     = new JBColor(new Color(0xf4f4f4), new Color(0x393939));
        Color LIGHT_FOREGROUND     = new JBColor(new Color(0x7f7f7f), new Color(0x999999));
        Color ERROR_BACKGROUND     = new JBColor(new Color(0x7f7f7f), new Color(0x999999));
        Color PK_FOREGROUND        = new JBColor(new Color(0x8B4233), new Color(0xC4D1DC));
        Color PK_BACKGROUND        = new JBColor(new Color(0xF7F7FF), new Color(0x2B3447));
        Color FK_FOREGROUND        = new JBColor(new Color(0x3F6B3F), new Color(0xC9D5C9));
        Color FK_BACKGROUND        = new JBColor(new Color(0xF7FFF7), new Color(0x2A3B2A));
        Color CARET_ROW_BACKGROUND = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR);
    }
    TextAttributesKey PLAIN_DATA     = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.PlainData",    DEFAULT_PLAIN_DATA);
    TextAttributesKey CARET_ROW      = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.CaretRow",     DEFAULT_CARET_ROW);
    TextAttributesKey AUDIT_DATA     = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.AuditData",    DEFAULT_AUDIT_DATA);
    TextAttributesKey MODIFIED_DATA  = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.ModifiedData", DEFAULT_MODIFIED_DATA);
    TextAttributesKey DELETED_DATA   = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.DeletedData",  DEFAULT_DELETED_DATA);
    TextAttributesKey ERROR_DATA     = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.ErrorData",    DEFAULT_ERROR_DATA);
    TextAttributesKey READONLY_DATA  = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.ReadonlyData", DEFAULT_READONLY_DATA);
    TextAttributesKey LOADING_DATA   = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.LoadingData",  DEFAULT_LOADING_DATA);
    TextAttributesKey UPDATING_DATA  = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.UpdatingData", DEFAULT_UPDATING_DATA);
    TextAttributesKey PRIMARY_KEY    = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.PrimaryKey",   DEFAULT_PRIMARY_KEY);
    TextAttributesKey FOREIGN_KEY    = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.ForeignKey",   DEFAULT_FOREIGN_KEY);
    TextAttributesKey SELECTION      = TextAttributesKey.createTextAttributesKey("DBN.Attributes.DataEditor.Selection",    DEFAULT_SELECTION);
}
