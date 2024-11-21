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

package com.dbn.diagnostics.data;


import com.dbn.common.locale.Formatter;
import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.state.PersistentStateElement;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;


@Getter
public class ParserDiagnosticsResult implements PersistentStateElement, Comparable<ParserDiagnosticsResult> {

    private final Map<String, IssueCounter> entries = new TreeMap<>();
    private final ProjectRef project;

    private String id = UUID.randomUUID().toString();
    private Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private int index;
    private boolean draft = true;
    private final IssueCounter issues = new IssueCounter();

    public ParserDiagnosticsResult(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    public ParserDiagnosticsResult(@NotNull Project project, Element element) {
        this(project);
        readState(element);
    }

    public ParserDiagnosticsDeltaResult delta(@Nullable ParserDiagnosticsResult previous) {
        return new ParserDiagnosticsDeltaResult(previous, this);
    }

    public void addEntry(String file, int errors, int warnings) {
        this.entries.put(file, new IssueCounter(errors, warnings));
        this.issues.merge(errors, warnings);
    }

    public Set<String> getFiles() {
        return entries.keySet();
    }

    @Nullable
    public IssueCounter getIssues(String file) {
        return entries.get(file);
    }

    public int getIssueCount(String file) {
        IssueCounter counter = entries.get(file);
        return counter == null ? 0 : counter.issueCount();
    }

    public boolean isPresent(String file) {
        return entries.containsKey(file);
    }

    public void markSaved() {
        this.draft = false;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public String getName() {
        Formatter formatter = RegionalSettings.getInstance(getProject()).getBaseFormatter();
        return "Result " + index + " - " + formatter.formatDateTime(timestamp) + (draft ? " (draft)" : "");
    }

    @Override
    public void readState(Element element) {
        if (element != null) {
            id = element.getAttributeValue("id");
            draft = false;
            timestamp = Timestamp.valueOf(element.getAttributeValue("timestamp"));
            List<Element> children = element.getChildren();
            for (Element child : children) {
                String filePath = child.getAttributeValue("path");
                int errorCount = integerAttribute(child, "error-count", 0);
                int unresolvedCount = integerAttribute(child, "warning-count", -1);
                addEntry(filePath, errorCount, unresolvedCount);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("id", id);
        element.setAttribute("timestamp", timestamp.toString());
        for (val entry : entries.entrySet()) {
            String filePath = entry.getKey();
            IssueCounter issues = entry.getValue();

            Element child = newElement(element, "file");
            child.setAttribute("path", filePath);
            setIntegerAttribute(child, "error-count", issues.getErrors());
            setIntegerAttribute(child, "warning-count", issues.getWarnings());
        }
    }

    @Override
    public int compareTo(@NotNull ParserDiagnosticsResult o) {
        return o.getTimestamp().compareTo(this.getTimestamp());
    }
}
