/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.message.ui;

import com.dbn.common.project.ProjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import javax.swing.Action;

@Getter
public class MessageBundleDialogConfig {
    private final ProjectRef project;
    private final String title;
    private String mainMessage;
    private Object contextObject;
    private Action[] actions;

    private MessageBundleDialogConfig(Project project, String title) {
        this.project = ProjectRef.of(project);
        this.title = title;
    }

    public static MessageBundleDialogConfig create(Project project, String title) {
        return new MessageBundleDialogConfig(project, title);
    }

    public Project getProject() {
        return project.ensure();
    }

    public MessageBundleDialogConfig withMainMessage(String mainMessage) {
        this.mainMessage = mainMessage;
        return this;
    }

    public MessageBundleDialogConfig withContextObject(Object contextObject) {
        this.contextObject = contextObject;
        return this;
    }

    public MessageBundleDialogConfig withActions(Action... actions) {
        this.actions = actions;
        return this;
    }
}
