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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.ui.util.LookAndFeel;
import com.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;

import static com.dbn.common.color.ColorCache.cached;
import static com.dbn.common.color.ColorKey.DARKER_PANEL_BACKGROUND;
import static com.dbn.common.color.ColorKey.DARK_PANEL_BACKGROUND;
import static com.dbn.common.color.ColorKey.EDITOR_BACKGROUND;
import static com.dbn.common.color.ColorKey.EDITOR_CARET_ROW_BACKGROUND;
import static com.dbn.common.color.ColorKey.EDITOR_FOREGROUND;
import static com.dbn.common.color.ColorKey.ERROR_HINT;
import static com.dbn.common.color.ColorKey.INFO_HINT;
import static com.dbn.common.color.ColorKey.LABEL_ERROR_FOREGROUND;
import static com.dbn.common.color.ColorKey.LABEL_FOREGROUND;
import static com.dbn.common.color.ColorKey.LABEL_INFO_FOREGROUND;
import static com.dbn.common.color.ColorKey.LABEL_SUCCESS_FOREGROUND;
import static com.dbn.common.color.ColorKey.LABEL_WARNING_FOREGROUND;
import static com.dbn.common.color.ColorKey.LIGHTER_PANEL_BACKGROUND;
import static com.dbn.common.color.ColorKey.LIGHT_PANEL_BACKGROUND;
import static com.dbn.common.color.ColorKey.LIST_BACKGROUND;
import static com.dbn.common.color.ColorKey.LIST_FOREGROUND;
import static com.dbn.common.color.ColorKey.LIST_SELECTION_BACKGROUND_FOCUSED;
import static com.dbn.common.color.ColorKey.LIST_SELECTION_BACKGROUND_UNFOCUSED;
import static com.dbn.common.color.ColorKey.LIST_SELECTION_FOREGROUND_FOCUSED;
import static com.dbn.common.color.ColorKey.LIST_SELECTION_FOREGROUND_UNFOCUSED;
import static com.dbn.common.color.ColorKey.OUTLINE;
import static com.dbn.common.color.ColorKey.PANEL_BACKGROUND;
import static com.dbn.common.color.ColorKey.READONLY_EDITOR_BACKGROUND;
import static com.dbn.common.color.ColorKey.READONLY_EDITOR_CARET_ROW_BACKGROUND;
import static com.dbn.common.color.ColorKey.TABLE_BACKGROUND;
import static com.dbn.common.color.ColorKey.TABLE_CARET_ROW;
import static com.dbn.common.color.ColorKey.TABLE_FOREGROUND;
import static com.dbn.common.color.ColorKey.TABLE_GRID;
import static com.dbn.common.color.ColorKey.TABLE_GUTTER_BACKGROUND;
import static com.dbn.common.color.ColorKey.TABLE_GUTTER_FOREGROUND;
import static com.dbn.common.color.ColorKey.TABLE_HEADER_GRID;
import static com.dbn.common.color.ColorKey.TABLE_SELECTION_BACKGROUND_FOCUSED;
import static com.dbn.common.color.ColorKey.TABLE_SELECTION_BACKGROUND_UNFOCUSED;
import static com.dbn.common.color.ColorKey.TABLE_SELECTION_FOREGROUND_FOCUSED;
import static com.dbn.common.color.ColorKey.TABLE_SELECTION_FOREGROUND_UNFOCUSED;
import static com.dbn.common.color.ColorKey.TEXT_FIELD_BACKGROUND;
import static com.dbn.common.color.ColorKey.TEXT_FIELD_DISABLED_BACKGROUND;
import static com.dbn.common.color.ColorKey.TEXT_FIELD_FOREGROUND;
import static com.dbn.common.color.ColorKey.TEXT_FIELD_INACTIVE_FOREGROUND;
import static com.dbn.common.color.ColorKey.WARNING_HINT;
import static com.dbn.common.color.ColorSchemes.background;
import static com.dbn.common.color.ColorSchemes.foreground;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.intellij.ui.ColorUtil.withAlpha;

