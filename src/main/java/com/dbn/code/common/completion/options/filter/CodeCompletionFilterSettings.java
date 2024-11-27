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
import com.dbn.code.common.completion.options.filter.ui.CodeCompletionFilterSettingsForm;
import com.dbn.code.common.completion.options.filter.ui.CodeCompletionFilterTreeNode;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ObjectTypeFilter;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.CheckedTreeNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CodeCompletionFilterSettings
        extends BasicConfiguration<CodeCompletionFiltersSettings, CodeCompletionFilterSettingsForm>
        implements CheckedTreeNodeProvider, ObjectTypeFilter {

    private static final int SCHEMA_TYPE_USER = 0;
    private static final int SCHEMA_TYPE_PUBLIC = 1;
    private static final int SCHEMA_TYPE_ANY = 2;

    private final boolean extended;
    private final CodeCompletionFilterOptionBundle rootFilterOptions;
    private final CodeCompletionFilterOptionBundle userSchemaOptions;
    private final CodeCompletionFilterOptionBundle publicSchemaOptions;
    private final CodeCompletionFilterOptionBundle anySchemaOptions;

    public CodeCompletionFilterSettings(CodeCompletionFiltersSettings parent, boolean extended) {
        super(parent);
        this.extended = extended;
        rootFilterOptions = new CodeCompletionFilterOptionBundle("Root elements", this);
        userSchemaOptions = new CodeCompletionFilterOptionBundle("User schema", this);
        publicSchemaOptions = new CodeCompletionFilterOptionBundle("Public schema", this);
        anySchemaOptions = new CodeCompletionFilterOptionBundle("Any schema", this);
    }

    @Override
    public String getDisplayName() {
        return extended ?
                txt("cfg.codeCompletion.title.Extended") :
                txt("cfg.codeCompletion.title.Basic");
    }

    public boolean acceptReservedWord(TokenTypeCategory tokenTypeCategory) {
        if (tokenTypeCategory != TokenTypeCategory.UNKNOWN) {
            for(CodeCompletionFilterOption option : rootFilterOptions.getOptions()) {
                if (option.getObjectType() == null && option.getTokenTypeCategory() == tokenTypeCategory) {
                    return option.isSelected();
                }
            }
        }
        return false;
    }

    @Override
    public boolean acceptsRootObject(DBObjectType objectType) {
        Set<DBObjectType> objectTypes = objectType.isGeneric() ? objectType.getInheritingTypes() : null;
        for(CodeCompletionFilterOption option : rootFilterOptions.getOptions()) {
            if (objectTypes != null) {
                for (DBObjectType type : objectTypes) {
                    if (option.getObjectType() == type) {
                        return option.isSelected();
                    }
                }
            }
            else if (option.getObjectType() == objectType) {
                return option.isSelected();
            }
        }
        return true;   // return true for object types which are not configured
    }

    @Override
    public boolean acceptsCurrentSchemaObject(DBObjectType objectType) {
        return showSchemaObject(SCHEMA_TYPE_USER, objectType);
    }

    @Override
    public boolean acceptsPublicSchemaObject(DBObjectType objectType) {
        return showSchemaObject(SCHEMA_TYPE_PUBLIC, objectType);
    }

    @Override
    public boolean acceptsAnySchemaObject(DBObjectType objcetType) {
        return showSchemaObject(SCHEMA_TYPE_ANY, objcetType);
    }

    @Override
    public boolean acceptsObject(DBSchema schema, DBSchema currentSchema, DBObjectType objectType) {
        boolean isPublic = schema.isPublicSchema();
        boolean isCurrent = Objects.equals(schema, currentSchema);
        return
            (isPublic && acceptsPublicSchemaObject(objectType)) ||
            (isCurrent && acceptsCurrentSchemaObject(objectType)) ||
            (!isPublic && !isCurrent && acceptsAnySchemaObject(objectType));
    }

    private boolean showSchemaObject(int schemaType, DBObjectType objectType) {
        Set<DBObjectType> objectTypes = objectType.isGeneric() ? objectType.getInheritingTypes() : null;
        CodeCompletionFilterOptionBundle schemaOptions;
        switch (schemaType) {
            case SCHEMA_TYPE_USER: schemaOptions = userSchemaOptions; break;
            case SCHEMA_TYPE_PUBLIC: schemaOptions = publicSchemaOptions; break;
            default: schemaOptions = anySchemaOptions; break;
        }

        for(CodeCompletionFilterOption option : schemaOptions.getOptions()) {
            if (objectTypes != null) {
                for (DBObjectType type : objectTypes) {
                    if (option.getObjectType() == type) {
                        return option.isSelected();
                    }
                }
            }
            else if (option.getObjectType() == objectType) {
                return option.isSelected();
            }
        }
        return false;
    }


    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public CodeCompletionFilterSettingsForm createConfigurationEditor() {
        return new CodeCompletionFilterSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return extended ? "extended-filter" : "basic-filter";
    }

    @Override
    public void readConfiguration(Element element) {
        rootFilterOptions.readConfiguration(element);

        Element userSchemaElement = element.getChild("user-schema");
        userSchemaOptions.readConfiguration(userSchemaElement);

        Element publicSchemaElement = element.getChild("public-schema");
        publicSchemaOptions.readConfiguration(publicSchemaElement);

        Element anySchemaElement = element.getChild("any-schema");
        anySchemaOptions.readConfiguration(anySchemaElement);
    }

    @Override
    public void writeConfiguration(Element element) {
        rootFilterOptions.writeConfiguration(element);

        Element userSchemaElement = newElement(element,"user-schema");
        userSchemaOptions.writeConfiguration(userSchemaElement);

        Element publicSchemaElement = newElement(element,"public-schema");
        publicSchemaOptions.writeConfiguration(publicSchemaElement);

        Element anySchemaElement = newElement(element,"any-schema");
        anySchemaOptions.writeConfiguration(anySchemaElement);
    }

    /*********************************************************
     *              CheckedTreeNodeProvider                  *
     *********************************************************/
    @Override
    public CheckedTreeNode createCheckedTreeNode() {
        CodeCompletionFilterTreeNode rootNode = new CodeCompletionFilterTreeNode(this, false);
        for (CodeCompletionFilterOption option: rootFilterOptions.getOptions()) {
            rootNode.add(option.createCheckedTreeNode());
        }
        rootNode.add(userSchemaOptions.createCheckedTreeNode());
        rootNode.add(publicSchemaOptions.createCheckedTreeNode());
        rootNode.add(anySchemaOptions.createCheckedTreeNode());
        rootNode.updateCheckedStatusFromChildren();
        return rootNode;
    }


}

