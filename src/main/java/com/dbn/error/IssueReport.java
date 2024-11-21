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

package com.dbn.error;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import lombok.Data;

import java.nio.charset.Charset;
import java.util.Locale;

import static com.dbn.common.util.Commons.nvl;

@Data
public class IssueReport {
    private final Project project;
    private final IdeaPluginDescriptor plugin;
    private final IdeaLoggingEvent[] events;
    private final String message;
    private final Consumer<SubmittedReportInfo> consumer;

    private String osVersion;
    private String ideVersion;
    private String javaVersion;
    private String pluginVersion;


    private String databaseType;
    private String databaseName;
    private String databaseVersion;
    private String databaseDriver;

    private String summary;
    private String description;
    private String clientId;
    
    public IssueReport(
            Project project,
            IdeaPluginDescriptor plugin,
            IdeaLoggingEvent[] events,
            String message,
            Consumer<SubmittedReportInfo> consumer) {
        this.project = project;
        this.plugin = plugin;
        this.events = events;
        this.message = message;
        this.consumer = consumer;
    }

    public IdeaLoggingEvent getEvent() {
        return events[0];
    }

    public String getDatabaseType() {
        return nvl(databaseType, "NA");
    }

    public String getDatabaseName() {
        return nvl(databaseName, "NA");
    }

    public String getDatabaseVersion() {
        return nvl(databaseVersion, "NA");
    }

    public String getDatabaseDriver() {
        return nvl(databaseDriver, "NA");
    }

    public String getLastActionId() {
        return nvl(IdeaLogger.ourLastActionId, "NA");
    }

    public String getSystemLocale() {
        Locale locale = Locale.getDefault();
        return locale.toLanguageTag() + " (" +
                locale.getDisplayLanguage(Locale.ENGLISH) + " - " +
                locale.getDisplayCountry(Locale.ENGLISH) + ")";
    }

    public String getSystemCharset() {
        return Charset.defaultCharset().name();
    }
}