@UtilityClass
public final class Colors {
    public static final Color LIGHT_BLUE = new JBColor(new Color(235, 244, 254), new Color(0x2D3548));
    public static final Color HINT_COLOR = new JBColor(new Color(-12029286), new Color(-10058060));

    public static final Color FAILURE_COLOR = new JBColor(new Color(0xFF0000), new Color(0xBC3F3C));
    public static final Color SUCCESS_COLOR = new JBColor(new Color(0x009600), new Color(0x629755));

    public static final Color SEPARATOR_COLOR = JBColor.namedColor("Separator.separatorColor", new JBColor(0xcdcdcd, 0x515151));

    public static Color getPanelBackground() {
        return cached(PANEL_BACKGROUND, () -> UIUtil.getPanelBackground());
    }

    public static Color getLabelForeground() {
        return cached(LABEL_FOREGROUND, () -> UIUtil.getLabelForeground());
    }

    public static Color getTextFieldBackground() {
        return cached(TEXT_FIELD_BACKGROUND, () -> UIUtil.getTextFieldBackground());
    }

    public static Color getTextFieldDisabledBackground() {
        return cached(TEXT_FIELD_DISABLED_BACKGROUND, () -> UIManager.getColor("TextField.disabledBackground"));
    }

    public static Color getTextFieldForeground() {
        return cached(TEXT_FIELD_FOREGROUND, () -> UIUtil.getTextFieldForeground());
    }

    public static Color getTableBackground() {
        return cached(TABLE_BACKGROUND, () -> UIUtil.getTableBackground());
    }

    public static Color getTableForeground() {
        return cached(TABLE_FOREGROUND, () -> UIUtil.getTableForeground());
    }

    public static Color getListBackground() {
        return cached(LIST_BACKGROUND, () -> UIUtil.getListBackground());
    }

    public static Color getListForeground() {
        return cached(LIST_FOREGROUND, () -> UIUtil.getListForeground());
    }

    public static Color getListSelectionBackground(boolean focused) {
        return focused ?
                cached(LIST_SELECTION_BACKGROUND_FOCUSED, () -> UIUtil.getListSelectionBackground(true)) :
                cached(LIST_SELECTION_BACKGROUND_UNFOCUSED, () -> UIUtil.getListSelectionBackground(false));

    }

    public static Color getListSelectionForeground(boolean focused) {
        return focused ?
                cached(LIST_SELECTION_FOREGROUND_FOCUSED, () -> UIUtil.getListSelectionForeground(true)) :
                cached(LIST_SELECTION_FOREGROUND_UNFOCUSED, () -> UIUtil.getListSelectionForeground(false));
    }

    public static Color getTableCaretRowColor() {
        return cached(TABLE_CARET_ROW, () -> background(
                DataGridTextAttributesKeys.CARET_ROW,
                EditorColors.CARET_ROW_COLOR,
                () -> UIUtil.getTableBackground()));
    }

