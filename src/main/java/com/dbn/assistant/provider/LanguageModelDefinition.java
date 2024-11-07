/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.provider;

import com.dbn.common.util.XmlContents;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
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
public class LanguageModelDefinition {
    private static final LanguageModelDefinition INSTANCE = new LanguageModelDefinition();
    private final List<AIProvider> providers;

    @SneakyThrows
    private LanguageModelDefinition() {
        Element element = XmlContents.fileToElement(getClass(), "language-model-providers.xml");
        List<Element> providerElements = element.getChildren("provider");

        providers = unmodifiableList(convert(providerElements, e -> createProvider(e)));
    }

    private static AIModel createModel(AIProvider provider, Element element) {
        String modelId = stringAttribute(element, "id");
        String modelApiName = stringAttribute(element, "api-name");
        return new AIModel(provider, modelId, modelApiName);
    }

    private static AIProvider createProvider(Element element) {
        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String host = stringAttribute(element, "host");
        boolean main = booleanAttribute(element, "default", false);
        boolean experimental = booleanAttribute(element, "experimental", false);
        AIProvider provider = new AIProvider(id, name, host, main, experimental);

        createModels(element, provider);
        createUrls(element, provider);

        return provider;
    }

    private static void createModels(Element element, AIProvider provider) {
        List<Element> modelElements = element.getChild("models").getChildren();
        List<AIModel> models = convert(modelElements, e -> createModel(provider, e));
        provider.setModels(unmodifiableList(models));
    }

    private static void createUrls(Element element, AIProvider provider) {
        List<Element> urlElements = element.getChild("urls").getChildren();
        Map<ProviderUrlType, String> urls = new HashMap<>();
        for (Element urlElement : urlElements) {
            ProviderUrlType urlType = enumAttribute(urlElement, "type", ProviderUrlType.class);
            urls.put(urlType, urlElement.getText());
        }
        provider.setUrls(unmodifiableMap(urls));
    }

    public static List<AIProvider> providers() {
        return INSTANCE.providers;
    }
}
