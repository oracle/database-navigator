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

package com.dbn.database;

import com.dbn.common.util.Chars;
import com.intellij.execution.configurations.GeneralCommandLine;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Strings.isEmpty;

@Getter
@Setter
public abstract class DatabaseClientCommand {
    private GeneralCommandLine commandLine = new GeneralCommandLine();

    private final String scriptContent;
    private final List<String> statements = new ArrayList<>();
    private char[] password;

    public DatabaseClientCommand(@NotNull String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public void addEnvironmentVariable(@NonNls String key, char[] value) {
        commandLine.withEnvironment(key, Chars.toString(value));
    }

    public void addEnvironmentVariable(@NonNls String key, @NonNls String value) {
        commandLine.withEnvironment(key, value);
    }

    public String getTextContent() {
        return scriptContent;
    }

    public void addStatement(@NonNls String statement) {
        statements.add(statement);
    }

    public void initCommand(String executable) {
        commandLine.setExePath(executable);
    }

    public void addParameter(@NonNls String param) {
        if (isEmpty(param)) return;
        commandLine.addParameter(param);
    }

    public void addParameter(@NonNls String param, @NonNls String value) {
        if (isEmpty(value)) return;
        commandLine.addParameter(param);
        commandLine.addParameter(value);
    }
    public void addKvParameter(@NonNls String param, @NonNls String value) {
        if (isEmpty(value)) return;
        commandLine.addParameter(param + "=" + value);
    }

    public void insertKvParameter(@NonNls String param, @NonNls String value) {
        if (isEmpty(value)) return;
        commandLine.getParametersList().addAt(0, param + "=" + value);
    }

    @NotNull
    public String getPresentableCommand() {
        return commandLine.getCommandLineString();
    }
}
