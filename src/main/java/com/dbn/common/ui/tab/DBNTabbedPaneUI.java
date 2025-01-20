
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
package com.dbn.common.ui.tab;

import com.dbn.common.Wrapper;
import com.dbn.common.color.Colors;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.ui.util.LookAndFeel;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.ui.paint.RectanglePainter2D;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBValue;
import com.intellij.util.ui.UIUtilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.util.UserInterface.findChildComponent;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Conditional.when;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.DISABLED_SELECTED_COLOR;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.DISABLED_TEXT_COLOR;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.SELECTION_HEIGHT;
import static com.intellij.util.ui.JBUI.CurrentTheme.TabbedPane.TAB_HEIGHT;

/**
 * Adjusted version of {@link com.intellij.ide.ui.laf.darcula.ui.DarculaTabbedPaneUI} to support colored tabs
 *
 * @author Dan Cioca (Oracle)
 */
public class DBNTabbedPaneUI extends BasicTabbedPaneUI {
    public static final JBValue SELECTION_ARC = new JBValue.UIInteger("TabbedPane.tabSelectionArc", 0);
    public static final JBInsets EMPTY_INSETS = new JBInsets(0, 0, 0, 0);

    private enum TabStyle {underline, fill}

    private TabStyle tabStyle;
    private List<Component> hiddenArrowButtons;

    private int hoverTab = -1;
    private boolean tabsOverlapBorder;
    private boolean validating;
    private Color tabHoverColor;

    private PropertyChangeListener propertyChangeListener;
    private ComponentListener componentListener;
    private ChangeListener changeListener;
    private MouseListener mouseListener;
    private MouseMotionAdapter mouseMotionListener;

    private static final JBValue OFFSET = new JBValue.Float(1);

    @Override
    protected void installComponents() {
        super.installComponents();

        // hide scroll arrow buttons
        hiddenArrowButtons = new ArrayList<>(2);
        UserInterface.visitRecursively(tabPane, c -> when(c instanceof BasicArrowButton, () -> hiddenArrowButtons.add(c)));

        tabPane.setLayout(new WrappingLayout((TabbedPaneLayout) tabPane.getLayout()));

        Object hoverColor = tabPane.getClientProperty("TabbedPane.hoverColor");
        if (hoverColor instanceof Color) tabHoverColor = (Color) hoverColor;
    }

    @Override
    @Workaround
    protected JButton createScrollButton(int direction) {
        return new ScrollableTabButton(direction);
    }

    private static class ScrollableTabButton extends BasicArrowButton implements UIResource, SwingConstants {
        public ScrollableTabButton(int direction) {
            super(direction);
        }

        @Override
        public Dimension getPreferredSize() {
            // layout is driven by the size of the scroll buttons which is
            // hard-coded in the basic implementation
            return new Dimension(20, 20);
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        hiddenArrowButtons = null;
    }


    @Override
    protected void installDefaults() {
        super.installDefaults();

        modifyFontSize();

        Object rStyle = UIManager.get("TabbedPane.tabFillStyle");
        tabStyle = rStyle != null ? TabStyle.valueOf(rStyle.toString()) : TabStyle.underline;
        contentBorderInsets = EMPTY_INSETS;
        tabsOverlapBorder = UIManager.getBoolean("TabbedPane.tabsOverlapBorder");
    }

