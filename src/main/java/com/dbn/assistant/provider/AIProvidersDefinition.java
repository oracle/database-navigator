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

package com.dbn.assistant.provider;

import com.dbn.assistant.AssistantType;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Safe;
import com.dbn.common.util.XmlContents;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.convert;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * AI-Provider and Large Language Model metadata, featuring currently supported AI providers as well as their language models.
 * The definition is held in the underlying language-model-providers.xml file having the following structure:
 * <pre>
 * {@code
 * <language-model-provider>
 *     <provider id="PROVIDER_ID" name="Provider Name" host="api.host.name">
 *         <urls>
 *             <url type="OFFICIAL">https://www.ai-provider.com</url>
 *             <url type="API">https://ai-provider.com/api-keys</url>
 *             <url type="GUIDE">https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/manage-ai-profiles</url>
 *         </urls>
 *         <models>
 *             <model id="MODEL_ABC"                api-name="model-abc"/>
 *             <model id="MODEL_ABCD"               api-name="model-abcd"/>
 *             ...
 *         </models>
 *     </provider>
 *     ...
 * </language-model-provider>
 * }
 * </pre>
 * The metadata also features urls of different flavors, meant to be presented in the setup and help screens
 *  <li> API: url to the API documentation of the LLM provider
 *  <li> OFFICIAL: url to the official LLM provider site (e.g. https://www.openai.com)
 *  <li> GUIDE: the url to the oracle ai-profile management documentation
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class AIProvidersDefinition {
    private static final Map<String, AIProvider> providers = initProviders();

    List<AIProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    @SneakyThrows
    private static @NotNull Map<String, AIProvider> initProviders() {
        Element element = XmlContents.fileToElement(AIProvidersDefinition.class, "ai-providers.xml");
        Element typesElement = element.getChild("provider-types");
        List<AIProvider> templates = loadProviders(typesElement);
        return Lists.toMap(templates, t -> t.getId());
    }

    @SneakyThrows
    public static List<AIProvider> loadProviders(AssistantType assistantType) {
        Element element = XmlContents.fileToElement(AIProvidersDefinition.class, "ai-providers.xml");
        Element supportElement = element.getChild("provider-support");

        List<Element> assistantElements = supportElement.getChildren("assistant");
        for (Element providersElement : assistantElements) {
            String type = providersElement.getAttributeValue("type");
            if (type.equals(assistantType.name())) {
                return loadProviders(providersElement);
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<AIProvider> loadProviders(Element element) {
        List<Element> providerElements = element.getChildren();
        List<AIProvider> assistantProviders = new ArrayList<>();
        for (Element providerElement : providerElements) {
            String providerId = stringAttribute(providerElement, "id");
            AIProvider template = Safe.call(providers, p -> p.get(providerId));
            AIProvider provider = createProvider(providerElement, template);
            assistantProviders.add(provider);
        }
        return assistantProviders;
    }


    private static AIProvider createProvider(Element element, AIProvider providerTemplate) {
        String id = stringAttribute(element, "id");
        String name = fallback(stringAttribute(element, "name"), providerTemplate, t -> t.getName());
        String host = fallback(stringAttribute(element, "host"), providerTemplate, t -> t.getHost());
        String baseUrl = fallback(stringAttribute(element, "base-url"), providerTemplate, t -> t.getBaseUrl());
        String apiName = fallback(stringAttribute(element, "api-name"), providerTemplate, t -> t.getApiName());

        AIProvider provider = new AIProvider(id, name);
        provider.setHost(host);
        provider.setApiName(apiName);
        provider.setBaseUrl(baseUrl);

        createModels(element, provider, providerTemplate);
        createUrls(element, provider, providerTemplate);

        return provider;
    }

    private static void createModels(Element element, AIProvider provider, AIProvider providerTemplate) {
        List<Element> modelElements = element.getChild("models").getChildren();
        List<AIModel> models = convert(modelElements, e -> createModel(e, provider, providerTemplate));
        provider.setModels(unmodifiableList(models));
    }

    private static AIModel createModel(Element element, AIProvider provider, AIProvider providerTemplate) {
        String modelId = stringAttribute(element, "id");
        AIModel modelTemplate = providerTemplate == null ? null : providerTemplate.getModel(modelId);
        boolean templateDefault = modelTemplate != null && modelTemplate.isDefault();
        boolean templateExperimental = modelTemplate != null && modelTemplate.isExperimental();
        boolean templateDeprecated = modelTemplate != null && modelTemplate.isDeprecated();

        String modelApiName = fallback(stringAttribute(element, "api-name"), modelTemplate, t -> t.getApiName());
        AIModel model = new AIModel(provider, modelId, modelApiName);

        model.set(AIModelProperty.DEFAULT, booleanAttribute(element, "default", templateDefault));
        model.set(AIModelProperty.DEPRECATED, booleanAttribute(element, "deprecated", templateDeprecated));
        model.set(AIModelProperty.EXPERIMENTAL, booleanAttribute(element, "experimental", templateExperimental));

        return model;
    }

    private static void createUrls(Element element, AIProvider provider, AIProvider providerTemplate) {
        Map<ProviderUrlType, String> urls = new LinkedHashMap<>();
        if (providerTemplate != null) {
            urls.putAll(providerTemplate.getUrls());
        }

        Element urlsElement = element.getChild("urls");
        List<Element> urlElements = childrenOf(urlsElement);
        for (Element urlElement : urlElements) {
            ProviderUrlType urlType = enumAttribute(urlElement, "type", ProviderUrlType.class);
            urls.put(urlType, urlElement.getText());
        }
        provider.setUrls(unmodifiableMap(urls));
    }

    private <T, F> T fallback(T value, F fallback, Function<F, T> supplier) {
        return value == null ? fallback == null ? null : supplier.apply(fallback) : value;
    }
}
