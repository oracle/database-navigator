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
import com.dbn.assistant.provider.AIAuthentication.Field;
import com.dbn.common.util.Csvs;
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

import static com.dbn.assistant.provider.AIAuthentication.Field.API_KEY;
import static com.dbn.assistant.provider.AIModelProperty.DEFAULT;
import static com.dbn.assistant.provider.AIModelProperty.DEPRECATED;
import static com.dbn.assistant.provider.AIModelProperty.DISCONTINUED;
import static com.dbn.assistant.provider.AIModelProperty.EXPERIMENTAL;
import static com.dbn.assistant.provider.AIModelProperty.RECOMMENDED;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Commons.coalesce;
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
    private static final Map<AIProviderId, AIProvider> providers = initProviders();

    List<AIProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    @SneakyThrows
    private static @NotNull Map<AIProviderId, AIProvider> initProviders() {
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
            AIProviderId providerId = enumAttribute(providerElement, "id", AIProviderId.class);
            AIProvider template = Safe.call(providers, p -> p.get(providerId));
            AIProvider provider = createProvider(providerElement, template);
            assistantProviders.add(provider);
        }
        return assistantProviders;
    }


    private static AIProvider createProvider(Element element, AIProvider providerTemplate) {
        AIProviderId id = enumAttribute(element, "id", AIProviderId.class);
        String name = fallback(stringAttribute(element, "name"), providerTemplate, t -> t.getName());
        String host = fallback(stringAttribute(element, "host"), providerTemplate, t -> t.getHost());
        String apiName = fallback(stringAttribute(element, "api-name"), providerTemplate, t -> t.getApiName());

        AIProvider provider = new AIProvider(id, name);
        provider.setHost(host);
        provider.setApiName(apiName);

        initModels(element, provider, providerTemplate);
        initUrls(element, provider, providerTemplate);
        initAuth(element, provider, providerTemplate);

        return provider;
    }

    private static void initModels(Element element, AIProvider provider, AIProvider providerTemplate) {
        List<Element> modelElements = childrenOf(element.getChild("models"));
        List<AIModel> models = convert(modelElements, e -> createModel(e, provider, providerTemplate));
        provider.setModels(unmodifiableList(models));
    }

    private static AIModel createModel(Element element, AIProvider provider, AIProvider providerTemplate) {
        String modelId = stringAttribute(element, "id");
        AIModel modelTemplate = providerTemplate == null ? null : providerTemplate.getModel(modelId);
        boolean templateDefault = modelTemplate != null && modelTemplate.isDefault();
        boolean templateRecommended = modelTemplate != null && modelTemplate.isRecommended();
        boolean templateExperimental = modelTemplate != null && modelTemplate.isExperimental();
        boolean templateDeprecated = modelTemplate != null && modelTemplate.isDeprecated();
        boolean templateDiscontinued = modelTemplate != null && modelTemplate.isDiscontinued();

        AIProviderId templateBaseProviderId = modelTemplate != null ? modelTemplate.getBaseProviderId() : null;

        AIProviderId baseProviderId = coalesce(
                () -> enumAttribute(element, "base-provider-id", AIProviderId.class),
                () -> templateBaseProviderId,
                () -> provider.getId());


        String modelApiName = fallback(stringAttribute(element, "api-name"), modelTemplate, t -> t.getApiName());
        String modelShortName = fallback(stringAttribute(element, "short-name"), modelTemplate, t -> t.getShortName());
        AIModel model = new AIModel(modelId, modelApiName, modelShortName, provider, baseProviderId);

        // status
        AIModelStatus modelStatus = enumAttribute(element, "status", AIModelStatus.class);
        model.set(DEFAULT, booleanAttribute(element, "default", templateDefault));
        model.set(RECOMMENDED, booleanAttribute(element, "recommended", templateRecommended));
        model.set(EXPERIMENTAL, booleanAttribute(element, "experimental", templateExperimental));
        if (modelStatus == null) {
            model.set(DEPRECATED, booleanAttribute(element, "deprecated", templateDeprecated));
            model.set(DISCONTINUED, booleanAttribute(element, "discontinued", templateDiscontinued));
        } else {
            model.set(DEPRECATED, modelStatus == AIModelStatus.DEPRECATED);
            model.set(DISCONTINUED, modelStatus == AIModelStatus.DISCONTINUED);
        }

        // features
        String featuresAttribute = fallback(stringAttribute(element, "features"), modelTemplate, t -> t.getFeaturesCsv());
        List<AIModelFeature> features = Csvs.csvToValues(featuresAttribute, s -> AIModelFeature.get(s));
        AIModelFeatures modelFeatures = model.getFeatures();
        if (featuresAttribute == null) {
            modelFeatures.set(AIModelFeature.VALUES, true);
        } else {
            modelFeatures.set(features, true);
        }

        return model;
    }

    private static void initUrls(Element element, AIProvider provider, AIProvider providerTemplate) {
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

    private static void initAuth(Element element, AIProvider provider, AIProvider providerTemplate) {
        AIAuthentication authentication;

        Element authElement = element.getChild("auth");
        if (authElement == null) {
            authentication = providerTemplate == null ? null : providerTemplate.getAuthentication();
            if (authentication == null) {
                authentication = new AIAuthentication();
                authentication.addField(API_KEY, true);
            }
        } else {
            authentication = new AIAuthentication();
            List<Element> fieldElements = childrenOf(authElement, "field");
            for (Element fieldElement : fieldElements) {
                Field field = enumAttribute(fieldElement, "id", Field.class);
                boolean required = booleanAttribute(fieldElement, "required", false);
                authentication.addField(field, required);
            }
        }

        provider.setAuthentication(authentication);
    }

    private <T, F> T fallback(T value, F fallback, Function<F, T> supplier) {
        return value == null ? fallback == null ? null : supplier.apply(fallback) : value;
    }
}
