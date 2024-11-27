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

package com.dbn.browser;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.collections.CompactArrayList;
import com.dbn.common.filter.Filter;
import com.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Checks.isTrue;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatabaseBrowserUtils {
    @Nullable
    public static TreePath createTreePath(@NotNull BrowserTreeNode treeNode) {
        try {
            Project project = treeNode.getProject();
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
            boolean isSingleRoot = browserManager.isSingleTreeMode();

            int treeDepth = treeNode.getTreeDepth();
            int nodeIndex = isSingleRoot ? treeDepth : treeDepth - 1;
            if (nodeIndex < 0) {
                return null;
            }

            BrowserTreeNode[] path = new BrowserTreeNode[nodeIndex];
            while (treeNode != null) {
                treeDepth = treeNode.getTreeDepth();
                path[isSingleRoot ? treeDepth - 1 : treeDepth - 2] = treeNode;
                if (treeNode instanceof DatabaseBrowserManager) break;
                if (!isSingleRoot && treeNode instanceof DBObjectBundle) break;
                treeNode = treeNode.getParent();
            }
            return new TreePath(path);
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            return null;
        } catch (IllegalArgumentException | IllegalStateException e) {
            conditionallyLog(e);
            // workaround for TreePath "Path elements must be non-null"
            return null;
        }
    }

    public static boolean treeVisibilityChanged(
            List<BrowserTreeNode> possibleTreeNodes,
            List<BrowserTreeNode> actualTreeNodes,
            Filter<BrowserTreeNode> filter) {
        for (BrowserTreeNode treeNode : possibleTreeNodes) {
            if (treeNode != null) {
                if (filter.accepts(treeNode)) {
                    if (!actualTreeNodes.contains(treeNode)) return true;
                } else {
                    if (actualTreeNodes.contains(treeNode)) return true;
                }
            }
        }
        return false;
    }

    public static List<BrowserTreeNode> createList(BrowserTreeNode... treeNodes) {
        List<BrowserTreeNode> treeNodeList = new ArrayList<>();
        for (BrowserTreeNode treeNode : treeNodes) {
            if (treeNode != null) {
                treeNodeList.add(treeNode);
            }
        }
        return new CompactArrayList<>(treeNodeList);
    }

    public static void markSkipBrowserAutoscroll(VirtualFile file) {
        file.putUserData(UserDataKeys.SKIP_BROWSER_AUTOSCROLL, true);
    }

    public static void unmarkSkipBrowserAutoscroll(VirtualFile file) {
        file.putUserData(UserDataKeys.SKIP_BROWSER_AUTOSCROLL, false);
    }

    static boolean isSkipBrowserAutoscroll(VirtualFile file) {
        return isTrue(file.getUserData(UserDataKeys.SKIP_BROWSER_AUTOSCROLL));
    }
}
