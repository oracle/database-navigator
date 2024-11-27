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

package com.dbn.data.find.action;

import com.dbn.common.action.ToggleAction;
import com.dbn.data.find.DataSearchComponent;
import com.intellij.find.FindModel;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import lombok.Getter;

import javax.swing.Icon;

@Getter
public abstract class DataSearchHeaderToggleAction extends ToggleAction implements DumbAware {
    private final DataSearchComponent searchComponent;

    protected DataSearchHeaderToggleAction(DataSearchComponent searchComponent, String text, Icon icon, Icon hoveredIcon, Icon selectedIcon) {
        super(text);
        this.searchComponent = searchComponent;
        Presentation templatePresentation = getTemplatePresentation();
        templatePresentation.setIcon(icon);
        templatePresentation.setHoveredIcon(hoveredIcon);
        templatePresentation.setSelectedIcon(selectedIcon);
    }

    protected FindModel getFindModel() {
        return getSearchComponent().getFindModel();
    }
}