    public static Color getTableSelectionBackground(boolean focused) {
        return focused ?
                cached(TABLE_SELECTION_BACKGROUND_FOCUSED, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(true))) :
                cached(TABLE_SELECTION_BACKGROUND_UNFOCUSED, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(false)));
    }

    public static Color getTableSelectionForeground(boolean focused) {
        return focused ?
                cached(TABLE_SELECTION_FOREGROUND_FOCUSED, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(true))) :
                cached(TABLE_SELECTION_FOREGROUND_UNFOCUSED, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(false)));
    }

    public static Color getTableGridColor() {
        return cached(TABLE_GRID, () -> JBColor.lazy(() -> lafDarker(Colors.getTableBackground(), 3)));
    }

    public static Color getTableHeaderGridColor() {
        return cached(TABLE_HEADER_GRID, () -> JBColor.lazy(() -> lafDarker(Colors.getPanelBackground(), 3)));
    }

    public static Color getTableGutterBackground() {
        return cached(TABLE_GUTTER_BACKGROUND, () -> background(null, EditorColors.GUTTER_BACKGROUND, () -> getPanelBackground()));
    }

    public static Color getTableGutterForeground() {
        return cached(TABLE_GUTTER_FOREGROUND, () -> background(null, EditorColors.LINE_NUMBERS_COLOR, () -> JBColor.GRAY));
    }

    public static Color getEditorBackground() {
        return cached(EDITOR_BACKGROUND, () -> background(HighlighterColors.NO_HIGHLIGHTING, null, () -> JBColor.WHITE));
    }

    public static Color getEditorForeground() {
        return cached(EDITOR_FOREGROUND, () -> foreground(HighlighterColors.NO_HIGHLIGHTING, null, () -> JBColor.BLACK));
    }

    public static Color getEditorCaretRowBackground() {
        return cached(EDITOR_CARET_ROW_BACKGROUND, () -> foreground(null, EditorColors.CARET_ROW_COLOR, () -> getEditorBackground()));
    }

    public static Color getReadonlyEditorBackground() {
        return cached(READONLY_EDITOR_BACKGROUND, () -> background(null, EditorColors.READONLY_BACKGROUND_COLOR, () -> Colors.lafDarker(getEditorBackground(), 1)));
    }

    public static Color getReadonlyEditorCaretRowBackground() {
        return cached(READONLY_EDITOR_CARET_ROW_BACKGROUND, () -> JBColor.lazy(() -> Colors.lafDarker(getReadonlyEditorBackground(), 1)));
    }

    public static Color getLighterPanelBackground() {
        return cached(LIGHTER_PANEL_BACKGROUND, () -> JBColor.lazy(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getLightPanelBackground() {
        return cached(LIGHT_PANEL_BACKGROUND, () -> JBColor.lazy(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 2)));
    }

    public static Color getDarkerPanelBackground() {
        return cached(DARKER_PANEL_BACKGROUND, () -> JBColor.lazy(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getDarkPanelBackground() {
        return cached(DARK_PANEL_BACKGROUND, () -> JBColor.lazy(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 2)));
    }

    public static Color getInfoHintColor() {
        return cached(INFO_HINT, () -> HintUtil.getInformationColor());
    }

    public static Color getLabelInfoForeground() {
        return cached(LABEL_INFO_FOREGROUND, () -> JBColor.namedColor("Label.infoForeground", new JBColor(Gray._120, Gray._135)));
    }

    public static Color getLabelErrorForeground() {
        return cached(LABEL_ERROR_FOREGROUND, () -> JBColor.namedColor("Label.errorForeground", new JBColor(new Color(0xC7222D), JBColor.RED)));
    }

    public static Color getLabelSuccessForeground() {
        return cached(LABEL_SUCCESS_FOREGROUND, () -> JBColor.namedColor("Label.successForeground", SUCCESS_COLOR));
    }

    public static Color getLabelWarningForeground() {
        return cached(LABEL_WARNING_FOREGROUND, () -> JBColor.namedColor("Label.warningForeground", JBColor.ORANGE));
    }

    public static Color getWarningHintColor() {
        return cached(WARNING_HINT, () -> HintUtil.getWarningColor());
    }

    public static Color getErrorHintColor() {
        return cached(ERROR_HINT, () -> HintUtil.getErrorColor());
    }

    public static Color getOutlineColor() {
        return cached(OUTLINE, () -> DarculaUIUtil.getOutlineColor(true, false));
    }

    public static Color getTextFieldInactiveForeground() {
        return cached(TEXT_FIELD_INACTIVE_FOREGROUND, () -> UIManager.getColor("TextField.inactiveForeground"));
    }


    @NotNull
    public static EditorColorsScheme getGlobalScheme() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    @Deprecated // remove after all colors confirm to be JBColor
    public static void subscribe(@Nullable Disposable parentDisposable,  Runnable runnable) {
        ApplicationEvents.subscribe(parentDisposable, EditorColorsManager.TOPIC, scheme -> runnable.run());

        UIManager.addPropertyChangeListener(evt -> {
            if (Objects.equals(evt.getPropertyName(), "lookAndFeel")) {
                guarded(runnable, r -> r.run());
            }
        });
    }

    public static Color lafBrighter(Color color, int tones) {
        return LookAndFeel.isDarkMode() ?
                darker(color, tones * 2) :
                brighter(color, tones);
    }

    public static Color lafDarker(Color color, int tones) {
        return LookAndFeel.isDarkMode() ?
                brighter(color, tones * 2) :
                darker(color, tones);
    }


    public static Color brighter(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.BRIGHTER, tones);
    }

    public static Color darker(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.DARKER, tones);
    }

    public static Color softer(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.SOFTER, tones);
    }

    public static Color stronger(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.STRONGER, tones);
    }

    @Compatibility
    public static Color faded(Color color) {
        return withAlpha(color, (double)0.45F);
    }

    public static Color dimmer(Color color) {
        return ColorUtil.dimmer(color);
    }


    public static Color delegate(Supplier<Color> supplier) {
        return new ColorDelegate(supplier);
    }


    private static class ColorDelegate extends Color {
        private final Supplier<Color> delegate;
        public ColorDelegate(Supplier<Color> delegate) {
            super(0);
            this.delegate = delegate;
        }

        @Delegate
        Color getDelegate() {
            return delegate.get();
        }
    }

    /**
     * Displays a color chooser dialog and allows the user to select a color.
     * If the user selects a color, it is returned; otherwise, the initial color is returned.
     *
     * @param project       the current project context, used for dialog consistency
     * @param parent        the parent component for the dialog
     * @param initialColor  the initial color to display in the dialog
     * @param caption       the text displayed as the dialog's title
     * @return the chosen color if the dialog selection is confirmed, or the initial color if canceled
     */
    @Compatibility
    public static Color chooseColor(Project project, JComponent parent, Color initialColor, @DialogTitle String caption) {
        ColorChooserService colorChooserService = ColorChooserService.getInstance();
        return colorChooserService.showDialog(project, parent, caption, initialColor, false, Collections.emptyList(), false);
    }

    /**
     * Copy of JBUI.CurrentTheme.Banner
     */
    @Compatibility
    @UtilityClass
    public static final class Banner {
        public static final Color INFO_BACKGROUND_COLOR = JBColor.namedColor("Banner.infoBackground", 0xF5F8FE, 0x25324D);
        public static final Color INFO_BORDER_COLOR = JBColor.namedColor("Banner.infoBorderColor", 0xC2D6FC, 0x35538F);

        public static final Color SUCCESS_BACKGROUND_COLOR = JBColor.namedColor("Banner.successBackground", 0xF2FCF3, 0x253627);
        public static final Color SUCCESS_BORDER_COLOR = JBColor.namedColor("Banner.successBorderColor", 0xC5E5CC, 0x375239);

        public static final Color WARNING_BACKGROUND_COLOR = JBColor.namedColor("Banner.warningBackground", 0xFFFAEB, 0x3d3223);
        public static final Color WARNING_BORDER_COLOR = JBColor.namedColor("Banner.warningBorderColor", 0xFED277, 0x5E4D33);

        public static final Color ERROR_BACKGROUND_COLOR = JBColor.namedColor("Banner.errorBackground", 0xFFF7F7, 0x402929);
        public static final Color ERROR_BORDER_COLOR = JBColor.namedColor("Banner.errorBorderColor", 0xFAD4D8, 0x5E3838);
        public static final Color FOREGROUND = JBColor.namedColor("Banner.foreground", 0x0, 0xDFE1E5);
    }
}
