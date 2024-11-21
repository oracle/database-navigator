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

import com.dbn.common.color.Colors;
import com.dbn.common.latent.Latent;
import com.dbn.common.util.TextAttributes;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;

import java.awt.Color;

import static com.dbn.common.util.Commons.nvln;

@Getter
public class BasicTableTextAttributes implements DataGridTextAttributes {
    private final SimpleTextAttributes plainData;
    private final SimpleTextAttributes plainDataModified;
    private final SimpleTextAttributes plainDataAtCaretRow;
    private final SimpleTextAttributes plainDataAtCaretRowModified;
    private final SimpleTextAttributes auditData;
    private final SimpleTextAttributes auditDataModified;
    private final SimpleTextAttributes auditDataAtCaretRow;
    private final SimpleTextAttributes auditDataAtCaretRowModified;
    private final SimpleTextAttributes modifiedData;
    private final SimpleTextAttributes modifiedDataAtCaretRow;
    private final SimpleTextAttributes deletedData;
    private final SimpleTextAttributes errorData;
    private final SimpleTextAttributes readonlyData;
    private final SimpleTextAttributes readonlyDataModified;
    private final SimpleTextAttributes readonlyDataAtCaretRow;
    private final SimpleTextAttributes readonlyDataAtCaretRowModified;
    private final SimpleTextAttributes loadingData;
    private final SimpleTextAttributes loadingDataAtCaretRow;
    private final SimpleTextAttributes primaryKey;
    private final SimpleTextAttributes primaryKeyModified;
    private final SimpleTextAttributes primaryKeyAtCaretRow;
    private final SimpleTextAttributes primaryKeyAtCaretRowModified;
    private final SimpleTextAttributes foreignKey;
    private final SimpleTextAttributes foreignKeyModified;
    private final SimpleTextAttributes foreignKeyAtCaretRow;
    private final SimpleTextAttributes foreignKeyAtCaretRowModified;
    private final SimpleTextAttributes selection;
    private final SimpleTextAttributes searchResult;

    private final Color caretRowBgColor;

    private static final Latent<BasicTableTextAttributes> INSTANCE = Latent.laf(() -> new BasicTableTextAttributes());

