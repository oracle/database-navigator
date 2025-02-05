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

package com.dbn.editor.data.filter;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Strings;
import com.dbn.data.sorting.SortingState;
import com.dbn.editor.data.filter.ui.DatasetCustomFilterForm;
import com.dbn.object.DBDataset;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DatasetCustomFilter extends DatasetFilterImpl {
    private @NonNls String condition;

    protected DatasetCustomFilter(DatasetFilterGroup parent, String name) {
        super(parent, name, DatasetFilterType.CUSTOM);
    }

    @Override
    public void generateName() {}

    @Override
    public String getVolatileName() {
        ConfigurationEditorForm configurationEditorForm = getSettingsEditor();
        if (configurationEditorForm != null) {
            DatasetCustomFilterForm customFilterForm = (DatasetCustomFilterForm) configurationEditorForm;
            return customFilterForm.getFilterName();
        }
        return super.getDisplayName();
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public Icon getIcon() {
        return getError() == null ?
                Icons.DATASET_FILTER_CUSTOM :
                Icons.DATASET_FILTER_CUSTOM_ERR;
    }

    @Override
    public String createSelectStatement(DBDataset dataset, SortingState sortingState) {
        setError(null);
        @NonNls StringBuilder buffer = new StringBuilder();
        DatasetFilterUtil.createSimpleSelectStatement(dataset, buffer);
        buffer.append(" where ");
        buffer.append(condition);
        DatasetFilterUtil.addOrderByClause(dataset, buffer, sortingState);
        return buffer.toString();
    }

    /*****************************************************
     *                   Configuration                   *
     *****************************************************/
    @Override
    @NotNull
    public ConfigurationEditorForm createConfigurationEditor() {
        DBDataset dataset = Failsafe.nn(lookupDataset());
        return new DatasetCustomFilterForm(dataset, this);
    }

    @Override
    public void readConfiguration(Element element) {
        super.readConfiguration(element);
        Element conditionElement = element.getChild("condition");
        condition = Settings.readCdata(conditionElement);
        condition = Strings.replace(condition, "<br>", "\n");
        condition = Strings.replace(condition, "<sp>", "  ");
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        element.setAttribute("type", "custom");
        Element conditionElement = newElement(element, "condition");
        if (this.condition == null) return;

        @NonNls String condition = Strings.replace(this.condition, "\n", "<br>");
        condition = Strings.replace(condition, "  ", "<sp>");
        writeCdata(conditionElement, condition);
    }

}
