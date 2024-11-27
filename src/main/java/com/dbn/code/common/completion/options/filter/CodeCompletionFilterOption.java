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

package com.dbn.code.common.completion.options.filter;

import com.dbn.code.common.completion.options.filter.ui.CheckedTreeNodeProvider;
import com.dbn.code.common.completion.options.filter.ui.CodeCompletionFilterTreeNode;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.CheckedTreeNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import javax.swing.Icon;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
@Setter
@EqualsAndHashCode
public class CodeCompletionFilterOption implements CheckedTreeNodeProvider, PersistentConfiguration{
    private transient final CodeCompletionFilterSettings filterSettings;

    private TokenTypeCategory tokenTypeCategory = TokenTypeCategory.UNKNOWN;
    private DBObjectType objectType;
    private boolean selected;

    CodeCompletionFilterOption(CodeCompletionFilterSettings filterSettings) {
        this.filterSettings = filterSettings;
    }

    public String getName() {
        return objectType == null ?
                tokenTypeCategory.getName() :
                cachedUpperCase(objectType.getName());
    }

    public Icon getIcon() {
        return objectType == null ? null : objectType.getIcon();
    }

    @Override
    public void readConfiguration(Element element) {
        if (element != null) {
            String filterElementType = stringAttribute(element, "type");
            if (Objects.equals("OBJECT", filterElementType)) {
                String objectTypeName = stringAttribute(element, "id");
                objectType = DBObjectType.get(objectTypeName);
            } else {
                String tokenTypeName = stringAttribute(element, "id");
                tokenTypeCategory = TokenTypeCategory.getCategory(tokenTypeName);
            }
            selected = booleanAttribute(element, "selected", selected);
        }

    }

    @Override
    public void writeConfiguration(Element element) {
        if (objectType != null) {
            element.setAttribute("type", "OBJECT");
            element.setAttribute("id", objectType.getName());

        } else {
            element.setAttribute("type", "RESERVED_WORD");
            element.setAttribute("id", tokenTypeCategory.getName());
        }

        setBooleanAttribute(element, "selected", selected);
    }

    @Override
    public CheckedTreeNode createCheckedTreeNode() {
        return new CodeCompletionFilterTreeNode(this, selected);
    }
}
