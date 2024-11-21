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

package com.dbn.common.content.dependency;

import com.dbn.common.content.DynamicContent;
import org.jetbrains.annotations.NotNull;

public class DualContentDependencyAdapter extends BasicDependencyAdapter implements ContentDependencyAdapter {
    private ContentDependency first;
    private ContentDependency second;

    private DualContentDependencyAdapter(DynamicContent firstContent, DynamicContent secondContent) {
        first = dependency(firstContent);

        if (firstContent == secondContent) {
            // dual dependencies may rely on same content
            second = first;
        } else {
            second = dependency(secondContent);
        }
    }

    public static DualContentDependencyAdapter create(DynamicContent firstContent, DynamicContent secondContent) {
        return new DualContentDependencyAdapter(firstContent, secondContent);
    }

    @Override
    public boolean canLoad() {
        if (!content(first).isLoaded()) return false;
        if (!content(second).isLoaded()) return false;

        return true;
    }

    @Override
    public boolean canLoadInBackground() {
        if (content(first).isLoadingInBackground()) return false;
        if (content(second).isLoadingInBackground()) return false;

        return true;
    }

    @Override
    public boolean isDependencyDirty() {
        if (content(first).isDirty()) return true;
        if (content(second).isDirty()) return true;

        return false;
    }

    @Override
    public void refreshSources() {
        content(first).refresh();
        content(second).refresh();
        super.refreshSources();
    }

    @Override
    public boolean canLoadFast() {
        DynamicContent firstContent = content(first);
        DynamicContent secondContent = content(second);
        return
            firstContent.isLoaded() && !firstContent.isDirty() &&
            secondContent.isLoaded() && !secondContent.isDirty();
    }

    @Override
    public void beforeLoad(boolean force) {
        if (!force) return;

        content(first).refresh();
        content(second).refresh();
    }

    @Override
    public void afterLoad() {
        first.updateSignature();
        second.updateSignature();
    }

    private static DynamicContent content(ContentDependency dependency) {
        return dependency.getSourceContent();
    }

    @NotNull
    private static ContentDependency dependency(DynamicContent content) {
        if (content == null) return VoidContentDependency.INSTANCE;
        return new BasicContentDependency(content);
    }

    @Override
    public void dispose() {
        first = VoidContentDependency.INSTANCE;
        second = VoidContentDependency.INSTANCE;
        super.dispose();
    }
}
