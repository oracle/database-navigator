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

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.editor.data.options.ui.DataEditorQualifiedEditorSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
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

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DataEditorQualifiedEditorSettings extends BasicConfiguration<DataEditorSettings, DataEditorQualifiedEditorSettingsForm> {
    private final List<TextContentType> contentTypes = Stream.of(
        TextContentType.create("Text", "PLAIN_TEXT"),
        TextContentType.create("Properties", "Properties"),
        TextContentType.create("XML", "XML"),
        TextContentType.create("DTD", "DTD"),
        TextContentType.create("HTML", "HTML"),
        TextContentType.create("XHTML", "XHTML"),
        TextContentType.create("CSS", "CSS"),
        TextContentType.create("Java", "JAVA"),
        TextContentType.create("SQL", "DBN-SQL"),
        TextContentType.create("PL/SQL", "DBN-PSQL"),
        TextContentType.create("JPA QL", "JPA QL"),
        TextContentType.create("JavaScript", "JavaScript"),
        TextContentType.create("JSON", "JSON"),
        TextContentType.create("JSON5", "JSON5"),
        TextContentType.create("PHP", "PHP"),
        TextContentType.create("JSP", "JSP"),
        TextContentType.create("JSPx", "JSPX"),
        TextContentType.create("Perl", "Perl"),
        TextContentType.create("Groovy", "Groovy"),
        TextContentType.create("FTL", "FTL"),
        TextContentType.create("TML", "TML"),
        TextContentType.create("GSP", "GSP"),
        TextContentType.create("ASP", "ASP"),
        TextContentType.create("VTL", "VTL"),
        TextContentType.create("AIDL", "AIDL"),
        TextContentType.create("YAML", "YAML"),
        TextContentType.create("Flex", "SWF"),
        TextContentType.create("C#", "C#"),
        TextContentType.create("C++", "C++"),
        TextContentType.create("Bash", "Bash"),
        TextContentType.create("Manifest", "Manifest")
    ).filter(e -> e != null).collect(Collectors.toList());

    private int textLengthThreshold = 300;

    DataEditorQualifiedEditorSettings(DataEditorSettings parent) {
        super(parent);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return txt("cfg.dataEditor.title.ContentTypes");
    }

    @Override
    public String getHelpTopic() {
        return "dataEditor";
    }

    @Nullable
    public TextContentType getContentType(String name) {
        if (Strings.isNotEmpty(name)) {
            for (TextContentType contentType : contentTypes) {
                if (Objects.equals(contentType.getName(), name)) {
                    return contentType;
                }
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