    private void modifyFontSize() {
        if (SystemInfo.isMac || SystemInfo.isLinux) {
            Font font = UIManager.getFont("TabbedPane.font");
            //tabPane.setFont(RelativeFont.NORMAL.fromResource("TabbedPane.fontSizeOffset", -1).derive(font));
            tabPane.setFont(font);
        }
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        propertyChangeListener = e -> {
            String propName = e.getPropertyName();
            if ("JTabbedPane.hasFullBorder".equals(propName) || "tabLayoutPolicy".equals(propName)) {
                boolean fullBorder = tabPane.getClientProperty("JTabbedPane.hasFullBorder") == Boolean.TRUE;
                contentBorderInsets = fullBorder ? JBUI.insets(0, 1, 1, 1) : EMPTY_INSETS;

                tabPane.revalidate();
                tabPane.repaint();
            } else if ("enabled".equals(propName)) {
                for (int ti = 0; ti < tabPane.getTabCount(); ti++) {
                    Component tc = tabPane.getTabComponentAt(ti);
                    if (tc != null) {
                        tc.setEnabled(e.getNewValue() == Boolean.TRUE);
                    }
                }
                tabPane.repaint();
            } else if ("tabPlacement".equals(propName)) {
                int index = tabPane.getSelectedIndex();
                tabPane.setSelectedIndex(-1);
                SwingUtilities.invokeLater(() -> tabPane.setSelectedIndex(index));
            }
        };

        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ensureSelectedTabIsVisible();
                adjustHiddenTabsAction();
            }
        };

        changeListener = e -> ensureSelectedTabIsVisible();

        mouseListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverTab = tabForCoordinate(tabPane, e.getX(), e.getY());
                tabPane.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverTab = -1;
                tabPane.repaint();
            }
        };

        mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverTab = tabForCoordinate(tabPane, e.getX(), e.getY());
                tabPane.repaint();
            }
        };

        tabPane.addPropertyChangeListener(propertyChangeListener);
        tabPane.addComponentListener(componentListener);
        tabPane.addChangeListener(changeListener);
        tabPane.addMouseListener(mouseListener);
        tabPane.addMouseMotionListener(mouseMotionListener);
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        if (changeListener != null) {
            tabPane.removeChangeListener(changeListener);
            changeListener = null;
        }
        if (componentListener != null) {
            tabPane.removeComponentListener(componentListener);
            componentListener = null;
        }

        if (propertyChangeListener != null) {
            tabPane.removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }

        if (mouseListener != null) {
            tabPane.removeMouseListener(mouseListener);
            mouseListener = null;
        }

        if (mouseMotionListener != null) {
            tabPane.removeMouseMotionListener(mouseMotionListener);
            mouseMotionListener = null;
        }
    }

    private void adjustHiddenTabsAction() {
        JViewport viewport = getScrollableViewport();
        if (viewport == null) return;

        DBNTabbedPane tabPane = getTabPane();
        JPanel hiddenTabsActionPanel = tabPane.getHiddenTabsActionPanel();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            Rectangle rectangle = rects[i];
            if (!viewport.getViewRect().contains(rectangle)) {
                hiddenTabsActionPanel.setVisible(true);
                return;
            }
        }
        hiddenTabsActionPanel.setVisible(false);
    }

    private JViewport getScrollableViewport() {
        return findChildComponent(tabPane, JViewport.class, c -> Objects.equals(c.getName(), "TabbedPane.scrollableViewport"));
    }

    @Override
    public int tabForCoordinate(JTabbedPane pane, int x, int y) {
        if (!pane.isValid()) return -1;

        // prevent tab switch on right mouse click if showing popup
        if (getTabPane().isShowingPopup()) return -1;

        return super.tabForCoordinate(pane, x, y);
    }

    private boolean isTopBottom() {
        return tabPane.getTabPlacement() == TOP || tabPane.getTabPlacement() == BOTTOM;
    }

    private void ensureSelectedTabIsVisible() {
        int index = tabPane.getSelectedIndex();
        JViewport viewport = getScrollableViewport();
        if (viewport == null || rects.length <= index || index < 0) return;
        Dimension viewSize = viewport.getViewSize();
        Rectangle viewRect = viewport.getViewRect();
        Rectangle tabRect = rects[index];
        if (viewRect.contains(tabRect)) {
            // adjust scrolling if right side gap is not justified
            if (isTopBottom()) {
                Rectangle bounds = getTabPane().getHiddenTabsActionPanel().getBounds();
                Point viewPosition = viewport.getViewPosition();
                int shift = tabPane.getWidth() - (viewport.getViewSize().width - viewPosition.x) - bounds.width;
                if (shift > 0) {
                    viewPosition.x = viewPosition.x - shift;
                    viewport.setViewPosition(viewPosition);
                }
            } else {
                // TODO implement for vertically laid out tabs
            }
            return;
        }
        Point tabViewPosition = new Point();
        int location;
        Dimension extentSize;
        if (isTopBottom()) {
            location = tabRect.x < viewRect.x ? tabRect.x : tabRect.x + tabRect.width - viewRect.width;
            viewport.setViewPosition(new Point(Math.max(0, Math.min(viewSize.width - viewRect.width, location)), tabRect.y));
            tabViewPosition.x = index == 0 ? 0 : tabRect.x;
            extentSize = new Dimension(viewSize.width - tabViewPosition.x, viewRect.height);
        } else {
            location = tabRect.y < viewRect.y ? tabRect.y : tabRect.y + tabRect.height - viewRect.height;
            viewport.setViewPosition(new Point(tabRect.x, Math.max(0, Math.min(viewSize.height - viewRect.height, location))));
            tabViewPosition.y = index == 0 ? 0 : tabRect.y;
            extentSize = new Dimension(viewRect.width, viewSize.height - tabViewPosition.y);
        }
        viewport.setExtentSize(extentSize);

        PointerInfo info = MouseInfo.getPointerInfo();
        if (info != null) {
            Point mouseLocation = info.getLocation();
            SwingUtilities.convertPointFromScreen(mouseLocation, tabPane);
            int oldHoverTab = hoverTab;
            hoverTab = tabForCoordinate(tabPane, mouseLocation.x, mouseLocation.y);
            if (oldHoverTab != hoverTab) {
                tabPane.repaint();
            }
        }
    }

    @Override
    protected Insets getContentBorderInsets(int tabPlacement) {
        Insets i = JBInsets.create(contentBorderInsets);
        rotateInsets(contentBorderInsets, i, tabPlacement);
        return i;
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        Rectangle bounds = g.getClipBounds();
        g.setColor(Colors.SEPARATOR_COLOR);

        int offset = getOffset();
        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            throw new UnsupportedOperationException("");
            // TODO g.fillRect(bounds.x + bounds.width - offset, bounds.y, offset, bounds.y + bounds.height);
        } else if (tabPlacement == TOP){
            g.fillRect(bounds.x, bounds.y + bounds.height - offset, bounds.x + bounds.width, offset);
        } else if (tabPlacement == BOTTOM){
            g.fillRect(bounds.x, bounds.y, bounds.x + bounds.width, offset);
        }

        super.paintTabArea(g, tabPlacement, selectedIndex);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Color color = nvl(getTabPane().getTabColorAt(tabIndex), tabPane.getBackground());

        if (tabPane.isEnabled()) {

            if (tabIndex == hoverTab) {
                color = LookAndFeel.isDarkMode() ?
                        Colors.lafBrighter(color, 6) :
                        Colors.lafDarker(color, 6);
            } else  if (isSelected) {
                //color = Colors.lafDarker(color, 2);
            }
        }
        g.setColor(color);

        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            w -= getOffset();
        } else {
            h -= getOffset();
        }

        g.fillRect(x, y, w, h);
    }

    private DBNTabbedPane getTabPane() {
        return (DBNTabbedPane) tabPane;
    }

    private @NotNull Color getHoverColor() {
        return tabHoverColor == null ? HOVER_COLOR : tabHoverColor;
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex,
                             String title, Rectangle textRect, boolean isSelected) {

        View v = getTextViewForTab(tabIndex);
        if (v != null || tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        } else { // tab disabled
            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

            g.setFont(font);
            g.setColor(DISABLED_TEXT_COLOR);
            UIUtilities.drawStringUnderlineCharAt(tabPane, g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent());
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (!isSelected || tabStyle != TabStyle.underline) return;

        switch (tabPlacement) {
            case LEFT: {
                int offset = SELECTION_HEIGHT.get();
                paintUnderline(g, x + w - offset, y, SELECTION_HEIGHT.get(), h);
                break;
            }
            case RIGHT: {
                int offset = 0;
                paintUnderline(g, x - offset, y, SELECTION_HEIGHT.get(), h);
                break;
            }
            case BOTTOM: {
                int offset = 0;
                paintUnderline(g, x, y - offset, w, SELECTION_HEIGHT.get());
                break;
            }
            //case TOP,
            default: {
                int offset = SELECTION_HEIGHT.get();
                paintUnderline(g, x, y + h - offset, w, SELECTION_HEIGHT.get());
            }
        }
    }

    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        int delta = SELECTION_HEIGHT.get() - getOffset();
        switch (tabPlacement) {
            case RIGHT:
            case LEFT: return 0;
            case BOTTOM: return delta / 2;
            //case TOP,
            default: return -delta / 2;
        }
    }

    @Override
    protected View getTextViewForTab(int tabIndex) {
        if (tabPane.isValid()) {
            return super.getTextViewForTab(tabIndex);
        }
        return null;
    }

    @Override
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        int delta = SELECTION_HEIGHT.get() - getOffset();
        switch (tabPlacement) {
            case TOP:
            case BOTTOM: return 0;
            case LEFT: return -delta / 2;
            //case RIGHT,
            default: return delta / 2;
        }
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) - 3; //remove magic constant '3' added by parent
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        int height = super.calculateTabHeight(tabPlacement, tabIndex, fontHeight) - 2; //remove magic constant '2' added by parent
        int minHeight = TAB_HEIGHT.get() - 2;
        return Math.max(height, minHeight);
    }

    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {}

    @Override
    public void paint(Graphics g, JComponent c) {
        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();

        if (!tabPane.isValid()) {
            tabPane.validate();
        }

        if (!tabPane.isValid()) {
            TabbedPaneLayout layout = (TabbedPaneLayout) tabPane.getLayout();
            layout.calculateLayoutInfo();
        }

        if (tabsOverlapBorder) {
            paintContentBorder(g, tabPlacement, selectedIndex);
        }

        if (!tabsOverlapBorder) {
            paintContentBorder(g, tabPlacement, selectedIndex);
        }
    }

    @Override
    protected Rectangle getTabBounds(int tabIndex, Rectangle dest) {
        dest.width = rects[tabIndex].width;
        dest.height = rects[tabIndex].height;

        JViewport viewport = getScrollableViewport();
        if (viewport != null) {
            Point vpp = viewport.getLocation();
            Point viewp = viewport.getViewPosition();
            dest.x = rects[tabIndex].x + vpp.x - viewp.x;
            dest.y = rects[tabIndex].y + vpp.y - viewp.y;
        } else {
            dest.x = rects[tabIndex].x;
            dest.y = rects[tabIndex].y;
        }
        return dest;
    }

    protected List<Integer> getHiddenTabIndexes() {
        JViewport viewport = getScrollableViewport();
        if (viewport == null) return Collections.emptyList();

        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            Rectangle viewRect = viewport.getViewRect();
            if (!viewRect.contains(rects[i])) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private final class WrappingLayout extends TabbedPaneLayout implements Wrapper<TabbedPaneLayout> {
        private final TabbedPaneLayout delegate;

        private WrappingLayout(TabbedPaneLayout delegate) {
            this.delegate = delegate;
        }

        @Override
        protected int preferredTabAreaHeight(int tabPlacement, int width) {
            return calculateMaxTabHeight(tabPlacement);
        }

        @Override
        protected int preferredTabAreaWidth(int tabPlacement, int height) {
            return calculateMaxTabWidth(tabPlacement);
        }

        @Override
        public void calculateLayoutInfo() {
            delegate.calculateLayoutInfo();
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
            delegate.addLayoutComponent(name, comp);
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            delegate.removeLayoutComponent(comp);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return delegate.preferredLayoutSize(parent);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return delegate.minimumLayoutSize(parent);
        }

        @Override
        protected void padSelectedTab(int tabPlacement, int selectedIndex) {}

        @Override
        public void layoutContainer(Container parent) {
            DBNTabbedPane tabPane = getTabPane();
            JPanel hiddenTabsActionPanel = tabPane.getHiddenTabsActionPanel();
            hiddenTabsActionPanel.setBounds(new Rectangle());
            delegate.layoutContainer(parent);

            removeCurlyTabEdge();

            if (!hiddenArrowButtons.isEmpty() && hiddenTabsActionPanel.isVisible()) {
                Rectangle bounds = null;
                for (Component button : hiddenArrowButtons) {
                    bounds = bounds == null ? button.getBounds() : bounds.union(button.getBounds());
                    button.setBounds(new Rectangle());
                }
                JViewport viewport = getScrollableViewport();
                // the last tab is selected, BasicTabbedPaneUI fails a bit
                if (bounds.isEmpty() && viewport != null) {
                    Rectangle viewportBounds = viewport.getBounds();
                    if (isTopBottom()) {
                        int buttonsWidth = 2 * hiddenArrowButtons.get(0).getPreferredSize().width;
                        viewportBounds.width -= buttonsWidth;
                        viewport.setBounds(viewportBounds);
                        ensureSelectedTabIsVisible();
                        bounds = new Rectangle(viewport.getX() + viewport.getWidth(), viewport.getY(), buttonsWidth, viewport.getHeight());
                    } else {
                        int buttonHeight = 2 * hiddenArrowButtons.get(0).getPreferredSize().height;
                        viewportBounds.height -= buttonHeight;
                        viewport.setBounds(viewportBounds);
                        ensureSelectedTabIsVisible();
                        bounds = new Rectangle(viewport.getX(), viewport.getY() + viewport.getHeight(), viewport.getWidth(), buttonHeight);
                    }
                    hiddenTabsActionPanel.setBounds(bounds);
                    return;
                }

                int placement = tabPane.getTabPlacement();
                int size;
                if (placement == TOP || placement == BOTTOM) {
                    size = preferredTabAreaHeight(placement, tabPane.getWidth());
                } else {
                    size = preferredTabAreaWidth(placement, tabPane.getWidth());
                }
                switch (placement) {
                    case TOP:     bounds.y -= size - bounds.height;
                    case BOTTOM:  bounds.height = size; break;
                    case LEFT:    bounds.x -= size - bounds.width;
                    case RIGHT:   bounds.width = size; break;
                }

                hiddenTabsActionPanel.setBounds(bounds);
            }
        }

        @Workaround
        private void removeCurlyTabEdge() {
            Field obj = ReflectionUtil.getDeclaredField(BasicTabbedPaneUI.class, "tabScroller");
            if (obj == null) return;

            Object obj1 = ReflectionUtil.getFieldValue(obj, DBNTabbedPaneUI.this);
            if (obj1 == null) return;

            Field obj2 = ReflectionUtil.getDeclaredField(obj1.getClass(), "croppedEdge");
            if (obj2 == null) return;

            Object obj3 = ReflectionUtil.getFieldValue(obj2, obj1);
            if (obj3 == null) return;

            ReflectionUtil.resetField(obj3, "shape");
        }

        @Override
        public TabbedPaneLayout unwrap() {
            return delegate;
        }
    }

    protected int getOffset() {
        return OFFSET.get();
    }

    private void paintUnderline(Graphics g, int x, int y, int w, int h) {
        g.setColor(isTabFocused() ? ENABLED_SELECTED_COLOR : DISABLED_SELECTED_COLOR);
        double arc = SELECTION_ARC.get();

        if (arc == 0) {
            g.fillRect(x, y, w, h);
        } else {
            RectanglePainter2D.FILL.paint((Graphics2D) g, x, y, w, h, arc, LinePainter2D.StrokeType.INSIDE, 1.0,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private boolean isTabFocused() {
        DBNTabbedPane tabPane = getTabPane();
        return
            tabPane.hasFocus() ||
            tabPane.hasInheritedFocus();
    }
}
