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

package com.dbn.common.thread;

import com.dbn.common.project.ProjectRef;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class ThreadInfo extends PropertyHolderBase.IntStore<ThreadProperty> {
    private static final ThreadLocal<ThreadInfo> THREAD_INFO = new ThreadLocal<>();
    private ProjectRef project;
    private ThreadInfo invoker;

    public static ThreadInfo copy() {
        ThreadInfo current = current();
        ThreadInfo copy = new ThreadInfo();
        copy.inherit(current);
        copy.setProject(current.getProject());
        copy.setInvoker(current.getInvoker());
        return copy;
    }

    public static ThreadInfo current() {
        ThreadInfo threadInfo = THREAD_INFO.get();
        if (threadInfo == null) {
            threadInfo = new ThreadInfo();
            THREAD_INFO.set(threadInfo);
        }
        return threadInfo;
    }

    @Nullable
    public Project getProject() {
        return ProjectRef.get(project);
    }

    public void setProject(@Nullable Project project) {
        this.project = ProjectRef.of(project);
    }

    @Override
    protected ThreadProperty[] properties() {
        return ThreadProperty.VALUES;
    }

    @Override
    public void merge(@Nullable PropertyHolder<ThreadProperty> source) {
        if (source == null) return;

        for (ThreadProperty property : properties()) {
            if (property.propagatable() && source.is(property)) {
                set(property, true);
            }
        }
    }

    @Override
    public void unmerge(@Nullable PropertyHolder<ThreadProperty> source) {
        if (source == null) return;

        for (ThreadProperty property : properties()) {
            if (property.propagatable() && source.is(property)) {
                set(property, false);
            }
        }
    }
}
