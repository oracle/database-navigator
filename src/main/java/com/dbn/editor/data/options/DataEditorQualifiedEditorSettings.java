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

package com.dbn.editor.data.options;

import com.dbn.common.latent.Latent;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.util.Unsafe;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.editor.data.options.ui.DataEditorQualifiedEditorSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DataEditorQualifiedEditorSettings extends BasicConfiguration<DataEditorSettings, DataEditorQualifiedEditorSettingsForm> {
    private final Latent<List<TextContentType>> contentTypes = Latent.basic(() -> createContentTypes());
    private int textLengthThreshold = 300;

    DataEditorQualifiedEditorSettings(DataEditorSettings parent) {
        super(parent);
    }

    private @NotNull List<TextContentType> createContentTypes() {
        return Stream.of(
                createContentType("Text", "PLAIN_TEXT"),
                createContentType("Properties", "Properties"),
                createContentType("XML", "XML"),
                createContentType("DTD", "DTD"),
                createContentType("HTML", "HTML"),
                createContentType("XHTML", "XHTML"),
                createContentType("CSS", "CSS"),
                createContentType("Java", "JAVA"),
                createContentType("SQL", "DBN-SQL"),
                createContentType("PL/SQL", "DBN-PSQL"),
                createContentType("JPA QL", "JPA QL"),
                createContentType("JavaScript", "JavaScript"),
                createContentType("JSON", "JSON"),
                createContentType("JSON5", "JSON5"),
                createContentType("PHP", "PHP"),
                createContentType("JSP", "JSP"),
                createContentType("JSPx", "JSPX"),
                createContentType("Perl", "Perl"),
                createContentType("Groovy", "Groovy"),
                createContentType("FTL", "FTL"),
                createContentType("TML", "TML"),
                createContentType("GSP", "GSP"),
                createContentType("ASP", "ASP"),
                createContentType("VTL", "VTL"),
                createContentType("AIDL", "AIDL"),
                createContentType("YAML", "YAML"),
                createContentType("Flex", "SWF"),
                createContentType("C#", "C#"),
                createContentType("C++", "C++"),
                createContentType("Bash", "Bash"),
                createContentType("Manifest", "Manifest")
        ).filter(e -> e != null).collect(Collectors.toList());
    }

    @Nullable
    private TextContentType createContentType(@NonNls String name, @NonNls String fileTypeName) {
        return Unsafe.warned(null, () -> TextContentType.create(name, fileTypeName));
    }

    @Nls
    @Override
    public String getDisplayName() {
        return txt("cfg.dataEditor.title.ContentTypes");
    }

    public List<TextContentType> getContentTypes() {
        return contentTypes.get();
    }

    @Nullable
    public TextContentType getContentType(String name) {
        if (isEmpty(name)) return null;

        List<TextContentType> contentTypes = getContentTypes();
        for (TextContentType contentType : contentTypes) {
            if (Objects.equals(contentType.getName(), name)) {
                return contentType;
            }
        }
        return null;
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public DataEditorQualifiedEditorSettingsForm createConfigurationEditor() {
        return new DataEditorQualifiedEditorSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "qualified-text-editor";
    }

    @Override
    public void readConfiguration(Element element) {
        textLengthThreshold = integerAttribute(element, "text-length-threshold", textLengthThreshold);
        Element contentTypes = element.getChild("content-types");
        for (Element child : contentTypes.getChildren()) {
            String name = stringAttribute(child, "name");
            TextContentType contentType = getContentType(name);
            if (contentType != null) {
                boolean enabled = booleanAttribute(child, "enabled", true);
                contentType.setSelected(enabled);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setIntegerAttribute(element, "text-length-threshold", textLengthThreshold);
        Element contentTypesElement = newElement(element, "content-types");
        for (TextContentType contentType : getContentTypes()) {
            Element contentTypeElement = newElement(contentTypesElement, "content-type");
            contentTypeElement.setAttribute("name", contentType.getName());
            contentTypeElement.setAttribute("enabled", Boolean.toString(contentType.isSelected()));
        }
    }
}
