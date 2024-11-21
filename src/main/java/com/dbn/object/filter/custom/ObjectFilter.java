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

package com.dbn.object.filter.custom;

import com.dbn.common.expression.ExpressionEvaluator;
import com.dbn.common.expression.ExpressionEvaluatorContext;
import com.dbn.common.filter.Filter;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode
public class ObjectFilter<T extends DBObject> implements Filter<T>, PersistentConfiguration {
    private DBObjectType objectType;
    private String expression = "";
    private boolean active = true;
    private final transient WeakRef<ObjectFilterSettings> settings;

    public ObjectFilter(ObjectFilterSettings settings) {
        this.settings = WeakRef.of(settings);
    }

    @NotNull
    public ObjectFilterSettings getSettings() {
        return settings.ensure();
    }

    public String getTitle() {
        return objectType.getName() + " Filter";
    }

    @Override
    public boolean accepts(T object) {
        if (expression == null) return true;
        if (!active) return true;

        var attributeValues = createEvaluatorContext(object);
        ExpressionEvaluator expressionEvaluator = getSettings().getExpressionEvaluator();
        return expressionEvaluator.evaluateBooleanExpression(expression, attributeValues);
    }

    public ExpressionEvaluatorContext createTestEvaluationContext() {
        List<ObjectFilterAttribute> attributesTypes = getDefinition().getAttributes();
        Map<String, Object> bindVariables = attributesTypes.stream().collect(Collectors.toMap(a -> a.getName(), a -> a.getTestValue()));
        ExpressionEvaluatorContext evaluatorContext = new ExpressionEvaluatorContext(bindVariables);
        evaluatorContext.setTemporary(true);
        return evaluatorContext;
    }

    private ExpressionEvaluatorContext createEvaluatorContext(T object) {
        ObjectFilterDefinition<T> definition = getDefinition();
        List<ObjectFilterAttribute> attributeTypes = definition.getAttributes();

        Map<String, Object> bindVariables = new HashMap<>();
        for (ObjectFilterAttribute attribute : attributeTypes) {
            String attributeName = attribute.getName();
            Object attributeValue = definition.getAttributeValue(object, attributeName);
            bindVariables.put(attributeName, attributeValue);
        }
        return new ExpressionEvaluatorContext(bindVariables);
    }

    public ObjectFilterDefinition<T> getDefinition() {
        return ObjectFilterDefinition.of(objectType);
    }

    @Override
    public void readConfiguration(Element element) {
        objectType = enumAttribute(element, "object-type", DBObjectType.class);
        active = booleanAttribute(element, "active", active);
        expression = readCdata(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnumAttribute(element, "object-type", objectType);
        setBooleanAttribute(element, "active", active);
        writeCdata(element, expression);
    }

    public Project getProject() {
        return getSettings().getProject();
    }

    public ConnectionId getConnectionId() {
        return getSettings().getConnectionId();
    }

    public ConnectionHandler getConnection() {
        return getSettings().getConnection();
    }

    public DBLanguagePsiFile createPreviewFile() {
        DBLanguage language = SQLLanguage.INSTANCE;
        ConnectionHandler connection = getConnection();
        DBLanguageDialect languageDialect = connection == null ?
                language.getMainLanguageDialect() :
                connection.getLanguageDialect(language);

        return DBLanguagePsiFile.createFromText(
                getProject(),
                "preview",
                languageDialect,
                expression,
                connection,
                null);
    }
}
