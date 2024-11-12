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

package com.dbn.assistant.chat.message;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.first;
import static com.intellij.lang.Language.findLanguageByID;

/**
 * Coding language utility class, providing mapping between AI language identifiers and the ones provided by the IDE
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class ChatMessageLanguages {
    public static final Map<String, String> LANGUAGE_MAPPINGS = new HashMap<>();
    static {
        // mappings between language ids from llm outputs to IntelliJ language identifiers
        LANGUAGE_MAPPINGS.put("sql", "DBN-SQL");
        LANGUAGE_MAPPINGS.put("oracle", "DBN-SQL");
        LANGUAGE_MAPPINGS.put("jql", "JQL");
        LANGUAGE_MAPPINGS.put("js", "JavaScript");
        LANGUAGE_MAPPINGS.put("javascript", "JavaScript");
        LANGUAGE_MAPPINGS.put("java", "JAVA");
        LANGUAGE_MAPPINGS.put("xml", "XML");
        LANGUAGE_MAPPINGS.put("html", "HTML");
        LANGUAGE_MAPPINGS.put("xhtml", "XHTML");
        LANGUAGE_MAPPINGS.put("yml", "yaml");
        LANGUAGE_MAPPINGS.put("yaml", "yaml");
        LANGUAGE_MAPPINGS.put("json", "JSON");
        LANGUAGE_MAPPINGS.put("json5", "JSON5");
        LANGUAGE_MAPPINGS.put("dtd", "DTD");
        LANGUAGE_MAPPINGS.put("svg", "SVG");
        LANGUAGE_MAPPINGS.put("groovy", "Groovy");
        LANGUAGE_MAPPINGS.put("kotlin", "kotlin");
        LANGUAGE_MAPPINGS.put("py", "Python");
        LANGUAGE_MAPPINGS.put("python", "Python");
        LANGUAGE_MAPPINGS.put("md", "Markdown");
        LANGUAGE_MAPPINGS.put("markdown", "Markdown");
        LANGUAGE_MAPPINGS.put("properties", "Properties");
        // TODO ...
    }

    @Nullable
    public static Language resolveLanguage(@Nullable String identifier) {
        if (identifier == null) return null;
        identifier = identifier.trim().toLowerCase();

        String languageId = rezolveLanguageId(identifier);
        Language language = findLanguageByID(languageId);

        return nvl(language, PlainTextLanguage.INSTANCE);
    }

    private static String rezolveLanguageId(String identifier) {
        return coalesce(
                () -> LANGUAGE_MAPPINGS.get(identifier),                              // strong match
                () -> first(LANGUAGE_MAPPINGS.keySet(), k -> identifier.contains(k)), // soft match (e.g. "oracle sql" -> "sql")
                () -> "TEXT");                                                        // last resort
    }
}
