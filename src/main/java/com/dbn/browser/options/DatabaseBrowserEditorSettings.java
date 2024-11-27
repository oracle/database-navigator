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

package com.dbn.browser.options;

import com.dbn.browser.options.ui.DatabaseBrowserEditorSettingsForm;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.object.common.editor.DefaultEditorOption;
import com.dbn.object.common.editor.DefaultEditorType;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatabaseBrowserEditorSettings extends BasicProjectConfiguration<DatabaseBrowserSettings, DatabaseBrowserEditorSettingsForm> {

    private List<DefaultEditorOption> options = new ArrayList<>();

    public DatabaseBrowserEditorSettings(DatabaseBrowserSettings parent) {
        super(parent);
        options.add(new DefaultEditorOption(DBObjectType.VIEW, DefaultEditorType.SELECTION));
        options.add(new DefaultEditorOption(DBObjectType.PACKAGE, DefaultEditorType.SELECTION));
        options.add(new DefaultEditorOption(DBObjectType.TYPE, DefaultEditorType.SELECTION));
    }

    @NotNull
    @Override
    public DatabaseBrowserEditorSettingsForm createConfigurationEditor() {
        return new DatabaseBrowserEditorSettingsForm(this);
    }

    /*********************************************************
     *                        Custom                         *
     *********************************************************/
    public DefaultEditorOption getOption(DBObjectType objectType) {
        for (DefaultEditorOption option : options) {
            if (option.getObjectType().matches(objectType)) {
                return option;
            }
        }
        return null;
    }

    private static boolean contains(List<DefaultEditorOption> options, DBObjectType objectType) {
        for (DefaultEditorOption option : options) {
            if (option.getObjectType() == objectType){
                return true;
            }
        }
        return false;
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    public String getConfigElementName() {
        return "default-editors";
    }

    @Override
    public String getDisplayName() {
        return txt("app.browser.title.DatabaseBrowser");
    }

    @Override
    public String getHelpTopic() {
        return "browserSettings";
    }

    @Override
    public void readConfiguration(Element element) {
        List<DefaultEditorOption> newOptions = new ArrayList<>();
        List<Element> children = element.getChildren();
        for (Element child : children) {
            String objectTypeName = stringAttribute(child, "name");
            String editorTypeName = stringAttribute(child, "editor-type");
            DBObjectType objectType = DBObjectType.get(objectTypeName);
            DefaultEditorType editorType = DefaultEditorType.valueOf(editorTypeName);

            DefaultEditorOption comparator = new DefaultEditorOption(objectType, editorType);
            newOptions.add(comparator);
        }
        for (DefaultEditorOption option : options) {
            if (!contains(newOptions, option.getObjectType())) {
                newOptions.add(option);
            }
        }
        options = newOptions;
    }

    @Override
    public void writeConfiguration(Element element) {
        for (DefaultEditorOption option : options) {
            Element child = newElement(element, "object-type");
            child.setAttribute("name", cachedUpperCase(option.getObjectType().getName()));
            child.setAttribute("editor-type", option.getEditorType().name());
        }
    }
}
