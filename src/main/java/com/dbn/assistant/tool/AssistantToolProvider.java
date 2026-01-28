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

package com.dbn.assistant.tool;

import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.common.util.Unsafe;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.IllegalConfigurationException;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.assistant.AssistantComponent.OBJECT_MAPPER;
import static dev.langchain4j.internal.JsonSchemaElementUtils.jsonSchemaElementFrom;

@Slf4j
public class AssistantToolProvider implements ToolProvider {
    private final AssistantToolCache cache;

    public AssistantToolProvider(AssistantToolCache cache) {
        this.cache = cache;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();

        for (AssistantTool tool : cache.getAvailableTools()) {
            for (Method method : tool.getClass().getDeclaredMethods()) {
                processToolMethod(tools, tool, method);
            }
        }
        return new ToolProviderResult(tools);
    }

    @Nullable
    public static Method getAnnotatedMethod(@NotNull Method method) {
        if (method.isAnnotationPresent(UtilitySpec.class)) return method;

        Class<?> declaringClass = method.getDeclaringClass();
        if (Proxy.isProxyClass(declaringClass)) {
            for (Class<?> spec : declaringClass.getInterfaces()) {
                try {
                    Method interfaceMethod = spec.getMethod(method.getName(), method.getParameterTypes());
                    if (interfaceMethod.isAnnotationPresent(UtilitySpec.class)) return interfaceMethod;
                } catch (NoSuchMethodException e) {
                    // Ignore and continue searching in the next interface
                }
            }
        }
        return null;
    }

    private void processToolMethod(Map<ToolSpecification, ToolExecutor> tools, Object object, Method method) {
        method = getAnnotatedMethod(method);
        if (method == null) return;
        if (isDiscontinued(method)) return;

        ToolSpecification specification = buildSpecification(method);
        if (isAlreadyDefined(tools, specification)) {
            throw new IllegalConfigurationException("Duplicated definition for tool: " + specification.name());
        }

        ToolExecutor executor = buildToolExecutor(object, method);
        tools.put(specification, executor);
    }

    private static boolean isAlreadyDefined(Map<ToolSpecification, ToolExecutor> tools, ToolSpecification specification) {
        return tools.keySet().stream().anyMatch(s -> s.name().equals(specification.name()));
    }

    private static boolean isDiscontinued(Method method) {
        UtilitySpec utilitySpec = method.getAnnotation(UtilitySpec.class);
        return utilitySpec.discontinued();
    }

    private static ToolSpecification buildSpecification(Method method) {
        ToolSpec toolSpec = method.getDeclaringClass().getAnnotation(ToolSpec.class);
        UtilitySpec utilitySpec = method.getAnnotation(UtilitySpec.class);
        Tool tool = method.getAnnotation(Tool.class);

        ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);

        String description =
                "type = " + toolSpec.type() + "\n" +
                "category = " + toolSpec.category() + "\n" +
                "name = " + utilitySpec.name() + "\n" +
                "description = " + utilitySpec.description();

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            description = description + "\nreturning=void";
        } else if (Collection.class.isAssignableFrom(returnType)) {
            description = description + "\nreturning=array";
        } else {
            JsonSchemaElement returnElement = Unsafe.warned(null, () -> jsonSchemaElementFrom(returnType));
            if (returnElement instanceof JsonObjectSchema objectElement) {
                String returnDescriptor = buildReturnDescriptor(objectElement);
                description = description + "\nreturning=" + returnDescriptor;
            } else if (returnElement != null) {
                String elementType = getElementType(returnElement);
                description = description + "\nreturning=" + elementType;
            }
        }

        return ToolSpecification
                .builder()
                .name(tool.name())
                .description(description)
                .parameters(specification.parameters())
                .build();
    }

    @SneakyThrows
    private static String buildReturnDescriptor(JsonObjectSchema returnElement) {
        Map<String, Object> descriptor = buildReturnDescr(returnElement);

        return OBJECT_MAPPER.writeValueAsString(descriptor);
    }

    private static @NotNull Map<String, Object> buildReturnDescr(JsonObjectSchema element) {
        @NonNls
        Map<String, Object> descriptor = new LinkedHashMap<>();

        descriptor.put("type", "object");
        descriptor.put("description", element.description());

        Map<String, JsonSchemaElement> properties = element.properties();

        List<Map<String, Object>> attributes = new ArrayList<>();
        for (String attribute : properties.keySet()) {
            JsonSchemaElement property = properties.get(attribute);

            if (property instanceof JsonObjectSchema objectSchema) {
                Map<String, Object> childDescriptor = new LinkedHashMap<>();
                childDescriptor.put("name", attribute);
                childDescriptor.putAll(buildReturnDescr(objectSchema));
                attributes.add(childDescriptor);
                continue;
            }

            String type = getElementType(property);
            attributes.add(Map.of(
                    "name", attribute,
                    "type", type,
                    "description", property.description()));
        }

        descriptor.put("attributes", attributes);
        return descriptor;
    }

    @NonNls
    private static String getElementType(JsonSchemaElement element) {
        if (element instanceof JsonStringSchema) return "string";
        if (element instanceof JsonBooleanSchema) return "boolean";
        if (element instanceof JsonNumberSchema) return "number";
        if (element instanceof JsonIntegerSchema) return "integer";
        if (element instanceof JsonEnumSchema) return "enumeration";
        if (element instanceof JsonArraySchema) return "array";

        return "object";
    }

    private static ToolExecutor buildToolExecutor(Object object, Method method) {
        return DefaultToolExecutor.builder()
                .object(object)
                .originalMethod(method)
                .methodToInvoke(method)
                .wrapToolArgumentsExceptions(true)
                .propagateToolExecutionExceptions(true)
                .build();
    }

}
