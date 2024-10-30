package com.dbn.common.color;

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
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Supplier;

import static com.dbn.common.color.ColorCache.cached;
import static com.dbn.common.color.ColorSchemes.background;
import static com.dbn.common.color.ColorSchemes.foreground;
import static com.dbn.common.dispose.Failsafe.guarded;

@UtilityClass
public final class Colors {
    public static Color LIGHT_BLUE = new JBColor(new Color(235, 244, 254), new Color(0x2D3548));
    public static Color HINT_COLOR = new JBColor(new Color(-12029286), new Color(-10058060));

    public static Color FAILURE_COLOR = new JBColor(new Color(0xFF0000), new Color(0xBC3F3C));
    public static Color SUCCESS_COLOR = new JBColor(new Color(0x009600), new Color(0x629755));

    public static Color getPanelBackground() {
        return cached(0, () -> UIUtil.getPanelBackground());
    }

    public static Color getLabelForeground() {
        return cached(1, () -> UIUtil.getLabelForeground());
    }

    public static Color getTextFieldBackground() {
        return cached(2, () -> UIUtil.getTextFieldBackground());
    }

    public static Color getTextFieldDisabledBackground() {
        return cached(3, () -> UIManager.getColor("TextField.disabledBackground"));
    }

    public static Color getTextFieldForeground() {
        return cached(4, () -> UIUtil.getTextFieldForeground());
    }

    public static Color getTableBackground() {
        return cached(5, () -> UIUtil.getTableBackground());
    }

    public static Color getTableForeground() {
        return cached(6, () -> UIUtil.getTableForeground());
    }

    public static Color getListBackground() {
        return cached(7, () -> UIUtil.getListBackground());
    }

    public static Color getListForeground() {
        return cached(8, () -> UIUtil.getListForeground());
    }

    public static Color getListSelectionBackground(boolean focused) {
        return focused ?
                cached(9, () -> UIUtil.getListSelectionBackground(true)) :
                cached(10, () -> UIUtil.getListSelectionBackground(false));

    }

    public static Color getListSelectionForeground(boolean focused) {
        return focused ?
                cached(11, () -> UIUtil.getListSelectionForeground(true)) :
                cached(12, () -> UIUtil.getListSelectionForeground(false));
    }

    public static Color getTableCaretRowColor() {
        return cached(13, () -> background(
                DataGridTextAttributesKeys.CARET_ROW,
                EditorColors.CARET_ROW_COLOR,
                () -> UIUtil.getTableBackground()));
    }

    public static Color getTableSelectionBackground(boolean focused) {
        return focused ?
                cached(14, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(true))) :
                cached(15, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(false)));
    }

    public static Color getTableSelectionForeground(boolean focused) {
        return focused ?
                cached(16, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(true))) :
                cached(17, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(false)));
    }

    public static Color getTableGridColor() {
        return cached(18, () -> new JBColor(() -> lafDarker(Colors.getTableBackground(), 3)));
    }

    public static Color getTableHeaderGridColor() {
        return cached(19, () -> new JBColor(() -> lafDarker(Colors.getPanelBackground(), 3)));
    }

    public static Color getTableGutterBackground() {
        return cached(20, () -> background(null, EditorColors.GUTTER_BACKGROUND, () -> getPanelBackground()));
    }

    public static Color getTableGutterForeground() {
        return cached(21, () -> background(null, EditorColors.LINE_NUMBERS_COLOR, () -> JBColor.GRAY));
    }

    public static Color getEditorBackground() {
        return cached(22, () -> background(HighlighterColors.NO_HIGHLIGHTING, null, () -> JBColor.WHITE));
    }

    public static Color getEditorForeground() {
        return cached(23, () -> foreground(HighlighterColors.NO_HIGHLIGHTING, null, () -> JBColor.BLACK));
    }

    public static Color getEditorCaretRowBackground() {
        return cached(24, () -> foreground(null, EditorColors.CARET_ROW_COLOR, () -> getEditorBackground()));
    }

    public static Color getReadonlyEditorBackground() {
        return cached(25, () -> background(null, EditorColors.READONLY_BACKGROUND_COLOR, () -> Colors.lafDarker(getEditorBackground(), 1)));
    }

    public static Color getReadonlyEditorCaretRowBackground() {
        return cached(26, () -> new JBColor(() -> Colors.lafDarker(getReadonlyEditorBackground(), 1)));
    }

    public static Color getLighterPanelBackground() {
        return cached(27, () -> new JBColor(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getLightPanelBackground() {
        return cached(28, () -> new JBColor(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 2)));
    }

    public static Color getDarkerPanelBackground() {
        return cached(29, () -> new JBColor(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getDarkPanelBackground() {
        return cached(30, () -> new JBColor(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 2)));
    }

    public static Color getInfoHintColor() {
        return cached(31, () -> HintUtil.getInformationColor());
    }

    public static Color getLabelInfoForeground() {
        return cached(32, () -> JBColor.namedColor("Label.infoForeground", new JBColor(Gray._120, Gray._135)));
    }

    public static Color getLabelErrorForeground() {
        return cached(33, () -> JBColor.namedColor("Label.errorForeground", new JBColor(new Color(0xC7222D), JBColor.RED)));
    }


    public static Color getWarningHintColor() {
        return cached(34, () -> HintUtil.getWarningColor());
    }

    public static Color getErrorHintColor() {
        return cached(35, () -> HintUtil.getErrorColor());
    }

    public static Color getOutlineColor() {
        return cached(36, () -> DarculaUIUtil.getOutlineColor(true, false));
    }

    public static Color getTextFieldInactiveForeground() {
        return cached(37, () -> UIManager.getColor("TextField.inactiveForeground"));
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
}