    private BasicTableTextAttributes() {
        EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
        caretRowBgColor = globalScheme.getAttributes(DataGridTextAttributesKeys.CARET_ROW).getBackgroundColor();

        deletedData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.DELETED_DATA);
        errorData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.ERROR_DATA);
        modifiedData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.MODIFIED_DATA);
        modifiedDataAtCaretRow = new SimpleTextAttributes(caretRowBgColor, modifiedData.getFgColor(), null, modifiedData.getFontStyle());

        plainData = createPlainData();

        plainDataModified = new SimpleTextAttributes(
                nvln(modifiedData.getBgColor(), plainData.getBgColor()),
                nvln(modifiedData.getFgColor(), plainData.getFgColor()), null,
                modifiedData.getFontStyle());
        plainDataAtCaretRow = new SimpleTextAttributes(caretRowBgColor, plainData.getFgColor(), null, plainData.getFontStyle());
        plainDataAtCaretRowModified = new SimpleTextAttributes(
                caretRowBgColor,
                nvln(modifiedData.getFgColor(), plainData.getFgColor()), null,
                modifiedData.getFontStyle());


        auditData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.AUDIT_DATA);
        auditDataModified = new SimpleTextAttributes(
                nvln(modifiedData.getBgColor(), auditData.getBgColor()),
                nvln(modifiedData.getFgColor(), plainData.getFgColor()), null,
                modifiedData.getFontStyle());
        auditDataAtCaretRow = new SimpleTextAttributes(caretRowBgColor, auditData.getFgColor(), null, auditData.getFontStyle());
        auditDataAtCaretRowModified = new SimpleTextAttributes(
                caretRowBgColor,
                nvln(modifiedData.getFgColor(), plainData.getFgColor()), null,
                modifiedData.getFontStyle());


        readonlyData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.READONLY_DATA);
        readonlyDataModified = new SimpleTextAttributes(
                nvln(modifiedData.getBgColor(), readonlyData.getBgColor()),
                nvln(modifiedData.getFgColor(), readonlyData.getFgColor()), null,
                modifiedData.getFontStyle());
        readonlyDataAtCaretRow = new SimpleTextAttributes(caretRowBgColor, readonlyData.getFgColor(), null, readonlyData.getFontStyle());
        readonlyDataAtCaretRowModified = new SimpleTextAttributes(
                caretRowBgColor,
                nvln(modifiedData.getFgColor(), readonlyData.getFgColor()), null,
                modifiedData.getFontStyle());

        loadingData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.LOADING_DATA);
        loadingDataAtCaretRow = new SimpleTextAttributes(caretRowBgColor, loadingData.getFgColor(), null, loadingData.getFontStyle());

        primaryKey= TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.PRIMARY_KEY);
        primaryKeyModified = new SimpleTextAttributes(
                nvln(modifiedData.getBgColor(), primaryKey.getBgColor()),
                nvln(modifiedData.getFgColor(), primaryKey.getFgColor()), null,
                modifiedData.getStyle());
        primaryKeyAtCaretRow = new SimpleTextAttributes(caretRowBgColor, primaryKey.getFgColor(), null, primaryKey.getStyle());
        primaryKeyAtCaretRowModified = new SimpleTextAttributes(
                caretRowBgColor,
                nvln(modifiedData.getFgColor(), primaryKey.getFgColor()), null,
                modifiedData.getStyle());

        foreignKey = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.FOREIGN_KEY);
        foreignKeyModified = new SimpleTextAttributes(
                nvln(modifiedData.getBgColor(), foreignKey.getBgColor()),
                nvln(modifiedData.getFgColor(), foreignKey.getFgColor()), null,
                modifiedData.getStyle());
        foreignKeyAtCaretRow = new SimpleTextAttributes(caretRowBgColor, foreignKey.getFgColor(), null, foreignKey.getStyle());
        foreignKeyAtCaretRowModified = new SimpleTextAttributes(
                caretRowBgColor,
                nvln(modifiedData.getFgColor(), foreignKey.getFgColor()), null,
                modifiedData.getStyle());

        selection = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.SELECTION);
        searchResult = TextAttributes.getSimpleTextAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    }

    private SimpleTextAttributes createPlainData() {
        SimpleTextAttributes plainData = TextAttributes.getSimpleTextAttributes(DataGridTextAttributesKeys.PLAIN_DATA);
        if (plainData.getFgColor() == null) plainData = plainData.derive(plainData.getStyle(), Colors.getTextFieldForeground(), plainData.getBgColor(), null);
        if (plainData.getBgColor() == null) plainData = plainData.derive(plainData.getStyle(), plainData.getFgColor(), Colors.getTextFieldBackground(), null);
        return plainData;
    }

    public static BasicTableTextAttributes get() {
        return INSTANCE.get();
    }

    @Override
    public SimpleTextAttributes getPlainData(boolean modified, boolean atCaretRow) {
        return modified && atCaretRow ? plainDataAtCaretRowModified :
                atCaretRow ? plainDataAtCaretRow :
                modified ? plainDataModified : plainData;
    }

    public SimpleTextAttributes getModifiedData(boolean atCaretRow) {
        return atCaretRow ? modifiedDataAtCaretRow : modifiedData;
    }

    public SimpleTextAttributes getReadonlyData(boolean modified, boolean atCaretRow) {
        return
            modified && atCaretRow ? readonlyDataAtCaretRowModified :
            atCaretRow ? readonlyDataAtCaretRow :
            modified ? readonlyDataModified : readonlyData;
    }

    @Override
    public SimpleTextAttributes getLoadingData(boolean atCaretRow) {
        return atCaretRow ? loadingDataAtCaretRow : loadingData;
    }

    public SimpleTextAttributes getPrimaryKey(boolean modified, boolean atCaretRow) {
        return
            modified && atCaretRow ? primaryKeyAtCaretRowModified :
            atCaretRow ? primaryKeyAtCaretRow :
            modified ? primaryKeyModified : primaryKey;
    }

    public SimpleTextAttributes getForeignKey(boolean modified, boolean atCaretRow) {
        return
            modified && atCaretRow ? foreignKeyAtCaretRowModified :
            atCaretRow ? foreignKeyAtCaretRow :
            modified ? foreignKeyModified : foreignKey;
    }

    public SimpleTextAttributes getAuditData(boolean modified, boolean atCaretRow) {
        return
            modified && atCaretRow ? auditDataAtCaretRowModified :
            atCaretRow ? auditDataAtCaretRow :
            modified ? auditDataModified : auditData;
    }

    @Override
    public SimpleTextAttributes getSelection() {
        return selection;
    }

    @Override
    public SimpleTextAttributes getSearchResult() {
        return searchResult;
    }

    @Override
    public Color getCaretRowBgColor() {
        return caretRowBgColor;
    }
}
