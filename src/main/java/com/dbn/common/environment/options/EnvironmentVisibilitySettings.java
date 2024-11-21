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

package com.dbn.common.environment.options;


import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import lombok.Getter;
import org.jdom.Element;

@Getter
public class EnvironmentVisibilitySettings implements PersistentConfiguration {
    private final BooleanSetting connectionTabs = new BooleanSetting("connection-tabs", true);
    private final BooleanSetting objectEditorTabs = new BooleanSetting("object-editor-tabs", true);
    private final BooleanSetting scriptEditorTabs = new BooleanSetting("script-editor-tabs", false);
    private final BooleanSetting dialogHeaders = new BooleanSetting("dialog-headers", true);
    private final BooleanSetting executionResultTabs = new BooleanSetting("execution-result-tabs", true);

    @Override
    public void readConfiguration(Element element) {
        connectionTabs.readConfiguration(element);
        dialogHeaders.readConfiguration(element);
        objectEditorTabs.readConfiguration(element);
        scriptEditorTabs.readConfiguration(element);
        executionResultTabs.readConfiguration(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        connectionTabs.writeConfiguration(element);
        dialogHeaders.writeConfiguration(element);
        objectEditorTabs.writeConfiguration(element);
        scriptEditorTabs.writeConfiguration(element);
        executionResultTabs.writeConfiguration(element);
    }
}
