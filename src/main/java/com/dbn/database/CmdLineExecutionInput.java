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
import com.dbn.common.util.Strings;
import com.intellij.execution.configurations.GeneralCommandLine;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CmdLineExecutionInput {
    private GeneralCommandLine command = new GeneralCommandLine();

    private final String scriptContent;
    private final List<String> statements = new ArrayList<>();
    private char[] password;

    public CmdLineExecutionInput(@NotNull String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public void addEnvironmentVariable(String key, char[] value) {
        command.withEnvironment(key, Chars.toString(value));
    }

    public void addEnvironmentVariable(String key, String value) {
        command.withEnvironment(key, value);
    }

    public String getTextContent() {
        return scriptContent;
    }

    public void addStatement(String statement) {
        statements.add(statement);
    }

    public void initCommand(String executable) {
        command.setExePath(executable);
    }

    public void addParameter(String param) {
        if (Strings.isEmpty(param)) return;
        command.addParameter(param);
    }

    public void addParameter(String param, String value) {
        if (Strings.isEmpty(value)) return;
        command.addParameter(param);
        command.addParameter(value);
    }
    public void addKvParameter(String param, String value) {
        if (Strings.isEmpty(value)) return;
        command.addParameter(param + "=" + value);
    }

    @NotNull
    public String getCommandLine() {
        return command.getCommandLineString();
    }
}
