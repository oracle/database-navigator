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

package com.dbn.common.ui.tree;

import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.thread.Dispatch.alarm;
import static com.dbn.common.thread.Dispatch.alarmRequest;
import static com.dbn.common.ui.util.UserInterface.getParentOfType;

public class DBNStickyPathTree extends DBNTree{
    private static final TreeModel EMPTY_TREE_MODEL = new EmptyTreeModel();
    private final JScrollPane scrollPane;
    private final JPanel headerPanel;
    private final Container container;

    private final State currentState = new State();
    private final Alarm refreshAlarm = alarm(this);
    private final boolean scrollBarOpaque;

    public DBNStickyPathTree(@NotNull DBNTree sourceTree) {
        super(sourceTree);
        setBackground(sourceTree.getBackground());
        setBorder(sourceTree.getBorder());
        setRootVisible(sourceTree.isRootVisible());
        setShowsRootHandles(sourceTree.getShowsRootHandles());
        setCellRenderer(sourceTree.getCellRenderer());
        setToggleClickCount(sourceTree.getToggleClickCount());
        setPreferredSize(new Dimension(-1, 0));
        //setBackground(Colors.lafDarker(sourceTree.getBackground(), 1));

        scrollPane = nn(getParentOfType(sourceTree, JScrollPane.class));
        //Reflection.invokeMethod(scrollPane, "setOverlappingScrollBar", false);
        container = scrollPane.getParent();

        JBLayeredPane layeredPane = new JBLayeredPane();
        UserInterface.replaceComponent(scrollPane, layeredPane);
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);


        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(-1, 0));
        headerPanel.add(this, BorderLayout.CENTER);
        headerPanel.setBorder(Borders.lineBorder(DarculaUIUtil.getOutlineColor(false, false), 0, 0, 1, 0));
        headerPanel.setBackground(sourceTree.getBackground());
        layeredPane.add(headerPanel, JLayeredPane.PALETTE_LAYER);

        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeScrollPane();
                resizeHeaderOverlay();
            }
        });

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();

        scrollBarOpaque = verticalScrollBar.isOpaque();
        verticalScrollBar.addAdjustmentListener(e -> refreshHeaderOverlay());
        horizontalScrollBar.addAdjustmentListener(e -> refreshHeaderOverlay());
        
        sourceTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                refreshHeaderOverlay();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                refreshHeaderOverlay();
            }
        });

        sourceTree.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                refreshHeaderOverlay();
            }
        });
        sourceTree.addTreeSelectionListener(e -> selectionHandover(() -> clearSelection()));

        addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                TreePath treePath = event.getPath();
                sourceTree.getSelectionModel().setSelectionPath(treePath);
                sourceTree.collapsePath(treePath);

                int overlayRows = computeOverlayRows(treePath.getParentPath());
                int overlayHeight = computeOverlayHeight(overlayRows);
                headerPanel.setPreferredSize(new Dimension(-1, overlayHeight));

                Rectangle bounds = sourceTree.getPathBounds(treePath);
                if (bounds == null) return;

                int x = (int) bounds.getX();
                int y = (int) (bounds.getY() - overlayHeight);
                bounds.setLocation(x, y);
                sourceTree.scrollRectToVisible(bounds);
                refreshHeaderOverlay();
            }
        });

        addTreeSelectionListener(e -> selectionHandover(() -> sourceTree.getSelectionModel().setSelectionPath(e.getPath())));

        addMouseListener(createMouseListener());
    }

    private void selectionHandover(Runnable runnable) {
        if (currentState.selectionHandover) return;

        try {
            currentState.selectionHandover = true;
            runnable.run();
        } finally {
            currentState.selectionHandover = false;
        }
    }

    private MouseListener createMouseListener() {
        return Mouse.listener().
                onRelease(e -> {
                    if (e.getButton() != MouseEvent.BUTTON3) return;

                    TreePath path = Trees.getPathAtMousePosition(this, e);
                    getSourceTree().showContextMenu(path, e.getX(), e.getY() + getVerticalScroll());
                });
    }

    private int getVerticalScroll() {
        return scrollPane.getVerticalScrollBar().getValue();
    }

    protected boolean checkFeatureEnabled() {
        return true;
    }

    private void resizeHeaderOverlay() {
        int width = container.getWidth() - scrollPane.getVerticalScrollBar().getWidth();
        int height = getOverlayHigh();
        headerPanel.setBounds(0, 0, width, height);
    }

    private void resizeScrollPane() {
        int width = container.getWidth();
        int height = container.getHeight();
        scrollPane.setBounds(0, 0, width, height);
    }

    private int getOverlayHigh() {
        return (int) headerPanel.getPreferredSize().getHeight();
    }

    private DBNTree getSourceTree() {
        return getParentComponent();
    }

    private void refreshHeaderOverlay() {
        int verticalScroll = getVerticalScroll();
        if (currentState.checkScroll(verticalScroll)) return;

        alarmRequest(refreshAlarm, 0, true, () -> renderHeaderOverlay());
    }


    private void renderHeaderOverlay() {
        TreePath parentPath = resolveHiddenTreePath();
        if (currentState.checkPath(parentPath)) return;

        int overlayRows = computeOverlayRows(parentPath);
        setVisibleRowCount(overlayRows);

        int overlayHeight = computeOverlayHeight(overlayRows);
        headerPanel.setPreferredSize(new Dimension(-1, overlayHeight));
        resizeHeaderOverlay();
        resizeScrollPane();

        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        if (parentPath != null && overlayRows > 0) {
            scrollBar.setOpaque(true);
            setVisible(true);
            setModel(new PathTreeModel(parentPath));
            Trees.expandAll(this);
        } else {
            setVisible(false);
            setModel(EMPTY_TREE_MODEL);
            scrollBar.setOpaque(scrollBarOpaque);
        }

        UserInterface.repaint(scrollPane);
    }

    private int computeOverlayRows(@Nullable TreePath treePath) {
        if (treePath == null) return 0;

        return rootVisible ? treePath.getPathCount() : treePath.getPathCount() - 1;
    }

    private int computeOverlayHeight(int visibleRows) {
        return visibleRows > 0 ? visibleRows * getRowHeight() + 1 : 0;
    }

    @Nullable
    private TreePath resolveHiddenTreePath() {
        if (!checkFeatureEnabled()) return null;

        int verticalScroll = getVerticalScroll();
        if (verticalScroll < getRowHeight()) return null;

        verticalScroll = verticalScroll + getOverlayHigh();
        DBNTree sourceTree = getSourceTree();
        TreePath treePath = sourceTree.getClosestPathForLocation(0, verticalScroll);
        return treePath.getParentPath();
    }


    private static class PathTreeModel implements ReadonlyTreeModel {
        private final List<?> path;

        public PathTreeModel(TreePath path) {
            this.path = Arrays.asList(path.getPath());
        }

        @Override
        public Object getRoot() {
            return path.isEmpty() ? null : path.get(0);
        }

        @Override
        public Object getChild(Object parent, int index) {
            int parentIndex = path.indexOf(parent);
            return index == 0 ? path.get(parentIndex + 1) : null;
        }

        @Override
        public int getChildCount(Object parent) {
            int parentIndex = path.indexOf(parent);
            return parentIndex < path.size() - 1 ? 1 : 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return false;
        }
    }


    private static class State {
        private int verticalScroll;
        private int scrollbarWidth;
        private boolean selectionHandover;
        private TreePath treePath;

        boolean checkPath(TreePath treePath) {
            if (Objects.equals(this.treePath, treePath)) return true;

            this.treePath = treePath;
            return false;
        }

        boolean checkScroll(int verticalScroll) {
            if (this.verticalScroll == verticalScroll) return true;

            this.verticalScroll = verticalScroll;
            return false;
        }

        boolean checkScrollbarWidth(int scrollbarWidth) {
            if (this.scrollbarWidth == scrollbarWidth) return true;

            this.scrollbarWidth = scrollbarWidth;
            return false;
        }
    }
}
